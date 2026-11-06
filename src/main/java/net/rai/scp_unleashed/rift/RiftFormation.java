package net.rai.scp_unleashed.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class RiftFormation {
    public static final String TimerKey = "scp_unleashed_rift_formation";
    public static final int MinInterval = 9600;
    public static final int MaxInterval = 18000;
    public static final int MinDistance = 24;
    public static final int MaxDistance = 64;
    public static final int CapRadius = 128;
    public static final int CapCount = 3;
    public static final int Attempts = 24;
    private static final int[] Weights = {460, 280, 160, 85, 15};
    private static final int WeightTotal = 1000;

    private RiftFormation() {
    }

    public static boolean allowedIn(ResourceKey<Level> Dimension) {
        return Level.OVERWORLD.equals(Dimension);
    }

    public static boolean allowedIn(Level World) {
        return allowedIn(World.dimension());
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || World.getGameTime() % 20 != 7 || !allowedIn(World)) {
            return;
        }
        long Now = World.getGameTime();
        for (ServerPlayer User : World.players()) {
            tickPlayer(World, User, Now);
        }
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent Event) {
        if (Event.getEntity() instanceof RiftEntity && !Event.getLevel().isClientSide() && !Event.loadedFromDisk() && !allowedIn(Event.getLevel())) {
            Event.setCanceled(true);
        }
    }

    public static boolean tickPlayer(ServerLevel World, ServerPlayer User, long Now) {
        if (User.isSpectator() || !allowedIn(World) || !RiftRegistry.spawning(World.getGameRules())) {
            return false;
        }
        CompoundTag Data = User.getPersistentData();
        long Next = Data.getLong(TimerKey);
        if (Next <= 0L || Next > Now + MaxInterval * 2L) {
            Data.putLong(TimerKey, Now + nextInterval(World.random));
            return false;
        }
        if (Now < Next) {
            return false;
        }
        Data.putLong(TimerKey, Now + nextInterval(World.random));
        tryForm(World, User);
        return true;
    }

    public static int nextInterval(RandomSource Random) {
        return MinInterval + Random.nextInt(MaxInterval - MinInterval + 1);
    }

    public static FragmentRarity rollRarity(RandomSource Random) {
        int Roll = Random.nextInt(WeightTotal);
        for (FragmentRarity Rarity : FragmentRarity.values()) {
            Roll -= Weights[Rarity.getTier() - 1];
            if (Roll < 0) {
                return Rarity;
            }
        }
        return FragmentRarity.Faint;
    }

    public static int countNearby(ServerLevel World, Vec3 Center, double Radius) {
        return World.getEntities(RiftRegistry.Rift.get(), new AABB(Center, Center).inflate(Radius), Rift -> Rift.distanceToSqr(Center) <= Radius * Radius).size();
    }

    public static boolean tryForm(ServerLevel World, ServerPlayer User) {
        return formAround(World, User.position(), MinDistance, MaxDistance);
    }

    public static boolean formAround(ServerLevel World, Vec3 Centre, double Near, double Far) {
        if (!allowedIn(World) || countNearby(World, Centre, CapRadius) >= CapCount) {
            return false;
        }
        RandomSource Random = World.random;
        FragmentRarity Rarity = rollRarity(Random);
        boolean Open = World.dimensionType().hasSkyLight();
        boolean Underground = !World.canSeeSky(BlockPos.containing(Centre).above());
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = Near + Random.nextDouble() * (Far - Near);
            int X = Mth.floor(Centre.x + Math.cos(Angle) * Distance);
            int Z = Mth.floor(Centre.z + Math.sin(Angle) * Distance);
            if (!World.isPositionEntityTicking(new BlockPos(X, Mth.floor(Centre.y), Z))) {
                continue;
            }
            boolean Surface = Open || Random.nextFloat() < (Underground ? 0.25f : 0.8f);
            int Y = Surface ? World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z) : Mth.floor(Centre.y) + Random.nextInt(25) - 12;
            Vec3 Spot = findSpot(World, X, Y, Z, Rarity, Random, Open);
            if (Spot != null && countNearby(World, Spot, CapRadius) < CapCount) {
                RiftEntity Rift = RiftRegistry.Rift.get().create(World);
                if (Rift == null) {
                    return false;
                }
                Rift.setRarity(Rarity);
                Rift.moveTo(Spot.x, Spot.y, Spot.z, Random.nextFloat() * 360.0f, 0.0f);
                return World.addFreshEntity(Rift);
            }
        }
        return false;
    }

    public static Vec3 findSpot(ServerLevel World, int X, int Y, int Z, FragmentRarity Rarity, RandomSource Random) {
        return findSpot(World, X, Y, Z, Rarity, Random, false);
    }

    public static Vec3 findSpot(ServerLevel World, int X, int Y, int Z, FragmentRarity Rarity, RandomSource Random, boolean SurfaceOnly) {
        int Ground = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z);
        for (int Step = 0; Step < 14; Step++) {
            int Probe = Y + (Step % 2 == 0 ? Step / 2 : -(Step + 1) / 2);
            BlockPos Pos = new BlockPos(X, Probe, Z);
            if (World.isOutsideBuildHeight(Pos) || !World.getWorldBorder().isWithinBounds(Pos) || !World.isPositionEntityTicking(Pos)) {
                continue;
            }
            if (!World.getBlockState(Pos).isAir()) {
                continue;
            }
            int Floor = Probe;
            while (Floor > Probe - 6 && World.getBlockState(new BlockPos(X, Floor - 1, Z)).isAir()) {
                Floor--;
            }
            if (SurfaceOnly && Floor < Ground) {
                continue;
            }
            Vec3 Bottom = new Vec3(X + 0.5, Floor + 0.35 + Random.nextDouble() * 0.6, Z + 0.5);
            if (isOpen(World, RiftEntity.boxAt(Bottom, Rarity).inflate(0.3, 0.2, 0.3))) {
                return Bottom;
            }
        }
        return null;
    }

    public static boolean isOpen(ServerLevel World, AABB Box) {
        for (BlockPos Pos : BlockPos.betweenClosed(Mth.floor(Box.minX), Mth.floor(Box.minY), Mth.floor(Box.minZ), Mth.floor(Box.maxX), Mth.floor(Box.maxY), Mth.floor(Box.maxZ))) {
            if (!World.isLoaded(Pos)) {
                return false;
            }
            BlockState State = World.getBlockState(Pos);
            if (!State.getFluidState().isEmpty() || !State.getCollisionShape(World, Pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
