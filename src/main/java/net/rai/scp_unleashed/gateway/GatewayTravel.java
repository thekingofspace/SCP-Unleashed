package net.rai.scp_unleashed.gateway;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class GatewayTravel {
    public static final String ReturnKey = "scp_unleashed_gateway_return";
    public static final int SafeRadius = 6;
    public static final int SafeVertical = 4;

    private GatewayTravel() {
    }

    public static boolean exempt(Player User) {
        return User.isCreative() && User.hasPermissions(2);
    }

    public static boolean inside(Player User, GatewayDestination Destination) {
        return User.level().dimension() == Destination.Dimension();
    }

    public static boolean enter(ServerPlayer User, GatewayDestination Destination, GatewayStash.Spot Return) {
        ServerLevel Target = User.server.getLevel(Destination.Dimension());
        if (Target == null || !User.isAlive() || User.isSpectator() || inside(User, Destination)) {
            return false;
        }
        if (!Destination.Hooks().canEnter(User, Target)) {
            return false;
        }
        Destination.Hooks().prepare(Target);
        if (Destination.Rules().StashBelongings()) {
            GatewayStash Stash = GatewayStash.get(User.server);
            GatewayStash.Entry Old = Stash.entry(User.getUUID());
            if (Old != null) {
                if (GatewayStash.matches(User, Old)) {
                    Stash.restore(User);
                } else {
                    Stash.discard(User.getUUID());
                }
            }
            if (!Stash.stash(User, Destination.Id(), Return, Destination.Rules().StashExperience())) {
                return false;
            }
        }
        setReturn(User, Return);
        GatewayDestination.Arrival Arrival = Destination.Entry().locate(Target, User);
        User.level().playSound(null, User.getX(), User.getY(), User.getZ(), GatewayRegistry.Travel.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        move(User, Target, Arrival.Pos(), Arrival.Yaw());
        Destination.Hooks().arrived(User, Target);
        return true;
    }

    public static void leave(ServerPlayer User) {
        GatewayStash.Spot Back = returnOf(User);
        ServerLevel Target = User.server.getLevel(Back.Dimension());
        if (Target == null || GatewayDestinations.sealed(Back.Dimension())) {
            Back = respawnSpot(User);
            Target = User.server.getLevel(Back.Dimension());
            if (Target == null) {
                Target = User.server.overworld();
            }
        }
        Vec3 Safe = safeSpot(Target, Back.Pos(), EntityType.PLAYER);
        move(User, Target, Safe, Back.Yaw());
        clearReturn(User);
        Target.playSound(null, Safe.x, Safe.y, Safe.z, GatewayRegistry.Travel.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    public static void setReturn(Player User, GatewayStash.Spot Spot) {
        CompoundTag Root = User.getPersistentData();
        CompoundTag Persisted = Root.getCompound(Player.PERSISTED_NBT_TAG);
        Persisted.put(ReturnKey, Spot.save());
        Root.put(Player.PERSISTED_NBT_TAG, Persisted);
    }

    public static void clearReturn(Player User) {
        CompoundTag Root = User.getPersistentData();
        CompoundTag Persisted = Root.getCompound(Player.PERSISTED_NBT_TAG);
        Persisted.remove(ReturnKey);
        Root.put(Player.PERSISTED_NBT_TAG, Persisted);
    }

    @Nullable
    public static GatewayStash.Spot storedReturn(Player User) {
        CompoundTag Persisted = User.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return Persisted.contains(ReturnKey) ? GatewayStash.Spot.load(Persisted.getCompound(ReturnKey)) : null;
    }

    public static GatewayStash.Spot returnOf(ServerPlayer User) {
        GatewayStash.Entry Found = GatewayStash.get(User.server).entry(User.getUUID());
        if (Found != null && User.server.getLevel(Found.Return.Dimension()) != null) {
            return Found.Return;
        }
        GatewayStash.Spot Stored = storedReturn(User);
        if (Stored != null && User.server.getLevel(Stored.Dimension()) != null) {
            return Stored;
        }
        return respawnSpot(User);
    }

    public static GatewayStash.Spot respawnSpot(ServerPlayer User) {
        DimensionTransition Home = User.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        if (GatewayDestinations.sealed(Home.newLevel().dimension())) {
            ServerLevel World = User.server.overworld();
            return new GatewayStash.Spot(World.dimension(), Vec3.atBottomCenterOf(World.getSharedSpawnPos()), 0.0f);
        }
        return new GatewayStash.Spot(Home.newLevel().dimension(), Home.pos(), Home.yRot());
    }

    public static void move(ServerPlayer User, ServerLevel World, Vec3 Spot, float Yaw) {
        User.stopRiding();
        if (User.isSleeping()) {
            User.stopSleepInBed(true, true);
        }
        if (User.isFallFlying()) {
            User.stopFallFlying();
        }
        World.getChunkAt(BlockPos.containing(Spot));
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
    }

    public static Vec3 safeSpot(ServerLevel World, Vec3 Near, EntityType<?> Type) {
        BlockPos Centre = BlockPos.containing(Near);
        World.getChunkAt(Centre);
        for (int Radius = 0; Radius <= SafeRadius; Radius++) {
            for (int Dy = 0; Dy <= SafeVertical * 2; Dy++) {
                int Offset = Dy % 2 == 0 ? Dy / 2 : -(Dy + 1) / 2;
                for (int Dx = -Radius; Dx <= Radius; Dx++) {
                    for (int Dz = -Radius; Dz <= Radius; Dz++) {
                        if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Radius) {
                            continue;
                        }
                        BlockPos Feet = Centre.offset(Dx, Offset, Dz);
                        if (standable(World, Feet, Type)) {
                            return Radius == 0 && Offset == 0 ? new Vec3(Near.x, Feet.getY(), Near.z) : Vec3.atBottomCenterOf(Feet);
                        }
                    }
                }
            }
        }
        BlockPos Top = World.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Centre);
        return Vec3.atBottomCenterOf(Top);
    }

    public static boolean standable(ServerLevel World, BlockPos Feet, EntityType<?> Type) {
        if (World.isOutsideBuildHeight(Feet) || !World.getWorldBorder().isWithinBounds(Feet)) {
            return false;
        }
        BlockPos Below = Feet.below();
        BlockState Floor = World.getBlockState(Below);
        if (!Floor.isFaceSturdy(World, Below, Direction.UP)) {
            return false;
        }
        if (!World.getFluidState(Feet).isEmpty() || !World.getFluidState(Feet.above()).isEmpty() || World.getFluidState(Below).is(FluidTags.LAVA)) {
            return false;
        }
        AABB Box = Type.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Feet));
        return World.noCollision(Box);
    }
}
