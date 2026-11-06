package net.rai.scp_unleashed.dclass.duty;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.HumanoidEyes;
import net.rai.scp_unleashed.dclass.duty.network.DClassBlinkPayload;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import net.rai.scp_unleashed.scp173.Scp173Witness;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public final class DClassGaze {
    public static final int BlinkTicks = HumanoidEyes.BlinkTicks;
    public static final int BlinkMin = 50;
    public static final int BlinkSpread = 90;
    public static final int AgainGap = 3;
    public static final float StressRate = 0.72f;
    public static final float PanicRate = 0.58f;
    public static final int StressMemory = 100;
    public static final double SightRange = Scp173Witness.Range;
    public static final double WatchRange = 16.0;
    public static final double PartnerRange = 16.0;
    public static final int WatchCheck = 5;
    public static final float LookSpeed = 30.0f;

    private DClassGaze() {
    }

    public static final class State {
        private int Clock;
        private int Blinking;
        private boolean Again;
        private boolean Watching;
        private boolean Seeded;
        private int Held;

        public int clock() {
            return Clock;
        }

        public int blinking() {
            return Blinking;
        }

        public boolean watching() {
            return Watching;
        }
    }

    public static State state(DClassLike Owner) {
        return Owner.getData(DClassDutyRegistry.Gaze.get());
    }

    public static boolean blinking(Entity Body) {
        State Found = Body.getExistingDataOrNull(DClassDutyRegistry.Gaze.get());
        return Found != null && Found.Blinking > 0;
    }

    public static void holdEyes(DClassLike Owner, int Ticks) {
        State Mine = state(Owner);
        Mine.Held = Math.max(0, Ticks);
        Mine.Blinking = 0;
        Mine.Again = false;
        Mine.Seeded = true;
    }

    public static void blinkNow(DClassLike Owner) {
        State Mine = state(Owner);
        Mine.Held = 0;
        Mine.Seeded = true;
        start(Owner, Mine);
    }

    public static void tick(DClassLike Owner) {
        if (!(Owner.level() instanceof ServerLevel World) || !Owner.isAlive() || Owner.isSpecimen() || Owner.isPuppet() || !ControlBodies.isPersonLike(Owner)) {
            return;
        }
        State Mine = state(Owner);
        if (!Mine.Seeded) {
            Mine.Seeded = true;
            Mine.Clock = 10 + Owner.getRandom().nextInt(BlinkMin);
            Mine.Watching = statueInView(Owner, WatchRange) != null;
        } else if ((Owner.tickCount + Owner.getId()) % WatchCheck == 0) {
            Mine.Watching = statueInView(Owner, WatchRange) != null;
        }
        if (Owner.eyesShut()) {
            Mine.Blinking = 0;
            return;
        }
        if (Mine.Blinking > 0) {
            Mine.Blinking--;
            if (Mine.Blinking == 0 && Mine.Again) {
                Mine.Again = false;
                Mine.Clock = AgainGap;
            }
            return;
        }
        if (Mine.Held > 0) {
            Mine.Held--;
            return;
        }
        if (--Mine.Clock > 0) {
            return;
        }
        if (yields(World, Owner, Mine)) {
            Mine.Clock = BlinkTicks + 2 + Owner.getRandom().nextInt(4);
            return;
        }
        start(Owner, Mine);
    }

    private static void start(DClassLike Owner, State Mine) {
        Mine.Blinking = BlinkTicks;
        Mine.Again = Owner.getRandom().nextInt(6) == 0;
        Mine.Clock = interval(Owner, Mine);
        if (Owner.level() instanceof ServerLevel) {
            PacketDistributor.sendToPlayersTrackingEntity(Owner, new DClassBlinkPayload(Owner.getId()));
        }
    }

    public static int interval(DClassLike Owner, State Mine) {
        float Rate = Math.max(0.6f, Owner.glanceRate() * 0.8f);
        int Stress = stress(Owner, Mine);
        float Scale = Stress >= 2 ? PanicRate : Stress == 1 ? StressRate : 1.0f;
        return Math.max(BlinkTicks + 2, Math.round((BlinkMin + Owner.getRandom().nextInt(BlinkSpread)) / Rate * Scale));
    }

    public static int stress(DClassLike Owner, State Mine) {
        int Count = 0;
        if (Mine.Watching) {
            Count++;
        }
        if (Owner.getLastHurtByMob() != null && Owner.tickCount - Owner.getLastHurtByMobTimestamp() < StressMemory || Owner.hurtTime > 0) {
            Count++;
        }
        if (Owner.getHealth() < Owner.getMaxHealth() * 0.5f || Owner.isOnFire() || Owner.isFreezing()) {
            Count++;
        }
        return Count;
    }

    private static boolean yields(ServerLevel World, DClassLike Owner, State Mine) {
        if (!Mine.Watching || DClassDuties.role(Owner) != DClassRole.Janitor) {
            return false;
        }
        List<DClass> Partners = World.getEntitiesOfClass(DClass.class, Owner.getBoundingBox().inflate(PartnerRange),
                Found -> Found != Owner && Found.isAlive() && !Found.eyesShut() && DClassDuties.role(Found) == DClassRole.Janitor);
        for (DClass Partner : Partners) {
            State Theirs = Partner.getExistingDataOrNull(DClassDutyRegistry.Gaze.get());
            if (Theirs == null || !Theirs.Watching) {
                continue;
            }
            if (Theirs.Blinking > 0) {
                return true;
            }
            if (Theirs.Held == 0 && Theirs.Clock <= BlinkTicks + 1 && Partner.getId() < Owner.getId()) {
                return true;
            }
        }
        return false;
    }

    public static boolean watcher(LivingEntity Viewer) {
        if (Viewer instanceof Player || !Viewer.isAlive() || Viewer.isRemoved() || !Targeting.canWitness(Viewer) || !ControlBodies.isPersonLike(Viewer) || ControlSessions.isBoundToSession(Viewer)) {
            return false;
        }
        if (Viewer instanceof DClassLike Person) {
            return !Person.eyesShut() && !Person.isPuppet() && !Person.isSpecimen();
        }
        return !Viewer.isSleeping();
    }

    public static boolean watches(LivingEntity Viewer, AABB Box) {
        return !blinking(Viewer) && couldSee(Viewer, Box);
    }

    public static boolean couldSee(LivingEntity Viewer, AABB Box) {
        return watcher(Viewer) && Scp173Witness.canSee(Viewer, Box);
    }

    public static boolean inSight(LivingEntity Viewer, Scp173Statue Statue) {
        return watcher(Viewer) && Scp173Witness.canSee(Viewer, Statue);
    }

    @Nullable
    public static Scp173Statue statueInView(LivingEntity Viewer, double Range) {
        List<Scp173Statue> Found = Viewer.level().getEntitiesOfClass(Scp173Statue.class, Viewer.getBoundingBox().inflate(Range), Statue -> Statue.isAlive() && !Statue.isRemoved());
        if (Found.isEmpty()) {
            return null;
        }
        Found.sort(Comparator.comparingDouble(Viewer::distanceToSqr));
        for (Scp173Statue Statue : Found) {
            if (Statue.distanceToSqr(Viewer) <= Range * Range && inSight(Viewer, Statue)) {
                return Statue;
            }
        }
        return null;
    }

    public static void lookAt(DClass Viewer, Scp173Statue Statue) {
        AABB Box = Statue.sightBox();
        double Y = Mth.lerp(0.75, Box.minY, Box.maxY);
        Viewer.getLookControl().setLookAt(Statue.getX(), Y, Statue.getZ(), LookSpeed, LookSpeed);
    }
}
