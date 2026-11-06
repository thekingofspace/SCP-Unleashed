package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.rai.scp_unleashed.anomaly.Targeting;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp354Grab {
    public static final ResourceLocation SpeedLock = Scp354Registry.location("scp354_grasp_speed");
    public static final ResourceLocation JumpLock = Scp354Registry.location("scp354_grasp_jump");
    public static final ResourceLocation SwimLock = Scp354Registry.location("scp354_grasp_swim");
    public static final long SpareTicks = 200L;
    public static final double SurfaceSlack = 0.05;
    private static final Map<UUID, Integer> Seized = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> Spared = new ConcurrentHashMap<>();

    private Scp354Grab() {
    }

    public static boolean isEligible(LivingEntity Target) {
        if (!Target.isAlive() || Target.isRemoved() || Target.isSpectator() || Target instanceof ArmorStand || !Targeting.isTargetable(Target)) {
            return false;
        }
        if (Target.getType().is(Scp354Registry.Spawns)) {
            return false;
        }
        return !(Target instanceof Player User) || !User.isCreative() && !User.isSpectator();
    }

    public static boolean isSeized(Entity Target) {
        return Seized.containsKey(Target.getUUID());
    }

    public static boolean isLocked(LivingEntity Target) {
        AttributeInstance Speed = Target.getAttribute(Attributes.MOVEMENT_SPEED);
        return Speed != null && Speed.hasModifier(SpeedLock);
    }

    public static void touch(ServerLevel World, LivingEntity Target, BlockPos Pos) {
        if (!isEligible(Target)) {
            return;
        }
        double Surface = Pos.getY() + World.getFluidState(Pos).getHeight(World, Pos);
        if (Target.getY() > Surface + SurfaceSlack) {
            return;
        }
        Integer Keeper = Seized.get(Target.getUUID());
        if (Keeper != null) {
            if (World.getEntity(Keeper) instanceof Scp354Grasp Grasp && !Grasp.isRemoved() && Grasp.holds(Target)) {
                return;
            }
            Seized.remove(Target.getUUID());
        }
        Long Until = Spared.get(Target.getUUID());
        if (Until != null) {
            if (World.getGameTime() < Until) {
                return;
            }
            Spared.remove(Target.getUUID());
        }
        seize(World, Target, Pos);
    }

    public static Scp354Grasp seize(ServerLevel World, LivingEntity Target, BlockPos Pos) {
        BlockPos Floor = Pos;
        while (Scp354Fluid.isRed(World.getFluidState(Floor.below())) && Floor.getY() > World.getMinBuildHeight()) {
            Floor = Floor.below();
        }
        double FloorY = Floor.getY() + 0.02;
        Vec3 Start = new Vec3(Target.getX(), Math.max(FloorY, Target.getY()), Target.getZ());
        Target.stopRiding();
        Target.ejectPassengers();
        Scp354Grasp Grasp = new Scp354Grasp(Scp354Registry.Grasp.get(), World);
        Grasp.setup(Target, Start, FloorY);
        if (!World.addFreshEntity(Grasp)) {
            return null;
        }
        Seized.put(Target.getUUID(), Grasp.getId());
        lock(Target, Grasp);
        return Grasp;
    }

    public static void release(LivingEntity Target, boolean Spare) {
        Seized.remove(Target.getUUID());
        unlock(Target);
        if (Spare) {
            Spared.put(Target.getUUID(), Target.level().getGameTime() + SpareTicks);
        }
    }

    public static void forget(UUID Target) {
        Seized.remove(Target);
    }

    public static void clear() {
        Seized.clear();
        Spared.clear();
    }

    public static void lock(LivingEntity Target, Scp354Grasp Grasp) {
        add(Target, Attributes.MOVEMENT_SPEED, SpeedLock);
        add(Target, Attributes.JUMP_STRENGTH, JumpLock);
        add(Target, NeoForgeMod.SWIM_SPEED, SwimLock);
        Target.setSprinting(false);
        Target.setSwimming(false);
        Target.stopUsingItem();
        if (Target instanceof Mob Body) {
            Grasp.rememberAi(Body.isNoAi());
            Body.getNavigation().stop();
            Body.setTarget(null);
            Body.setNoAi(true);
        }
    }

    public static void unlock(LivingEntity Target) {
        remove(Target, Attributes.MOVEMENT_SPEED, SpeedLock);
        remove(Target, Attributes.JUMP_STRENGTH, JumpLock);
        remove(Target, NeoForgeMod.SWIM_SPEED, SwimLock);
    }

    public static void hold(LivingEntity Target, Vec3 Point) {
        Target.fallDistance = 0.0f;
        if (Target.isPassenger()) {
            Target.stopRiding();
        }
        if (Target instanceof ServerPlayer User) {
            Vec3 Gap = Point.subtract(User.position());
            if (Gap.length() > Scp354Grasp.SnapDistance) {
                User.connection.teleport(Point.x, Point.y, Point.z, 0.0f, 0.0f, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT));
                User.setDeltaMovement(Vec3.ZERO);
            } else {
                User.setDeltaMovement(Gap.scale(Scp354Grasp.Follow));
            }
            User.hurtMarked = true;
            return;
        }
        Target.setPos(Point.x, Point.y, Point.z);
        Target.setDeltaMovement(Vec3.ZERO);
        if (Target instanceof Mob Body) {
            Body.getNavigation().stop();
        }
    }

    private static void add(LivingEntity Target, Holder<Attribute> Kind, ResourceLocation Id) {
        AttributeInstance Instance = Target.getAttribute(Kind);
        if (Instance != null && !Instance.hasModifier(Id)) {
            Instance.addTransientModifier(new AttributeModifier(Id, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void remove(LivingEntity Target, Holder<Attribute> Kind, ResourceLocation Id) {
        AttributeInstance Instance = Target.getAttribute(Kind);
        if (Instance != null) {
            Instance.removeModifier(Id);
        }
    }
}
