package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public abstract class Scp407GrowthBlock extends Block {
    public static final IntegerProperty Wither = IntegerProperty.create("wither", 0, 2);
    public static final IntegerProperty Variant = IntegerProperty.create("variant", 0, Scp407Carpets.Kinds - 1);
    public static final BooleanProperty Carpet = BooleanProperty.create("carpet");

    protected Scp407GrowthBlock(Properties Settings) {
        super(Settings);
    }

    public static boolean alive(BlockState State) {
        return !State.hasProperty(Wither) || State.getValue(Wither) == 0;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return Type != PathComputationType.WATER && State.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
    }

    public static boolean isGrowth(BlockState State) {
        return State.getBlock() instanceof Scp407GrowthBlock;
    }

    @Nullable
    public Direction base(BlockState State) {
        return Direction.DOWN;
    }

    public boolean soft(BlockState State) {
        return false;
    }

    public void carpetFaces(BlockState State, List<Direction> Out) {
    }

    public boolean skirted(BlockState State) {
        return true;
    }

    public void skirts(BlockGetter World, BlockPos Pos, BlockState State, List<Direction> Out) {
        if (!skirted(State)) {
            return;
        }
        for (Direction Side : Direction.values()) {
            if (Scp407Faces.holds(World, Pos, Side)) {
                Out.add(Side);
            }
        }
    }

    public BlockState dress(LevelReader World, BlockPos Pos, BlockState State) {
        BlockState Out = State;
        if (Out.hasProperty(Variant)) {
            Out = Out.setValue(Variant, Scp407Carpets.variantAt(Pos));
        }
        if (Out.hasProperty(Carpet)) {
            Direction Base = base(Out);
            Out = Out.setValue(Carpet, Base != null && Scp407Faces.holds(World, Pos, Base));
        }
        return Out;
    }

    protected BlockState refreshCarpet(BlockState State, Direction Side, LevelAccessor World, BlockPos Pos) {
        if (State.hasProperty(Carpet) && alive(State) && Side == base(State)) {
            return State.setValue(Carpet, Scp407Faces.holds(World, Pos, Side));
        }
        return State;
    }

    protected static Map<Direction, VoxelShape> facing(double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        Map<Direction, VoxelShape> Shapes = new EnumMap<>(Direction.class);
        Shapes.put(Direction.DOWN, box(X0, Y0, Z0, X1, Y1, Z1));
        Shapes.put(Direction.UP, box(X0, 16.0 - Y1, Z0, X1, 16.0 - Y0, Z1));
        Shapes.put(Direction.NORTH, box(X0, Z0, Y0, X1, Z1, Y1));
        Shapes.put(Direction.SOUTH, box(X0, Z0, 16.0 - Y1, X1, Z1, 16.0 - Y0));
        Shapes.put(Direction.WEST, box(Y0, Z0, X0, Y1, Z1, X1));
        Shapes.put(Direction.EAST, box(16.0 - Y1, Z0, X0, 16.0 - Y0, Z1, X1));
        return Shapes;
    }

    protected static boolean supported(LevelReader World, BlockPos Pos, Direction Toward) {
        BlockPos Support = Pos.relative(Toward);
        BlockState Holder = World.getBlockState(Support);
        return Holder.getBlock() instanceof Scp407GrowthBlock || Holder.isFaceSturdy(World, Support, Toward.getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Wither);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState State) {
        return true;
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (!Scp407Sessions.get(World).owns(Pos)) {
            crumble(World, Pos, State, Blocks.AIR.defaultBlockState());
        }
    }

    public static void crumble(ServerLevel World, BlockPos Pos, BlockState State, BlockState Back) {
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.05);
        World.sendParticles(Scp407Registry.Ash.get(), Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
        World.setBlock(Pos, Back, Block.UPDATE_ALL);
    }

    protected float sporeChance(BlockState State) {
        return 0.0f;
    }

    protected float moteChance(BlockState State) {
        return 0.0f;
    }

    protected float glintChance(BlockState State) {
        return 0.0f;
    }

    protected float puffChance(BlockState State) {
        return 0.0f;
    }

    private static void emit(Level World, ParticleOptions Kind, BlockPos Pos, RandomSource Random, double Lift, double SpeedY) {
        World.addParticle(Kind, Pos.getX() + 0.1 + Random.nextDouble() * 0.8, Pos.getY() + Lift + Random.nextDouble() * 0.7, Pos.getZ() + 0.1 + Random.nextDouble() * 0.8,
                (Random.nextDouble() - 0.5) * 0.01, SpeedY, (Random.nextDouble() - 0.5) * 0.01);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!alive(State)) {
            if (Random.nextFloat() < 0.15f) {
                World.addParticle(Scp407Registry.Ash.get(), Pos.getX() + Random.nextDouble(), Pos.getY() + Random.nextDouble(), Pos.getZ() + Random.nextDouble(), 0.0, -0.01, 0.0);
            }
            return;
        }
        if (Random.nextFloat() < sporeChance(State) && Scp407Ambience.allow(World, Pos, 1)) {
            emit(World, Scp407Registry.Spore.get(), Pos, Random, 0.2, 0.006 + Random.nextDouble() * 0.01);
        }
        if (Random.nextFloat() < moteChance(State) && Scp407Ambience.allow(World, Pos, 1)) {
            emit(World, Scp407Registry.Mote.get(), Pos, Random, 0.3, 0.0);
        }
        if (Random.nextFloat() < glintChance(State) && Scp407Ambience.allow(World, Pos, 1)) {
            emit(World, Scp407Registry.Glint.get(), Pos, Random, 0.2, 0.01 + Random.nextDouble() * 0.008);
        }
        if (Random.nextFloat() < puffChance(State) && Scp407Ambience.allow(World, Pos, Scp407Ambience.PuffCount)) {
            for (int Index = 0; Index < Scp407Ambience.PuffCount; Index++) {
                World.addParticle(Scp407Registry.Spore.get(), Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.4, Pos.getY() + 0.45 + Random.nextDouble() * 0.2,
                        Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.4, (Random.nextDouble() - 0.5) * 0.05, 0.02 + Random.nextDouble() * 0.03, (Random.nextDouble() - 0.5) * 0.05);
            }
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader World, BlockPos Pos, BlockState State) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }
}
