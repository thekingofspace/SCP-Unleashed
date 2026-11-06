package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;

public final class Scp407Fusion {
    public static final long FuseDelay = 600L;
    public static final int StageTicks = 12;
    public static final int Stages = 3;
    public static final double Drift = 0.35;
    public static final double Rise = 1.2;
    public static final int GroundSearch = 3;
    public static final ResourceLocation HeldId = Scp407Registry.location("scp407_held");

    private Scp407Fusion() {
    }

    public static long fuseAt(Scp407Session Session, Scp407Session.Listener Entry) {
        return Math.max(Session.ActTick, Entry.FirstHeard) + FuseDelay;
    }

    public static void tick(ServerLevel World, Scp407Session Session, long Position) {
        long Now = World.getGameTime();
        for (Scp407Session.Listener Entry : Session.Listeners.values()) {
            if (Entry.Struck) {
                continue;
            }
            if (!Entry.Fused) {
                if (Position < fuseAt(Session, Entry) || !Scp407Hearing.hearingNow(Entry, Now)) {
                    continue;
                }
                LivingEntity Target = Scp407Hearing.resolveIn(World, Entry.Key);
                if (Target == null || !Target.isAlive() || !Targeting.isTargetable(Target)) {
                    continue;
                }
                free(Target);
                if (!Session.Region.contains(Target.blockPosition().asLong())) {
                    continue;
                }
                begin(World, Entry, Target);
            }
            if (Entry.Fused && Entry.PodStage < Stages && Now >= Entry.PodTick + StageTicks) {
                build(World, Session, Entry);
            }
        }
    }

    public static void begin(ServerLevel World, Scp407Session.Listener Entry, LivingEntity Target) {
        free(Target);
        double Ground = groundUnder(World, Target);
        BlockPos Feet = BlockPos.containing(Target.getX(), Ground, Target.getZ());
        Entry.Fused = true;
        Entry.Anchor = Feet;
        Entry.HoldX = Feet.getX() + 0.5;
        Entry.HoldZ = Feet.getZ() + 0.5;
        Entry.HoldY = Ground;
        Entry.PodStage = 0;
        Entry.PodTick = World.getGameTime() - StageTicks;
        settle(Target, Entry);
        hold(Target);
        World.sendParticles(Scp407Registry.Spore.get(), Entry.HoldX, Entry.HoldY + 1.0, Entry.HoldZ, 24, 0.4, 0.7, 0.4, 0.01);
    }

    public static double groundUnder(ServerLevel World, LivingEntity Target) {
        BlockPos Feet = Target.blockPosition();
        if (Target.onGround()) {
            return Target.getY();
        }
        if (Target.isInWater() || Target.isInLava() || Target.isFallFlying() || Target instanceof Player Flyer && Flyer.getAbilities().flying) {
            return Feet.getY();
        }
        BlockPos.MutableBlockPos Cursor = Feet.mutable();
        for (int Step = 0; Step < GroundSearch; Step++) {
            BlockPos Below = Cursor.below();
            VoxelShape Shape = World.getBlockState(Below).getCollisionShape(World, Below);
            if (!Shape.isEmpty()) {
                return Below.getY() + Shape.max(Direction.Axis.Y);
            }
            Cursor.move(Direction.DOWN);
        }
        return Feet.getY();
    }

    private static void build(ServerLevel World, Scp407Session Session, Scp407Session.Listener Entry) {
        BlockPos Anchor = Entry.Anchor;
        if (Anchor == null) {
            Entry.PodStage = Stages;
            return;
        }
        int Stage = ++Entry.PodStage;
        Entry.PodTick = World.getGameTime();
        BlockState Pod = Scp407Registry.Pod.get().defaultBlockState();
        if (Stage <= 2) {
            BlockPos Ring = Stage == 1 ? Anchor : Anchor.above();
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                Scp407Growth.place(World, Session, Ring.relative(Side), Pod.setValue(Scp407PodBlock.Facing, Side).setValue(Scp407PodBlock.Shape, Scp407PodBlock.Part.Side));
                Scp407Growth.place(World, Session, Ring.relative(Side).relative(Side.getClockWise()),
                        Pod.setValue(Scp407PodBlock.Facing, Side).setValue(Scp407PodBlock.Shape, Scp407PodBlock.Part.Corner));
            }
        } else {
            BlockPos Top = Anchor.above(2);
            Scp407Growth.place(World, Session, Top, Pod.setValue(Scp407PodBlock.Shape, Scp407PodBlock.Part.Cap));
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                Scp407Growth.place(World, Session, Top.relative(Side), Pod.setValue(Scp407PodBlock.Facing, Side).setValue(Scp407PodBlock.Shape, Scp407PodBlock.Part.Shoulder));
            }
        }
        World.playSound(null, Anchor, Scp407Registry.PodClose.get(), SoundSource.BLOCKS, 0.8f, 0.8f + Stage * 0.1f);
        World.sendParticles(Scp407Registry.Leaf.get(), Entry.HoldX, Anchor.getY() + Stage * 0.7, Entry.HoldZ, 8, 0.6, 0.3, 0.6, 0.02);
    }

    public static void holdAll(ServerLevel World, Scp407Session Session) {
        for (Scp407Session.Listener Entry : Session.Listeners.values()) {
            if (!Entry.Fused || Entry.Struck) {
                continue;
            }
            LivingEntity Target = Scp407Hearing.resolveIn(World, Entry.Key);
            if (Target == null || !Target.isAlive()) {
                continue;
            }
            free(Target);
            hold(Target);
            settle(Target, Entry);
        }
    }

    public static void free(LivingEntity Target) {
        if (Target.isPassenger()) {
            Target.stopRiding();
        }
        if (Target.isSleeping()) {
            Target.stopSleeping();
        }
    }

    public static void unholdStrays(ServerLevel World, Scp407Sessions Data) {
        for (ServerPlayer User : World.players()) {
            if (isHeld(User) && Data.holding(Scp407Hearing.keyOf(User)) == null) {
                release(User);
            }
        }
    }

    public static boolean refusesStasis(Entity Target) {
        if (!(Target instanceof LivingEntity Living)) {
            return false;
        }
        if (isHeld(Living)) {
            return true;
        }
        return Living.level() instanceof ServerLevel World && Scp407Sessions.get(World).holding(Scp407Hearing.keyOf(Living)) != null;
    }

    private static void settle(LivingEntity Target, Scp407Session.Listener Entry) {
        double Dx = Target.getX() - Entry.HoldX;
        double Dz = Target.getZ() - Entry.HoldZ;
        double Dy = Target.getY() - Entry.HoldY;
        if (Dx * Dx + Dz * Dz > Drift * Drift || Math.abs(Dy) > Rise) {
            if (Target instanceof ServerPlayer User) {
                User.teleportTo(Entry.HoldX, Entry.HoldY, Entry.HoldZ);
            } else {
                Target.teleportTo(Entry.HoldX, Entry.HoldY, Entry.HoldZ);
            }
        }
        Vec3 Motion = Target.getDeltaMovement();
        Target.setDeltaMovement(0.0, Math.min(0.0, Motion.y), 0.0);
        if (Target instanceof Mob Body) {
            Body.getNavigation().stop();
        }
    }

    public static void hold(LivingEntity Target) {
        modify(Target.getAttribute(Attributes.MOVEMENT_SPEED));
        modify(Target.getAttribute(Attributes.JUMP_STRENGTH));
    }

    private static void modify(AttributeInstance Attribute) {
        if (Attribute != null && !Attribute.hasModifier(HeldId)) {
            Attribute.addTransientModifier(new AttributeModifier(HeldId, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void release(LivingEntity Target) {
        AttributeInstance Speed = Target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null) {
            Speed.removeModifier(HeldId);
        }
        AttributeInstance Jump = Target.getAttribute(Attributes.JUMP_STRENGTH);
        if (Jump != null) {
            Jump.removeModifier(HeldId);
        }
    }

    public static boolean isHeld(LivingEntity Target) {
        AttributeInstance Speed = Target.getAttribute(Attributes.MOVEMENT_SPEED);
        return Speed != null && Speed.hasModifier(HeldId);
    }
}
