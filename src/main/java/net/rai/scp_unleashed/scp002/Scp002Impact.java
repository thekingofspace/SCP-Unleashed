package net.rai.scp_unleashed.scp002;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp002.network.ImpactPayload;

import java.util.Arrays;
import java.util.Optional;

public final class Scp002Impact {
    public static final float FlatRadius = 7.6f;
    public static final float RimWidth = 4.0f;
    public static final int EjectaReach = 13;
    public static final int PorchNear = 6;
    public static final int PorchFar = 7;
    public static final int SiteAttempts = 14;
    public static final double AnnounceRange = 256.0;
    public static final double BlastRadius = 9.0;
    public static final float BlastDamage = 22.0f;

    private Scp002Impact() {
    }

    public record Result(BlockPos Door, BlockPos Origin, float Radius) {
    }

    public static boolean isNatural(BlockState State) {
        return State.isAir() || State.is(BlockTags.OVERWORLD_CARVER_REPLACEABLES) || State.is(BlockTags.LOGS) || State.is(BlockTags.LEAVES)
                || State.is(BlockTags.REPLACEABLE) || State.is(BlockTags.REPLACEABLE_BY_TREES) || State.is(BlockTags.FLOWERS) || State.is(BlockTags.SAPLINGS)
                || State.is(BlockTags.SNOW) || State.is(BlockTags.ICE) || State.is(BlockTags.DIRT) || State.is(BlockTags.SAND) || State.is(BlockTags.TERRACOTTA)
                || State.is(BlockTags.BASE_STONE_NETHER) || State.is(BlockTags.NYLIUM) || State.is(Tags.Blocks.END_STONES) || State.is(Tags.Blocks.GRAVELS)
                || State.is(Tags.Blocks.ORES) || State.is(Blocks.CACTUS) || State.is(Blocks.SUGAR_CANE) || State.is(Blocks.BAMBOO) || State.is(Blocks.MOSS_BLOCK)
                || State.is(Blocks.CLAY) || State.is(Blocks.MUD) || State.is(Blocks.PUMPKIN) || State.is(Blocks.MELON) || State.is(Blocks.MUSHROOM_STEM)
                || State.is(Blocks.RED_MUSHROOM_BLOCK) || State.is(Blocks.BROWN_MUSHROOM_BLOCK);
    }

    public static boolean isProtected(ServerLevel World, BlockPos Pos, BlockState State) {
        return State.getDestroySpeed(World, Pos) < 0.0f || State.hasBlockEntity() || State.getBlock().getExplosionResistance() >= 1200.0f
                || State.is(BlockTags.WITHER_IMMUNE) || FleshBlock.isFlesh(State);
    }

    private static boolean mayChange(ServerLevel World, BlockPos Pos, boolean Griefing) {
        BlockState State = World.getBlockState(Pos);
        if (!State.getFluidState().isEmpty() || isProtected(World, Pos, State)) {
            return false;
        }
        return Griefing || isNatural(State);
    }

    private static boolean isOpenCell(BlockState State) {
        return State.isAir() || State.canBeReplaced() && State.getFluidState().isEmpty();
    }

    private static boolean isGround(BlockState State) {
        return !State.isAir() && !State.is(BlockTags.LEAVES) && !State.is(BlockTags.LOGS) && !State.canBeReplaced() && State.getFluidState().isEmpty()
                && !State.is(Blocks.BAMBOO) && !State.is(Blocks.CACTUS) && !State.is(Blocks.SUGAR_CANE) && !State.is(Blocks.MUSHROOM_STEM)
                && !State.is(Blocks.RED_MUSHROOM_BLOCK) && !State.is(Blocks.BROWN_MUSHROOM_BLOCK);
    }

    public static int groundY(ServerLevel World, int X, int Z) {
        int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z) - 1;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, Top, Z);
        int Floor = World.getMinBuildHeight();
        for (int Step = 0; Step < 48 && Cursor.getY() > Floor; Step++) {
            BlockState State = World.getBlockState(Cursor);
            if (isGround(State)) {
                return Cursor.getY();
            }
            Cursor.move(Direction.DOWN);
        }
        return Top;
    }

    private static void loadArea(ServerLevel World, int X, int Z, int Reach) {
        for (int ChunkX = (X - Reach) >> 4; ChunkX <= (X + Reach) >> 4; ChunkX++) {
            for (int ChunkZ = (Z - Reach) >> 4; ChunkZ <= (Z + Reach) >> 4; ChunkZ++) {
                World.getChunk(ChunkX, ChunkZ);
            }
        }
    }

    public static Optional<BlockPos> chooseSite(ServerLevel World, Vec3 Around, int MinDistance, int MaxDistance, RandomSource Random) {
        WorldBorder Border = World.getWorldBorder();
        BlockPos Best = null;
        double BestScore = -Double.MAX_VALUE;
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        for (int Attempt = 0; Attempt < SiteAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = Low + Random.nextDouble() * (High - Low);
            int X = Mth.floor(Around.x + Math.cos(Angle) * Distance);
            int Z = Mth.floor(Around.z + Math.sin(Angle) * Distance);
            if (!Border.isWithinBounds(X, Z) || Border.getDistanceToBorder(X, Z) < 32.0) {
                continue;
            }
            loadArea(World, X, Z, 16);
            int Y = groundY(World, X, Z);
            if (Y < World.getMinBuildHeight() + 12 || Y > World.getMaxBuildHeight() - 24) {
                continue;
            }
            double Score = scoreSite(World, X, Y, Z);
            if (Score > BestScore) {
                BestScore = Score;
                Best = new BlockPos(X, Y, Z);
            }
            if (BestScore > -6.0) {
                break;
            }
        }
        return Optional.ofNullable(Best);
    }

    private static double scoreSite(ServerLevel World, int X, int Y, int Z) {
        double Score = 0.0;
        for (int Ring = 0; Ring <= 12; Ring += 4) {
            int Spokes = Ring == 0 ? 1 : 8;
            for (int Spoke = 0; Spoke < Spokes; Spoke++) {
                double Angle = Spoke * Math.PI * 2.0 / Spokes;
                int SampleX = X + (int) Math.round(Math.cos(Angle) * Ring);
                int SampleZ = Z + (int) Math.round(Math.sin(Angle) * Ring);
                int SampleY = groundY(World, SampleX, SampleZ);
                BlockPos Ground = new BlockPos(SampleX, SampleY, SampleZ);
                Score -= Math.min(10, Math.abs(SampleY - Y)) * 0.6;
                if (!World.getFluidState(Ground.above()).isEmpty() || !World.getFluidState(Ground).isEmpty()) {
                    Score -= 12.0;
                }
                if (!isNatural(World.getBlockState(Ground))) {
                    Score -= 8.0;
                }
            }
        }
        for (int ChunkX = (X - 16) >> 4; ChunkX <= (X + 16) >> 4; ChunkX++) {
            for (int ChunkZ = (Z - 16) >> 4; ChunkZ <= (Z + 16) >> 4; ChunkZ++) {
                LevelChunk Loaded = World.getChunk(ChunkX, ChunkZ);
                for (BlockEntity Stored : Loaded.getBlockEntities().values()) {
                    BlockPos At = Stored.getBlockPos();
                    if (Math.abs(At.getX() - X) <= 16 && Math.abs(At.getZ() - Z) <= 16) {
                        Score -= 40.0;
                    }
                }
            }
        }
        return Score;
    }

    public static Direction frontToward(BlockPos Ground, Vec3 Toward, RandomSource Random) {
        double Dx = Toward.x - (Ground.getX() + 0.5);
        double Dz = Toward.z - (Ground.getZ() + 0.5);
        if (Dx * Dx + Dz * Dz < 1.0) {
            return Direction.Plane.HORIZONTAL.getRandomDirection(Random);
        }
        return Direction.getNearest(Dx, 0.0, Dz);
    }

    private static BlockState scorched(RandomSource Random, boolean Inner) {
        float Roll = Random.nextFloat();
        if (Inner) {
            if (Roll < 0.32f) {
                return Blocks.BLACKSTONE.defaultBlockState();
            }
            if (Roll < 0.47f) {
                return Blocks.MAGMA_BLOCK.defaultBlockState();
            }
            if (Roll < 0.64f) {
                return Blocks.COARSE_DIRT.defaultBlockState();
            }
            if (Roll < 0.78f) {
                return Blocks.BASALT.defaultBlockState();
            }
            if (Roll < 0.91f) {
                return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            }
            return Blocks.TUFF.defaultBlockState();
        }
        if (Roll < 0.42f) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (Roll < 0.58f) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (Roll < 0.70f) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (Roll < 0.80f) {
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        if (Roll < 0.87f) {
            return Blocks.ROOTED_DIRT.defaultBlockState();
        }
        if (Roll < 0.93f) {
            return Blocks.TUFF.defaultBlockState();
        }
        return Blocks.MAGMA_BLOCK.defaultBlockState();
    }

    private static BlockState rimStone(RandomSource Random) {
        float Roll = Random.nextFloat();
        if (Roll < 0.38f) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (Roll < 0.56f) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (Roll < 0.70f) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (Roll < 0.84f) {
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        if (Roll < 0.94f) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        return Blocks.COBBLESTONE.defaultBlockState();
    }

    private static int baseLevel(ServerLevel World, int X, int Z) {
        int[] Heights = new int[25];
        int Count = 0;
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                Heights[Count++] = groundY(World, X + Dx, Z + Dz);
            }
        }
        Arrays.sort(Heights);
        return Heights[Count / 2];
    }

    public static Result strike(ServerLevel World, BlockPos Ground, Direction Front, RandomSource Random) {
        GameRules Rules = World.getGameRules();
        boolean Griefing = Rules.getBoolean(GameRules.RULE_MOBGRIEFING);
        boolean Burn = Griefing && Rules.getBoolean(GameRules.RULE_DOFIRETICK);
        int Cx = Ground.getX();
        int Cz = Ground.getZ();
        float Radius = 11.5f + Random.nextFloat() * 2.0f;
        int Reach = (int) Math.ceil(Radius + RimWidth) + EjectaReach;
        loadArea(World, Cx, Cz, Reach + 2);
        int Base = baseLevel(World, Cx, Cz);
        float Depth = 4.5f + Random.nextFloat();
        int Floor0 = Math.round(Base - Depth);
        float RimHeight = 1.8f + Random.nextFloat() * 0.8f;
        double PhaseA = Random.nextDouble() * Math.PI * 2.0;
        double PhaseB = Random.nextDouble() * Math.PI * 2.0;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                if (Distance > Reach) {
                    continue;
                }
                double Theta = Math.atan2(Dz, Dx);
                double Warp = 1.0 + 0.07 * Math.sin(3.0 * Theta + PhaseA) + 0.05 * Math.sin(5.0 * Theta + PhaseB);
                double Local = Distance / Warp;
                int X = Cx + Dx;
                int Z = Cz + Dz;
                int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z) - 1;
                int GroundTop = groundY(World, X, Z);
                if (Local < Radius) {
                    carveColumn(World, Cursor, X, Z, Local, Radius, Floor0, Base, Top, GroundTop, Griefing, Burn, Random);
                } else if (Local < Radius + RimWidth) {
                    raiseRim(World, Cursor, X, Z, (Local - Radius) / RimWidth, RimHeight, GroundTop, Griefing, Random);
                } else {
                    scatter(World, Cursor, X, Z, (Local - Radius - RimWidth) / Math.max(1.0, Reach - Radius - RimWidth), GroundTop, Griefing, Random);
                }
            }
        }
        BlockPos Origin = new BlockPos(Cx, Floor0, Cz).relative(Front);
        Scp002Structure.build(World, Origin, Front, true, Pos -> !isProtected(World, Pos, World.getBlockState(Pos)));
        clearPorch(World, Origin, Front, Random);
        if (Griefing) {
            launchEjecta(World, Origin.relative(Front.getOpposite()), Random);
        }
        return new Result(Scp002Structure.toWorld(Origin, Front, Scp002Structure.DoorLower), Origin, Radius);
    }

    private static void carveColumn(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Z, double Local, float Radius, int Floor0, int Base, int Top, int GroundTop,
                                    boolean Griefing, boolean Burn, RandomSource Random) {
        int Floor = Local <= FlatRadius ? Floor0 : (int) Math.round(Floor0 + (Base - Floor0) * Math.pow((Local - FlatRadius) / (Radius - FlatRadius), 1.7));
        int Ceiling = Math.max(Top, Base + 4);
        for (int Y = Floor + 1; Y <= Ceiling; Y++) {
            Cursor.set(X, Y, Z);
            if (!World.getBlockState(Cursor).isAir() && mayChange(World, Cursor, Griefing)) {
                World.setBlock(Cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (GroundTop < Floor && Floor - GroundTop <= 6) {
            for (int Y = GroundTop + 1; Y <= Floor; Y++) {
                Cursor.set(X, Y, Z);
                if (isOpenCell(World.getBlockState(Cursor)) && mayChange(World, Cursor, Griefing)) {
                    World.setBlock(Cursor, scorched(Random, Local < FlatRadius * 0.8), Block.UPDATE_ALL);
                }
            }
        }
        boolean Inner = Local < FlatRadius * 0.8;
        int Lining = Inner ? 3 : 2;
        for (int Y = Floor - Lining + 1; Y <= Floor; Y++) {
            Cursor.set(X, Y, Z);
            BlockState Here = World.getBlockState(Cursor);
            if (!Here.isAir() && isGround(Here) && mayChange(World, Cursor, Griefing)) {
                World.setBlock(Cursor, scorched(Random, Inner), Block.UPDATE_ALL);
            }
        }
        if (Burn && Local > 2.0 && Local < Radius - 0.6 && Random.nextFloat() < 0.06f) {
            Cursor.set(X, Floor + 1, Z);
            BlockState Below = World.getBlockState(Cursor.below());
            if (World.getBlockState(Cursor).isAir() && !Below.is(Blocks.MAGMA_BLOCK) && Below.isFaceSturdy(World, Cursor.below(), Direction.UP)) {
                World.setBlock(Cursor, BaseFireBlock.getState(World, Cursor), Block.UPDATE_ALL);
            }
        }
    }

    private static void raiseRim(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Z, double Along, float RimHeight, int GroundTop, boolean Griefing, RandomSource Random) {
        int Height = (int) Math.round(RimHeight * Math.pow(1.0 - Along, 1.4) + (Random.nextFloat() - 0.5f) * 0.8f);
        for (int Step = 1; Step <= Height; Step++) {
            Cursor.set(X, GroundTop + Step, Z);
            if (isOpenCell(World.getBlockState(Cursor)) && mayChange(World, Cursor, Griefing)) {
                World.setBlock(Cursor, Step == Height && Random.nextFloat() < 0.55f ? scorched(Random, false) : rimStone(Random), Block.UPDATE_ALL);
            }
        }
        if (Height <= 0 && Random.nextFloat() < 0.75f) {
            scorchTop(World, Cursor, X, GroundTop, Z, Griefing, Random);
        }
    }

    private static void scatter(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Z, double Along, int GroundTop, boolean Griefing, RandomSource Random) {
        double Fade = Math.max(0.0, 1.0 - Along);
        if (Random.nextFloat() < 0.17 * Math.pow(Fade, 1.6)) {
            Cursor.set(X, GroundTop + 1, Z);
            if (isOpenCell(World.getBlockState(Cursor)) && mayChange(World, Cursor, Griefing)) {
                World.setBlock(Cursor, Random.nextFloat() < 0.5f ? rimStone(Random) : scorched(Random, false), Block.UPDATE_ALL);
                return;
            }
        }
        if (Random.nextFloat() < 0.45 * Fade) {
            scorchTop(World, Cursor, X, GroundTop, Z, Griefing, Random);
        }
    }

    private static void scorchTop(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int GroundTop, int Z, boolean Griefing, RandomSource Random) {
        Cursor.set(X, GroundTop, Z);
        BlockState Here = World.getBlockState(Cursor);
        if ((Here.is(Blocks.GRASS_BLOCK) || Here.is(Blocks.PODZOL) || Here.is(Blocks.MYCELIUM) || Here.is(Blocks.DIRT) || Here.is(Blocks.SNOW_BLOCK)) && mayChange(World, Cursor, Griefing)) {
            World.setBlock(Cursor, Random.nextFloat() < 0.8f ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.ROOTED_DIRT.defaultBlockState(), Block.UPDATE_ALL);
            Cursor.set(X, GroundTop + 1, Z);
            BlockState Above = World.getBlockState(Cursor);
            if (!Above.isAir() && Above.canBeReplaced() && Above.getFluidState().isEmpty()) {
                World.setBlock(Cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    public static void clearPorch(ServerLevel World, BlockPos Origin, Direction Front, RandomSource Random) {
        for (int X = -1; X <= 1; X++) {
            for (int Z = PorchNear; Z <= PorchFar; Z++) {
                BlockPos Floor = Scp002Structure.toWorld(Origin, Front, X, 0, Z);
                BlockState Ground = World.getBlockState(Floor);
                if (!isProtected(World, Floor, Ground) && (Ground.isAir() || Ground.canBeReplaced() || Ground.is(Blocks.MAGMA_BLOCK))) {
                    World.setBlock(Floor, Random.nextFloat() < 0.7f ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState(), Block.UPDATE_ALL);
                }
                for (int Y = 1; Y <= 3; Y++) {
                    BlockPos Above = Scp002Structure.toWorld(Origin, Front, X, Y, Z);
                    BlockState State = World.getBlockState(Above);
                    if (!State.isAir() && !isProtected(World, Above, State) && State.getFluidState().isEmpty()) {
                        World.setBlock(Above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static void launchEjecta(ServerLevel World, BlockPos Origin, RandomSource Random) {
        BlockPos Spout = Origin.above(Scp002Structure.MinY + Scp002Structure.height() + 2);
        int Count = 14 + Random.nextInt(8);
        for (int Index = 0; Index < Count; Index++) {
            BlockPos Start = Spout.offset(Random.nextInt(3) - 1, Random.nextInt(2), Random.nextInt(3) - 1);
            if (!World.getBlockState(Start).isAir()) {
                continue;
            }
            FallingBlockEntity Debris = FallingBlockEntity.fall(World, Start, Random.nextFloat() < 0.5f ? scorched(Random, false) : rimStone(Random));
            Debris.dropItem = false;
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Speed = 0.55 + Random.nextDouble() * 0.45;
            Debris.setDeltaMovement(Math.cos(Angle) * Speed, 0.65 + Random.nextDouble() * 0.45, Math.sin(Angle) * Speed);
        }
    }

    public static void blast(ServerLevel World, Entity Source) {
        Vec3 Centre = Source.position();
        AABB Reach = new AABB(Centre, Centre).inflate(BlastRadius);
        for (LivingEntity Caught : World.getEntitiesOfClass(LivingEntity.class, Reach, LivingEntity::isAlive)) {
            Vec3 Away = Caught.position().subtract(Centre);
            double Distance = Away.length();
            if (Distance > BlastRadius) {
                continue;
            }
            float Strength = (float) (1.0 - Distance / BlastRadius);
            Caught.hurt(World.damageSources().explosion(Source, null), BlastDamage * Strength * Strength + 1.0f);
            Vec3 Push = Distance < 0.01 ? new Vec3(0.0, 1.0, 0.0) : Away.normalize();
            Caught.push(Push.x * Strength * 1.4, 0.35 + Strength * 0.6, Push.z * Strength * 1.4);
            Caught.hurtMarked = true;
        }
    }

    public static void announce(ServerLevel World, Result Struck, Vec3 At) {
        ImpactPayload Payload = new ImpactPayload(BlockPos.containing(At), Struck.Radius());
        for (ServerPlayer Viewer : World.players()) {
            if (Viewer.distanceToSqr(At) < AnnounceRange * AnnounceRange) {
                PacketDistributor.sendToPlayer(Viewer, Payload);
            }
        }
    }
}
