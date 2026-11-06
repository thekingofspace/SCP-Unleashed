package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.labware.LabExposure;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class Scp610SporeBearer extends Scp610Utility {
    public static final int SpewTicks = 32;
    public static final int ReleaseTick = 16;
    public static final int PuffGap = 240;
    public static final double CloudRadius = 3.5;
    public static final double Range = 48.0;
    public static final double HomeReach = 8.0;
    public static final int Puffs = 3;
    public static final int TripTicks = 1200;
    public static final int RestTicks = 600;
    @Nullable
    private Vec3 Destination;
    private int PuffReady = PuffGap;
    private int PuffsLeft;
    private int TripLeft;
    private int RestLeft = 200;

    public Scp610SporeBearer(EntityType<? extends Scp610SporeBearer> Type, Level World) {
        super(Type, World, Scp610Form.SporeBearer);
    }

    @Override
    protected void registerWork() {
        goalSelector.addGoal(3, new Roam(this));
    }

    @Nullable
    public Vec3 destination() {
        return Destination;
    }

    public boolean away() {
        Scp610Hive Hive = hive();
        return Hive != null && position().distanceToSqr(Vec3.atBottomCenterOf(Hive.anchor())) > HomeReach * HomeReach;
    }

    @Nullable
    public Vec3 chooseDestination(ServerLevel World) {
        Scp610Hive Hive = hive();
        BlockPos Anchor = Hive != null ? Hive.anchor() : blockPosition();
        Optional<BlockPos> Village = World.getPoiManager().findClosest(Holder -> Holder.is(PoiTypes.MEETING), Anchor, (int) Range, PoiManager.Occupancy.ANY);
        if (Village.isPresent()) {
            return Vec3.atBottomCenterOf(Village.get());
        }
        AABB Area = new AABB(Anchor).inflate(Range * 0.6, 8.0, Range * 0.6);
        List<LivingEntity> Prey = World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Scp610Infection.exposable(Found) && !(Found instanceof Scp610Creature));
        if (!Prey.isEmpty()) {
            return Prey.get(random.nextInt(Prey.size())).position();
        }
        double Angle = random.nextDouble() * Math.PI * 2.0;
        double Reach = 20.0 + random.nextDouble() * 20.0;
        return Vec3.atBottomCenterOf(Anchor).add(Math.cos(Angle) * Reach, 0.0, Math.sin(Angle) * Reach);
    }

    public void puff(ServerLevel World) {
        Vec3 Vent = position().add(0.0, getBbHeight() * 0.9, 0.0);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp610Sounds.SpewLayers) {
            World.playSound(null, Vent.x, Vent.y, Vent.z, Layer.get(), SoundSource.HOSTILE, 1.0f, 1.2f);
        }
        World.sendParticles(Scp610Registry.MawParticle.get(), Vent.x, Vent.y + 0.2, Vent.z, 1, 0.0, 0.0, 0.0, 0.0);
        Vec3 Centre = position().add(0.0, getBbHeight() * 0.5, 0.0);
        Scp610Infection.cloud(World, Centre, CloudRadius, 40);
        Scp610Infection.exposeAround(World, Centre, LabExposure.NothingIgnored, CloudRadius);
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionSpew && Elapsed == ReleaseTick && level() instanceof ServerLevel World) {
            puff(World);
            return;
        }
        super.actionTick(Kind, Elapsed);
    }

    @Override
    protected void work() {
        if (!(level() instanceof ServerLevel World) || fleeing()) {
            return;
        }
        PuffReady = Math.max(0, PuffReady - 1);
        if (Destination == null) {
            RestLeft = Math.max(0, RestLeft - 1);
            if (RestLeft <= 0) {
                Destination = chooseDestination(World);
                TripLeft = TripTicks;
                PuffsLeft = Puffs;
            }
            return;
        }
        TripLeft--;
        if (PuffReady <= 0 && away() && canAct()) {
            PuffReady = PuffGap;
            PuffsLeft--;
            setAction(ActionSpew, SpewTicks);
        }
        if (PuffsLeft <= 0 || TripLeft <= 0) {
            Destination = null;
            RestLeft = RestTicks;
        }
    }

    public static final class Roam extends Goal {
        private final Scp610SporeBearer Bearer;

        public Roam(Scp610SporeBearer Bearer) {
            this.Bearer = Bearer;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Bearer.destination() != null && !Bearer.fleeing() && !Bearer.locksMovement();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 Target = Bearer.destination();
            if (Target != null && !Bearer.near(Target, 3.0)) {
                Bearer.walkTo(Target, 0.9);
            }
        }
    }
}
