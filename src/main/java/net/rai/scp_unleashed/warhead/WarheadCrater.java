package net.rai.scp_unleashed.warhead;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WarheadCrater {
    public static final float Radius = 32.0f;
    public static final float Depth = 18.0f;
    public static final float Dome = 30.0f;
    public static final float RimWidth = 7.0f;
    public static final float RimHeight = 3.6f;
    public static final int Reach = 56;
    public static final int SurfaceSlack = 8;
    public static final int GroundSlack = 9;
    public static final int Budget = 2500;
    public static final long TickNanos = 6_000_000L;
    public static final int WaitLimit = 100;
    public static final int EjectaCount = 64;
    public static final int TicketRadius = 5;
    private static int[][] SharedOrder;

    private final BlockPos Base;
    private final long Seed;
    private final boolean Burn;
    private final double PhaseA;
    private final double PhaseB;
    private int Progress;
    private boolean Launched;
    private boolean Ticketed;
    private int Waiting;

    public WarheadCrater(BlockPos Base, long Seed, boolean Burn) {
        this.Base = Base.immutable();
        this.Seed = Seed;
        this.Burn = Burn;
        RandomSource Random = RandomSource.create(Seed);
        this.PhaseA = Random.nextDouble() * Math.PI * 2.0;
        this.PhaseB = Random.nextDouble() * Math.PI * 2.0;
    }

    private static int[][] order() {
        if (SharedOrder == null) {
            List<int[]> Columns = new ArrayList<>();
            for (int Dx = -Reach; Dx <= Reach; Dx++) {
                for (int Dz = -Reach; Dz <= Reach; Dz++) {
                    if (Dx * Dx + Dz * Dz <= Reach * Reach) {
                        Columns.add(new int[]{Dx, Dz});
                    }
                }
            }
            Columns.sort(Comparator.comparingInt(Column -> Column[0] * Column[0] + Column[1] * Column[1]));
            SharedOrder = Columns.toArray(new int[0][]);
        }
        return SharedOrder;
    }

    public BlockPos getBase() {
        return Base;
    }

    public int getProgress() {
        return Progress;
    }

    public static int columns() {
        return order().length;
    }

    public boolean step(ServerLevel World) {
        return step(World, System.nanoTime() + TickNanos);
    }

    public boolean step(ServerLevel World, long Deadline) {
        int[][] Order = order();
        if (!Ticketed || World.getGameTime() % 20L == 0L) {
            Ticketed = true;
            ChunkPos Home = new ChunkPos(Base);
            World.getChunkSource().addRegionTicket(WarheadNetwork.Ticket, Home, TicketRadius, Home);
        }
        RandomSource Random = RandomSource.create(Seed ^ (Progress * 341873128712L));
        int Changes = 0;
        int Columns = 0;
        while (Progress < Order.length && Changes < Budget && (Columns == 0 || System.nanoTime() < Deadline)) {
            int[] Column = Order[Progress];
            if (!ready(World, Column) && Waiting++ < WaitLimit) {
                break;
            }
            Waiting = 0;
            Progress++;
            Columns++;
            Changes += column(World, Column[0], Column[1], Random);
        }
        if (Progress >= Order.length && !Launched) {
            launch(World, Random);
            Launched = true;
        }
        return Progress >= Order.length && Launched;
    }

    private boolean ready(ServerLevel World, int[] Column) {
        return World.getChunkSource().getChunkNow((Base.getX() + Column[0]) >> 4, (Base.getZ() + Column[1]) >> 4) != null;
    }

    public static boolean isProtected(ServerLevel World, BlockPos Pos, BlockState State) {
        return State.getDestroySpeed(World, Pos) < 0.0f || State.getBlock().getExplosionResistance() >= 1200.0f;
    }

    private static boolean isSolid(BlockState State) {
        return !State.isAir() && State.getFluidState().isEmpty() && !State.canBeReplaced() && !State.is(BlockTags.LEAVES);
    }

    private static boolean isGround(BlockState State) {
        return isSolid(State) && !State.is(BlockTags.LOGS) && !State.is(Blocks.BAMBOO) && !State.is(Blocks.CACTUS) && !State.is(Blocks.SUGAR_CANE)
                && !State.is(Blocks.MUSHROOM_STEM) && !State.is(Blocks.RED_MUSHROOM_BLOCK) && !State.is(Blocks.BROWN_MUSHROOM_BLOCK);
    }

    private static int groundY(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Top, int Z) {
        int Floor = World.getMinBuildHeight();
        Cursor.set(X, Top, Z);
        for (int Step = 0; Step < 40 && Cursor.getY() > Floor; Step++) {
            if (isGround(World.getBlockState(Cursor))) {
                return Cursor.getY();
            }
            Cursor.move(Direction.DOWN);
        }
        return Top;
    }

    private static BlockState scorched(RandomSource Random, boolean Inner) {
        float Roll = Random.nextFloat();
        if (Inner) {
            if (Roll < 0.30f) {
                return Blocks.BLACKSTONE.defaultBlockState();
            }
            if (Roll < 0.44f) {
                return Blocks.MAGMA_BLOCK.defaultBlockState();
            }
            if (Roll < 0.60f) {
                return Blocks.BASALT.defaultBlockState();
            }
            if (Roll < 0.76f) {
                return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            }
            if (Roll < 0.90f) {
                return Blocks.COARSE_DIRT.defaultBlockState();
            }
            return Blocks.TUFF.defaultBlockState();
        }
        if (Roll < 0.40f) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (Roll < 0.56f) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (Roll < 0.68f) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (Roll < 0.80f) {
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        if (Roll < 0.90f) {
            return Blocks.TUFF.defaultBlockState();
        }
        return Blocks.MAGMA_BLOCK.defaultBlockState();
    }

    private static BlockState rimStone(RandomSource Random) {
        float Roll = Random.nextFloat();
        if (Roll < 0.34f) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (Roll < 0.52f) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (Roll < 0.68f) {
            return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }
        if (Roll < 0.82f) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (Roll < 0.93f) {
            return Blocks.TUFF.defaultBlockState();
        }
        return Blocks.COBBLESTONE.defaultBlockState();
    }

    private int column(ServerLevel World, int Dx, int Dz, RandomSource Random) {
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        double Theta = Math.atan2(Dz, Dx);
        double Warp = 1.0 + 0.06 * Math.sin(3.0 * Theta + PhaseA) + 0.04 * Math.sin(5.0 * Theta + PhaseB);
        double Local = Distance / Warp;
        int X = Base.getX() + Dx;
        int Z = Base.getZ() + Dz;
        World.getChunk(X >> 4, Z >> 4);
        int Surface = World.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z) - 1;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        if (Local < Radius) {
            return carve(World, Cursor, X, Z, Local, Surface, Random);
        }
        if (Surface > Base.getY() + (int) Dome + SurfaceSlack) {
            return 0;
        }
        int Ground = groundY(World, Cursor, X, Surface, Z);
        if (Math.abs(Ground - (Base.getY() - 1)) > GroundSlack) {
            return 0;
        }
        if (Local < Radius + RimWidth) {
            return rim(World, Cursor, X, Z, (Local - Radius) / RimWidth, Ground, Surface, Random);
        }
        return scorch(World, Cursor, X, Z, (Local - Radius - RimWidth) / (Reach - Radius - RimWidth), Ground, Random);
    }

    private boolean change(ServerLevel World, BlockPos Pos, BlockState Next) {
        BlockState State = World.getBlockState(Pos);
        if (State == Next || isProtected(World, Pos, State)) {
            return false;
        }
        World.setBlock(Pos, Next, Block.UPDATE_ALL);
        return true;
    }

    private int carve(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Z, double Local, int Surface, RandomSource Random) {
        double Fraction = Math.sqrt(Math.max(0.0, 1.0 - (Local / Radius) * (Local / Radius)));
        int Floor = Math.max(World.getMinBuildHeight() + 1, Base.getY() - (int) Math.round(Depth * Fraction));
        int Ceiling = Base.getY() + (int) Math.round(Dome * Fraction);
        boolean Exposed = Surface <= Ceiling + SurfaceSlack;
        if (Exposed) {
            Ceiling = Math.max(Ceiling, Surface);
        }
        Ceiling = Math.min(Ceiling, World.getMaxBuildHeight() - 1);
        int Changes = 0;
        BlockState Air = Blocks.AIR.defaultBlockState();
        for (int Y = Floor + 1; Y <= Ceiling; Y++) {
            Cursor.set(X, Y, Z);
            if (!World.getBlockState(Cursor).isAir() && change(World, Cursor, Air)) {
                Changes++;
            }
        }
        boolean Inner = Local < Radius * 0.55;
        int Lining = Inner ? 3 : 2;
        for (int Y = Floor - Lining + 1; Y <= Floor; Y++) {
            Cursor.set(X, Y, Z);
            if (isSolid(World.getBlockState(Cursor)) && change(World, Cursor, scorched(Random, Inner))) {
                Changes++;
            }
        }
        if (!Exposed) {
            Cursor.set(X, Ceiling + 1, Z);
            if (isSolid(World.getBlockState(Cursor)) && change(World, Cursor, scorched(Random, false))) {
                Changes++;
            }
        }
        if (Burn && Local > 2.0 && Local < Radius - 1.0 && Random.nextFloat() < 0.05f) {
            Changes += ignite(World, Cursor, X, Floor + 1, Z);
        }
        return Changes;
    }

    private int ignite(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Y, int Z) {
        Cursor.set(X, Y, Z);
        BlockPos Below = Cursor.below();
        BlockState Under = World.getBlockState(Below);
        if (World.getBlockState(Cursor).isAir() && !Under.is(Blocks.MAGMA_BLOCK) && Under.isFaceSturdy(World, Below, Direction.UP)) {
            World.setBlock(Cursor, BaseFireBlock.getState(World, Cursor), Block.UPDATE_ALL);
            return 1;
        }
        return 0;
    }

    private int clearAbove(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Ground, int Surface, int Z) {
        int Changes = 0;
        for (int Y = Ground + 1; Y <= Surface; Y++) {
            Cursor.set(X, Y, Z);
            BlockState State = World.getBlockState(Cursor);
            if (State.isAir()) {
                continue;
            }
            boolean Plant = State.is(BlockTags.LEAVES) || State.is(BlockTags.LOGS) || State.is(BlockTags.REPLACEABLE) || State.is(BlockTags.FLOWERS)
                    || State.is(BlockTags.SAPLINGS) || State.canBeReplaced();
            if (Plant && State.getFluidState().isEmpty() && change(World, Cursor, Blocks.AIR.defaultBlockState())) {
                Changes++;
            }
        }
        return Changes;
    }

    private int rim(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Z, double Along, int Ground, int Surface, RandomSource Random) {
        int Changes = clearAbove(World, Cursor, X, Ground, Surface, Z);
        int Height = (int) Math.round(RimHeight * Math.pow(1.0 - Along, 1.4) + (Random.nextFloat() - 0.5f) * 0.8f);
        for (int Step = 1; Step <= Height; Step++) {
            Cursor.set(X, Ground + Step, Z);
            if (World.getBlockState(Cursor).isAir() && change(World, Cursor, Step == Height && Random.nextFloat() < 0.5f ? scorched(Random, false) : rimStone(Random))) {
                Changes++;
            }
        }
        if (Height <= 0) {
            Cursor.set(X, Ground, Z);
            if (change(World, Cursor, scorched(Random, false))) {
                Changes++;
            }
        }
        if (Burn && Random.nextFloat() < 0.08f) {
            Changes += ignite(World, Cursor, X, Ground + Math.max(Height, 0) + 1, Z);
        }
        return Changes;
    }

    private int scorch(ServerLevel World, BlockPos.MutableBlockPos Cursor, int X, int Z, double Along, int Ground, RandomSource Random) {
        double Fade = Math.max(0.0, 1.0 - Along);
        int Changes = 0;
        Cursor.set(X, Ground, Z);
        BlockState Top = World.getBlockState(Cursor);
        boolean Soft = Top.is(BlockTags.DIRT) || Top.is(BlockTags.SAND) || Top.is(Blocks.SNOW_BLOCK) || Top.is(Blocks.GRAVEL) || Top.is(Blocks.CLAY);
        if (Soft && Random.nextFloat() < 0.75 * Fade) {
            if (change(World, Cursor, Random.nextFloat() < 0.7f ? Blocks.COARSE_DIRT.defaultBlockState() : Random.nextFloat() < 0.5f ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState())) {
                Changes++;
            }
            Cursor.set(X, Ground + 1, Z);
            BlockState Above = World.getBlockState(Cursor);
            if (!Above.isAir() && Above.canBeReplaced() && Above.getFluidState().isEmpty() && change(World, Cursor, Blocks.AIR.defaultBlockState())) {
                Changes++;
            }
        }
        if (Random.nextFloat() < 0.14 * Fade * Fade) {
            Cursor.set(X, Ground + 1, Z);
            if (World.getBlockState(Cursor).isAir() && change(World, Cursor, Random.nextFloat() < 0.5f ? rimStone(Random) : scorched(Random, false))) {
                Changes++;
            }
        } else if (Burn && Random.nextFloat() < 0.04 * Fade) {
            Changes += ignite(World, Cursor, X, Ground + 1, Z);
        }
        return Changes;
    }

    private void launch(ServerLevel World, RandomSource Random) {
        BlockPos Spout = Base.below((int) Depth - 2);
        for (int Index = 0; Index < EjectaCount; Index++) {
            BlockPos Start = Spout.offset(Random.nextInt(5) - 2, Random.nextInt(3), Random.nextInt(5) - 2);
            if (!World.getBlockState(Start).isAir()) {
                continue;
            }
            FallingBlockEntity Debris = FallingBlockEntity.fall(World, Start, Random.nextFloat() < 0.5f ? scorched(Random, false) : rimStone(Random));
            Debris.dropItem = false;
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Speed = 0.7 + Random.nextDouble() * 0.8;
            Debris.setDeltaMovement(Math.cos(Angle) * Speed, 0.95 + Random.nextDouble() * 0.6, Math.sin(Angle) * Speed);
            Debris.hurtMarked = true;
        }
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putLong("Base", Base.asLong());
        Saved.putLong("Seed", Seed);
        Saved.putBoolean("Burn", Burn);
        Saved.putInt("Progress", Progress);
        Saved.putBoolean("Launched", Launched);
        return Saved;
    }

    public static WarheadCrater load(CompoundTag Saved) {
        WarheadCrater Crater = new WarheadCrater(BlockPos.of(Saved.getLong("Base")), Saved.getLong("Seed"), Saved.getBoolean("Burn"));
        Crater.Progress = Saved.getInt("Progress");
        Crater.Launched = Saved.getBoolean("Launched");
        return Crater;
    }
}
