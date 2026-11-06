package net.rai.scp_unleashed.scp3199;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp3199Nesting {
    public static final int SettleRadius = 12;
    public static final int SettleTicks = 2400;
    public static final int LayInterval = 3600;
    public static final int RetryTicks = 200;
    public static final int ClipLead = Scp3199GaitController.Blend + 1;
    public static final double ClipSeconds = 3.6;
    public static final double SquelchSecond = 1.95;
    public static final double ExitSecond = 2.08;
    public static final double LandSecond = 2.36;
    public static final double[] GulpSeconds = {0.92, 1.24, 1.36, 1.55, 1.84};
    public static final float[] GulpPitches = {0.88f, 0.93f, 0.97f, 1.02f, 1.08f};
    public static final int LayTicks = clipTick(ClipSeconds);
    public static final int HeaveTick = clipTick(0.0);
    public static final int SquelchTick = clipTick(SquelchSecond);
    public static final int ExitTick = clipTick(ExitSecond);
    public static final int LandTick = clipTick(LandSecond);
    public static final int AreaRadius = 12;
    public static final int AreaCap = 16;
    public static final double MouthForward = 1.3217;
    public static final double MouthHeight = 0.9021;
    public static final double ExitForward = -0.0944;
    public static final double ExitDrop = -0.0299;
    public static final double Lift = 0.05;
    public static final double AlertRange = 10.0;
    public static final int ClearSteps = 8;
    public static final double ClearRise = 0.1;

    @Nullable
    private BlockPos Anchor;
    private int SettledFor;
    private int LayWait;
    private int Laying = -1;
    private float LockedYaw;
    private boolean EggOut;

    @Nullable
    public BlockPos anchor() {
        return Anchor;
    }

    public int laying() {
        return Laying;
    }

    public int layWait() {
        return LayWait;
    }

    public float lockedYaw() {
        return LockedYaw;
    }

    public boolean eggOut() {
        return EggOut;
    }

    public int settledFor() {
        return SettledFor;
    }

    public boolean isSettled() {
        return SettledFor >= SettleTicks;
    }

    public void unsettle(Scp3199 Owner) {
        Anchor = null;
        SettledFor = 0;
        if (Laying < 0) {
            LayWait = 0;
        }
    }

    public void settleNow(Scp3199 Owner) {
        settleFor(Owner, SettleTicks);
    }

    public void settleFor(Scp3199 Owner, int Ticks) {
        Anchor = Owner.blockPosition();
        SettledFor = Math.max(0, Math.min(SettleTicks, Ticks));
        LayWait = 0;
    }

    public boolean mayStart(ServerLevel World, Scp3199 Owner) {
        if (!isSettled() || LayWait > 0 || !Owner.isAdult() || Owner.getTarget() != null || !standing(World, Owner) || Owner.isInWater()) {
            return false;
        }
        return Scp3199Rules.breedingAllowed(World) && Scp3199Population.area(World, Owner.position()) < AreaCap && spotted(World, Owner) == null;
    }

    public void tick(ServerLevel World, Scp3199 Owner) {
        BlockPos Here = Owner.blockPosition();
        if (Anchor == null || Anchor.distSqr(Here) > (double) SettleRadius * SettleRadius) {
            Anchor = Here;
            SettledFor = 0;
        } else if (SettledFor < SettleTicks) {
            SettledFor++;
        }
        if (LayWait > 0) {
            LayWait--;
        }
        if (Laying >= 0) {
            layTick(World, Owner);
            return;
        }
        if (!isSettled() || LayWait > 0) {
            return;
        }
        if (!mayStart(World, Owner)) {
            LayWait = RetryTicks;
            return;
        }
        begin(Owner);
    }

    private void begin(Scp3199 Owner) {
        LockedYaw = Owner.yBodyRot;
        EggOut = false;
        Laying = 0;
        face(Owner, LockedYaw);
        Owner.setLaying(true);
        Owner.getNavigation().stop();
    }

    public static int clipTick(double Seconds) {
        return ClipLead + (int) Math.round(Seconds * 20.0);
    }

    private static void face(Scp3199 Owner, float Yaw) {
        Owner.setYRot(Yaw);
        Owner.setYBodyRot(Yaw);
        Owner.setYHeadRot(Yaw);
        Owner.yRotO = Yaw;
        Owner.yBodyRotO = Yaw;
        Owner.yHeadRotO = Yaw;
    }

    private void layTick(ServerLevel World, Scp3199 Owner) {
        Owner.getNavigation().stop();
        face(Owner, LockedYaw);
        Laying++;
        if (Laying == HeaveTick) {
            Scp3199Sounds.play(World, Owner.getEyePosition(), List.of(Scp3199Registry.Lay), 0.9f, Owner.voicePitch());
        }
        for (int Index = 0; Index < GulpSeconds.length; Index++) {
            if (Laying == clipTick(GulpSeconds[Index])) {
                Scp3199Sounds.play(World, Owner.getEyePosition().add(0.0, -0.35, 0.0), List.of(Scp3199Registry.Gulp), 0.8f, GulpPitches[Index]);
            }
        }
        if (Laying == SquelchTick) {
            Scp3199Sounds.play(World, Owner.getEyePosition(), List.of(Scp3199Registry.LaySquelch), 0.85f, Owner.voicePitch());
        }
        if (Laying >= ExitTick && !EggOut) {
            release(World, Owner, true);
        }
        if (Laying == LandTick) {
            Vec3 Mouth = mouth(Owner, LockedYaw);
            Vec3 Landing = new Vec3(Mouth.x, Owner.getY(), Mouth.z).add(direction(LockedYaw).scale(ExitForward * (LandTick - ExitTick) * Owner.growthScale()));
            Scp3199Sounds.play(World, Landing, List.of(Scp3199Registry.Cackle), 0.9f, Owner.voicePitch());
        }
        if (Laying >= LayTicks) {
            Laying = -1;
            LayWait = LayInterval;
            Owner.setLaying(false);
        }
    }

    public void release(ServerLevel World, Scp3199 Owner, boolean Thrown) {
        if (EggOut) {
            return;
        }
        EggOut = true;
        float Scale = Owner.growthScale();
        Vec3 Mouth = mouth(Owner, LockedYaw);
        Vec3 Spot = clearSpot(World, Owner, Mouth);
        Vec3 Motion = Thrown && Spot == Mouth ? direction(LockedYaw).scale(ExitForward * Scale).add(0.0, ExitDrop * Scale, 0.0) : Vec3.ZERO;
        Scp3199Egg.laid(World, Spot, Motion, LockedYaw, Owner.getId());
    }

    public static Vec3 clearSpot(ServerLevel World, Scp3199 Owner, Vec3 Mouth) {
        EntityDimensions Size = Scp3199Registry.EggType.get().getDimensions();
        Vec3 Home = new Vec3(Owner.getX(), Mouth.y, Owner.getZ());
        for (int Step = 0; Step <= ClearSteps; Step++) {
            Vec3 Try = Mouth.lerp(Home, Step / (double) ClearSteps);
            if (World.noCollision(Size.makeBoundingBox(Try))) {
                return Step == 0 ? Mouth : Try;
            }
        }
        for (double Rise = Mouth.y; Rise >= Owner.getY(); Rise -= ClearRise) {
            Vec3 Try = new Vec3(Owner.getX(), Rise, Owner.getZ());
            if (World.noCollision(Size.makeBoundingBox(Try))) {
                return Try;
            }
        }
        return Owner.position();
    }

    public void abandon(ServerLevel World, Scp3199 Owner) {
        if (Laying >= 0 && !EggOut) {
            release(World, Owner, false);
        }
    }

    public static Vec3 direction(float Yaw) {
        return Vec3.directionFromRotation(0.0f, Yaw);
    }

    public static Vec3 mouth(Scp3199 Owner, float Yaw) {
        float Scale = Owner.growthScale();
        return Owner.position().add(direction(Yaw).scale(MouthForward * Scale)).add(0.0, MouthHeight * Scale, 0.0);
    }

    @Nullable
    public static LivingEntity spotted(ServerLevel World, Scp3199 Owner) {
        LivingEntity Best = null;
        double BestDistance = AlertRange * AlertRange;
        for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, Owner.getBoundingBox().inflate(AlertRange), Found -> Scp3199Goals.isPrey(Owner, Found))) {
            double Distance = Body.distanceToSqr(Owner);
            if (Distance < BestDistance && Owner.getSensing().hasLineOfSight(Body)) {
                Best = Body;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    public CompoundTag save() {
        CompoundTag Tag = new CompoundTag();
        if (Anchor != null) {
            Tag.put("Anchor", NbtUtils.writeBlockPos(Anchor));
        }
        Tag.putInt("SettledFor", SettledFor);
        Tag.putInt("LayWait", LayWait);
        Tag.putInt("Laying", Laying);
        Tag.putFloat("LockedYaw", LockedYaw);
        Tag.putBoolean("EggOut", EggOut);
        return Tag;
    }

    public void load(CompoundTag Tag, Scp3199 Owner) {
        Anchor = NbtUtils.readBlockPos(Tag, "Anchor").orElse(null);
        SettledFor = Math.max(0, Tag.getInt("SettledFor"));
        LayWait = Math.max(0, Tag.getInt("LayWait"));
        Laying = Tag.contains("Laying") ? Tag.getInt("Laying") : -1;
        LockedYaw = Tag.getFloat("LockedYaw");
        EggOut = Tag.getBoolean("EggOut");
        Owner.setLaying(Laying >= 0);
    }

    public static boolean standing(ServerLevel World, Scp3199 Owner) {
        if (Owner.onGround()) {
            return true;
        }
        BlockPos Feet = BlockPos.containing(Owner.getX(), Owner.getY() - Lift, Owner.getZ());
        VoxelShape Floor = World.getBlockState(Feet).getCollisionShape(World, Feet);
        return !Floor.isEmpty() && Math.abs(Feet.getY() + Floor.max(Direction.Axis.Y) - Owner.getY()) < Lift;
    }
}
