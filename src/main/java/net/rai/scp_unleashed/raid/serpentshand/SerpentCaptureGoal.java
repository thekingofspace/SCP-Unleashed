package net.rai.scp_unleashed.raid.serpentshand;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.hostile.HostileHauls;
import net.rai.scp_unleashed.raid.hostile.HostileRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileTargets;
import net.rai.scp_unleashed.raid.hostile.HostileTrophy;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SerpentCaptureGoal extends Goal {
    public static final int SearchEvery = 40;
    public static final double SearchMargin = 24.0;
    public static final double OwnReach = 32.0;
    public static final double CaptureReach = 3.0;
    public static final double SlipReach = 4.5;
    public static final int ChannelTicks = 30;
    public static final int GiveUp = 600;
    public static final double Speed = 1.1;
    private final SerpentsHandAgent Owner;
    private final Set<UUID> Refused = new HashSet<>();
    @Nullable
    private Mob Prey;
    private int Cooldown;
    private int Time;
    private int Channel;
    private int Repath;

    public SerpentCaptureGoal(SerpentsHandAgent Owner) {
        this.Owner = Owner;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static long claimKey(Entity Body) {
        UUID Id = Body.getUUID();
        return Id.getMostSignificantBits() ^ Id.getLeastSignificantBits() ^ 0x5E5E5E5EL;
    }

    private boolean ready() {
        Raid Current = Owner.raid();
        return Current != null && Current.stage() == RaidStage.Active && Owner.seeker() && Owner.active() && !Owner.hauling()
                && !(Owner.recentlyHurt() && Owner.getTarget() != null);
    }

    @Nullable
    public static Mob find(ServerLevel World, Raid Current, Mob Seeker, @Nullable Set<UUID> Refused) {
        BlockPos Centre = Current.estimate();
        double Reach = Current.footprint() + SearchMargin;
        AABB Area = new AABB(Centre).inflate(Reach, 24.0, Reach).minmax(Seeker.getBoundingBox().inflate(OwnReach, 12.0, OwnReach));
        List<Mob> Found = World.getEntitiesOfClass(Mob.class, Area, Candidate -> HostileTargets.capturable(Candidate)
                && (Refused == null || !Refused.contains(Candidate.getUUID())) && !Current.claimed(claimKey(Candidate)));
        Mob Best = null;
        double BestGap = Double.MAX_VALUE;
        for (Mob Candidate : Found) {
            double Gap = Candidate.distanceToSqr(Seeker);
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = Candidate;
            }
        }
        return Best;
    }

    @Override
    public boolean canUse() {
        if (!ready() || --Cooldown > 0 || !(Owner.level() instanceof ServerLevel World)) {
            return false;
        }
        Cooldown = SearchEvery / 2 + Owner.getRandom().nextInt(SearchEvery / 2);
        Raid Current = Owner.raid();
        if (Current == null) {
            return false;
        }
        Mob Found = find(World, Current, Owner, Refused);
        if (Found == null || !Current.claim(claimKey(Found))) {
            return false;
        }
        Prey = Found;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return Prey != null && Prey.isAlive() && !Prey.isRemoved() && ready() && Time < GiveUp && HostileTargets.capturable(Prey);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        Time = 0;
        Channel = 0;
        Repath = 0;
        Owner.setTarget(null);
    }

    @Override
    public void stop() {
        Raid Current = Owner.raid();
        if (Prey != null) {
            if (Current != null) {
                Current.unclaim(claimKey(Prey));
            }
            if (Time >= GiveUp) {
                Refused.add(Prey.getUUID());
            }
        }
        Prey = null;
        Channel = 0;
        Owner.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (Prey == null || !(Owner.level() instanceof ServerLevel World)) {
            return;
        }
        Time++;
        Owner.getLookControl().setLookAt(Prey, 30.0f, 30.0f);
        double Gap = Owner.distanceTo(Prey);
        boolean Sees = Owner.getSensing().hasLineOfSight(Prey);
        if (Gap <= CaptureReach && Sees || Channel > 0 && Gap <= SlipReach && Sees) {
            Owner.getNavigation().stop();
            if (Channel == 0) {
                World.playSound(null, Owner.getX(), Owner.getY(), Owner.getZ(), StasisRegistry.CaptureWarp.get(), SoundSource.HOSTILE, 0.8f, 1.2f);
                Owner.swing(InteractionHand.MAIN_HAND);
            }
            Channel++;
            Vec3 Hand = Owner.getEyePosition().add(Owner.getLookAngle().scale(0.4)).subtract(0.0, 0.4, 0.0);
            SerpentPods.beam(World, Hand, Prey.getBoundingBox().getCenter(), Channel);
            if (Channel >= ChannelTicks) {
                capture(World);
            }
            return;
        }
        Channel = 0;
        if (--Repath <= 0) {
            Repath = 10;
            Owner.getNavigation().moveTo(Prey, Speed);
        }
    }

    private void capture(ServerLevel World) {
        Mob Target = Prey;
        Raid Current = Owner.raid();
        RaidContext Context = HostileRaidStyle.context(Owner);
        if (Target == null || Current == null || Context == null) {
            return;
        }
        HostileTrophy Trophy = SerpentPods.capture(World, Target, Owner);
        if (Trophy == null) {
            Refused.add(Target.getUUID());
            Prey = null;
            return;
        }
        HostileHauls.put(Current, Owner.getUUID(), Trophy);
        Owner.carry(SerpentPodItem.filled(Trophy, World.registryAccess()));
        Context.notice("captured", Trophy.label(World.registryAccess()));
        Current.unclaim(claimKey(Target));
        Prey = null;
    }
}
