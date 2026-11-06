package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.scp1730.block.Scp1730SymbolBlock;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.scp1730.network.Scp1730HoldPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp1730Symbols {
    public static final double LookRange = 14.0;
    public static final double MinCone = 4.0;
    public static final double SymbolHalf = 0.45;
    public static final double FaceInset = 0.47;
    public static final int ScanEvery = 2;
    public static final int WallDamageEvery = 10;
    public static final float WallDamageBase = 1.0f;
    public static final float WallDamageRamp = 0.25f;
    public static final double GuardianRange = 24.0;
    public static final double GuardianNear = 6.0;
    public static final double GuardianCone = 70.0;
    public static final int GuardianDamageEvery = 5;
    public static final float GuardianDamage = 3.0f;
    public static final float WallPull = 0.9f;
    public static final float GuardianPull = 0.9f;
    public static final int SyncEvery = 5;
    public static final net.minecraft.resources.ResourceLocation SpeedId = Scp1730Registry.location("scp1730_hold_speed");
    public static final net.minecraft.resources.ResourceLocation JumpId = Scp1730Registry.location("scp1730_hold_jump");
    private static final Map<ResourceKey<Level>, Set<BlockPos>> Index = new ConcurrentHashMap<>();
    private static final Map<UUID, Hold> Holds = new ConcurrentHashMap<>();

    public static final class Hold {
        public final boolean Guardian;
        public final BlockPos Symbol;
        public final int EntityId;
        public Vec3 Target;
        public int Ticks;

        private Hold(boolean Guardian, @Nullable BlockPos Symbol, int EntityId, Vec3 Target) {
            this.Guardian = Guardian;
            this.Symbol = Symbol;
            this.EntityId = EntityId;
            this.Target = Target;
        }
    }

    private Scp1730Symbols() {
    }

    public static void indexed(Level World, BlockPos Pos, boolean Present) {
        if (World.isClientSide) {
            return;
        }
        Set<BlockPos> Found = Index.computeIfAbsent(World.dimension(), Key -> ConcurrentHashMap.newKeySet());
        if (Present) {
            Found.add(Pos.immutable());
        } else {
            Found.remove(Pos);
        }
    }

    public static int indexedCount(Level World) {
        Set<BlockPos> Found = Index.get(World.dimension());
        return Found == null ? 0 : Found.size();
    }

    public static int countNear(Level World, BlockPos Centre, double Radius) {
        Set<BlockPos> Found = Index.get(World.dimension());
        if (Found == null) {
            return 0;
        }
        int Count = 0;
        double Limit = Radius * Radius;
        for (BlockPos Pos : Found) {
            if (Pos.distSqr(Centre) <= Limit) {
                Count++;
            }
        }
        return Count;
    }

    public static void clear() {
        Index.clear();
        Holds.clear();
    }

    @Nullable
    public static Hold holdOf(Player User) {
        return Holds.get(User.getUUID());
    }

    public static boolean held(Player User) {
        return Holds.containsKey(User.getUUID());
    }

    public static boolean exempt(Player User) {
        return User.isSpectator() || User.isCreative() || AntiMemetic.isProtected(User);
    }

    public static Vec3 faceCentre(BlockPos Pos, Direction Facing) {
        return Vec3.atCenterOf(Pos).subtract(Facing.getStepX() * FaceInset, Facing.getStepY() * FaceInset, Facing.getStepZ() * FaceInset);
    }

    public static double angleTo(Vec3 Eye, Vec3 Look, Vec3 Point) {
        Vec3 Way = Point.subtract(Eye);
        double Length = Way.length();
        if (Length < 1.0e-6) {
            return 0.0;
        }
        double Dot = Math.max(-1.0, Math.min(1.0, Look.normalize().dot(Way.scale(1.0 / Length))));
        return Math.toDegrees(Math.acos(Dot));
    }

    public static double cone(double Distance) {
        return Math.max(MinCone, Math.toDegrees(Math.atan2(SymbolHalf, Math.max(0.1, Distance))));
    }

    public static boolean looking(Vec3 Eye, Vec3 Look, BlockPos Pos, Direction Facing) {
        Vec3 Centre = faceCentre(Pos, Facing);
        Vec3 Normal = new Vec3(Facing.getStepX(), Facing.getStepY(), Facing.getStepZ());
        if (Eye.subtract(Centre).dot(Normal) <= 0.05) {
            return false;
        }
        double Distance = Eye.distanceTo(Centre);
        return Distance <= LookRange && angleTo(Eye, Look, Centre) <= cone(Distance);
    }

    public static boolean clearSight(Level World, Entity Viewer, Vec3 Eye, Vec3 Target, @Nullable BlockPos Ignored, @Nullable Entity Seen) {
        BlockHitResult Hit = World.clip(new ClipContext(Eye, Target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Viewer));
        if (Hit.getType() == HitResult.Type.BLOCK && (Ignored == null || !Hit.getBlockPos().equals(Ignored))) {
            if (Hit.getLocation().distanceToSqr(Target) > 0.04) {
                return false;
            }
        }
        AABB Span = new AABB(Eye, Target).inflate(0.6);
        for (Entity Other : World.getEntities(Viewer, Span, Each -> Each instanceof LivingEntity && Each != Seen && !Each.isSpectator() && Each.isAlive())) {
            if (Other.getBoundingBox().inflate(0.05).clip(Eye, Target).isPresent()) {
                return false;
            }
        }
        return true;
    }

    public static void tick(ServerPlayer User) {
        Hold Current = Holds.get(User.getUUID());
        if (Current != null) {
            tickHold(User, Current);
            return;
        }
        if (exempt(User) || !User.isAlive() || (User.tickCount + User.getId()) % ScanEvery != 0) {
            return;
        }
        BlockPos Seen = scan(User);
        if (Seen != null) {
            start(User, new Hold(false, Seen, -1, faceCentre(Seen, User.level().getBlockState(Seen).getValue(Scp1730SymbolBlock.Facing))));
            return;
        }
        Scp1730Guardian Watching = guardianSeen(User);
        if (Watching != null) {
            start(User, new Hold(true, null, Watching.getId(), Watching.glyphPoint()));
        }
    }

    @Nullable
    public static BlockPos scan(ServerPlayer User) {
        Set<BlockPos> Found = Index.get(User.level().dimension());
        if (Found == null || Found.isEmpty()) {
            return null;
        }
        Vec3 Eye = User.getEyePosition();
        Vec3 Look = User.getViewVector(1.0f);
        double Limit = (LookRange + 1.0) * (LookRange + 1.0);
        BlockPos Best = null;
        double BestAngle = Double.MAX_VALUE;
        for (BlockPos Pos : Found) {
            if (Pos.distToCenterSqr(Eye) > Limit || !User.level().isLoaded(Pos)) {
                continue;
            }
            BlockState State = User.level().getBlockState(Pos);
            if (!(State.getBlock() instanceof Scp1730SymbolBlock)) {
                continue;
            }
            Direction Facing = State.getValue(Scp1730SymbolBlock.Facing);
            if (!looking(Eye, Look, Pos, Facing)) {
                continue;
            }
            Vec3 Centre = faceCentre(Pos, Facing);
            double Angle = angleTo(Eye, Look, Centre);
            if (Angle < BestAngle && clearSight(User.level(), User, Eye, Centre.add(Facing.getStepX() * 0.05, Facing.getStepY() * 0.05, Facing.getStepZ() * 0.05), Pos, null)) {
                Best = Pos.immutable();
                BestAngle = Angle;
            }
        }
        return Best;
    }

    @Nullable
    public static Scp1730Guardian guardianSeen(ServerPlayer User) {
        Vec3 Eye = User.getEyePosition();
        Vec3 Look = User.getViewVector(1.0f);
        for (Scp1730Guardian Guardian : User.level().getEntitiesOfClass(Scp1730Guardian.class, User.getBoundingBox().inflate(GuardianRange), Scp1730Guardian::isAlive)) {
            if (Guardian.isDeadOrDying()) {
                continue;
            }
            Vec3 Point = Guardian.glyphPoint();
            double Distance = Eye.distanceTo(Point);
            if (Distance > GuardianRange) {
                continue;
            }
            if (Distance <= GuardianNear) {
                return Guardian;
            }
            if (angleTo(Eye, Look, Point) <= GuardianCone && clearSight(User.level(), User, Eye, Point, null, Guardian)) {
                return Guardian;
            }
        }
        return null;
    }

    private static void start(ServerPlayer User, Hold Made) {
        Holds.put(User.getUUID(), Made);
        freeze(User, true);
        User.level().playSound(null, User.getX(), User.getY(), User.getZ(), Scp1730Registry.SymbolGaze.get(), SoundSource.PLAYERS, 1.0f, Made.Guardian ? 0.7f : 1.0f);
        sync(User, Made);
    }

    private static void sync(ServerPlayer User, Hold Made) {
        PacketDistributor.sendToPlayer(User, new Scp1730HoldPayload(true, Made.Target.x, Made.Target.y, Made.Target.z, Made.Guardian ? GuardianPull : WallPull));
    }

    public static void release(ServerPlayer User) {
        Hold Gone = Holds.remove(User.getUUID());
        freeze(User, false);
        if (Gone != null) {
            PacketDistributor.sendToPlayer(User, Scp1730HoldPayload.released());
        }
    }

    public static void forget(UUID Id) {
        Holds.remove(Id);
    }

    public static void freeze(LivingEntity User, boolean Frozen) {
        AttributeInstance Speed = User.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance Jump = User.getAttribute(Attributes.JUMP_STRENGTH);
        if (Frozen) {
            if (Speed != null && !Speed.hasModifier(SpeedId)) {
                Speed.addTransientModifier(new AttributeModifier(SpeedId, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            if (Jump != null && !Jump.hasModifier(JumpId)) {
                Jump.addTransientModifier(new AttributeModifier(JumpId, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else {
            if (Speed != null) {
                Speed.removeModifier(SpeedId);
            }
            if (Jump != null) {
                Jump.removeModifier(JumpId);
            }
        }
    }

    private static boolean stillValid(ServerPlayer User, Hold Current) {
        if (!User.isAlive() || exempt(User)) {
            return false;
        }
        Level World = User.level();
        Vec3 Eye = User.getEyePosition();
        if (Current.Guardian) {
            Entity Found = World.getEntity(Current.EntityId);
            if (!(Found instanceof Scp1730Guardian Guardian) || !Guardian.isAlive() || Guardian.isDeadOrDying()) {
                return false;
            }
            Current.Target = Guardian.glyphPoint();
            double Distance = Eye.distanceTo(Current.Target);
            if (Distance > GuardianRange + 4.0) {
                return false;
            }
            return Distance <= GuardianNear || clearSight(World, User, Eye, Current.Target, null, Guardian);
        }
        BlockState State = World.getBlockState(Current.Symbol);
        if (!(State.getBlock() instanceof Scp1730SymbolBlock)) {
            return false;
        }
        Direction Facing = State.getValue(Scp1730SymbolBlock.Facing);
        Vec3 Centre = faceCentre(Current.Symbol, Facing);
        if (Eye.distanceTo(Centre) > LookRange + 4.0) {
            return false;
        }
        return clearSight(World, User, Eye, Centre.add(Facing.getStepX() * 0.05, Facing.getStepY() * 0.05, Facing.getStepZ() * 0.05), Current.Symbol, null);
    }

    private static void tickHold(ServerPlayer User, Hold Current) {
        if (!stillValid(User, Current)) {
            release(User);
            return;
        }
        Current.Ticks++;
        User.setDeltaMovement(0.0, Math.min(0.0, User.getDeltaMovement().y), 0.0);
        User.hurtMarked = true;
        User.setRemainingFireTicks(Math.max(User.getRemainingFireTicks(), 60));
        ServerLevel World = User.serverLevel();
        if (Current.Guardian) {
            if (Current.Ticks % SyncEvery == 0) {
                sync(User, Current);
            }
            if (Current.Ticks % GuardianDamageEvery == 0) {
                User.hurt(source(World), GuardianDamage + Current.Ticks * 0.05f);
            }
        } else if (Current.Ticks % WallDamageEvery == 0) {
            User.hurt(source(World), WallDamageBase + WallDamageRamp * (Current.Ticks / (float) WallDamageEvery));
        }
        if (Current.Ticks % 20 == 0) {
            World.playSound(null, User.getX(), User.getY(), User.getZ(), Scp1730Registry.SymbolBurn.get(), SoundSource.PLAYERS, 0.8f, 0.9f + World.random.nextFloat() * 0.2f);
        }
        if (!User.isAlive()) {
            release(User);
        }
    }

    public static DamageSource source(ServerLevel World) {
        Holder<net.minecraft.world.damagesource.DamageType> Type = World.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(Scp1730Registry.SymbolDamage);
        return new DamageSource(Type);
    }
}
