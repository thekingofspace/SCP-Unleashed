package net.rai.scp_unleashed.scp015;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class Scp015PipeBlock extends Block {
    public static final MapCodec<Scp015PipeBlock> BlockCodec = simpleCodec(Scp015PipeBlock::new);
    public static final Map<Direction, BooleanProperty> Connections = PipeBlock.PROPERTY_BY_DIRECTION;
    public static final int ArmInset = 4;
    public static final int CoreInset = 2;
    public static final int DripChance = 18;
    public static final int GroanChance = 90;
    public static final int GroanGap = 160;
    public static final int DripGap = 14;
    private static final VoxelShape[] ShapeByMask = buildShapes();
    private static long LastGroan;
    private static long LastDrip;

    public Scp015PipeBlock(Properties Properties) {
        super(Properties);
        BlockState Default = stateDefinition.any();
        for (BooleanProperty Connection : Connections.values()) {
            Default = Default.setValue(Connection, false);
        }
        registerDefaultState(Default);
    }

    public static boolean isPipe(BlockState State) {
        return State.getBlock() instanceof Scp015PipeBlock;
    }

    public static BooleanProperty connection(Direction Side) {
        return Connections.get(Side);
    }

    public static int connectionMask(BlockState State) {
        int Mask = 0;
        for (Map.Entry<Direction, BooleanProperty> Entry : Connections.entrySet()) {
            if (State.getValue(Entry.getValue())) {
                Mask |= 1 << Entry.getKey().get3DDataValue();
            }
        }
        return Mask;
    }

    public static boolean isAxial(int Mask) {
        if (Mask == 0) {
            return false;
        }
        for (Direction.Axis Axis : Direction.Axis.values()) {
            int AxisBits = (1 << Direction.get(Direction.AxisDirection.POSITIVE, Axis).get3DDataValue())
                    | (1 << Direction.get(Direction.AxisDirection.NEGATIVE, Axis).get3DDataValue());
            if ((Mask & ~AxisBits) == 0) {
                return true;
            }
        }
        return false;
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] Built = new VoxelShape[64];
        for (int Mask = 0; Mask < Built.length; Mask++) {
            Built[Mask] = shapeFor(Mask);
        }
        return Built;
    }

    private static VoxelShape shapeFor(int Mask) {
        if (isAxial(Mask)) {
            return run(Direction.from3DDataValue(Integer.numberOfTrailingZeros(Mask)).getAxis());
        }
        VoxelShape Shape = Block.box(CoreInset, CoreInset, CoreInset, 16 - CoreInset, 16 - CoreInset, 16 - CoreInset);
        for (Direction Side : Direction.values()) {
            if ((Mask & (1 << Side.get3DDataValue())) != 0) {
                Shape = Shapes.or(Shape, arm(Side));
            }
        }
        return Shape.optimize();
    }

    private static VoxelShape run(Direction.Axis Axis) {
        int High = 16 - ArmInset;
        return switch (Axis) {
            case X -> Block.box(0, ArmInset, ArmInset, 16, High, High);
            case Y -> Block.box(ArmInset, 0, ArmInset, High, 16, High);
            case Z -> Block.box(ArmInset, ArmInset, 0, High, High, 16);
        };
    }

    private static VoxelShape arm(Direction Side) {
        int High = 16 - ArmInset;
        return switch (Side) {
            case DOWN -> Block.box(ArmInset, 0, ArmInset, High, CoreInset, High);
            case UP -> Block.box(ArmInset, 16 - CoreInset, ArmInset, High, 16, High);
            case NORTH -> Block.box(ArmInset, ArmInset, 0, High, High, CoreInset);
            case SOUTH -> Block.box(ArmInset, ArmInset, 16 - CoreInset, High, High, 16);
            case WEST -> Block.box(0, ArmInset, ArmInset, CoreInset, High, High);
            case EAST -> Block.box(16 - CoreInset, ArmInset, ArmInset, 16, High, High);
        };
    }

    private static double dripHeight(int Mask) {
        if (!isAxial(Mask)) {
            return CoreInset / 16.0;
        }
        return (Mask & (1 << Direction.UP.get3DDataValue())) != 0 ? 0.0 : ArmInset / 16.0;
    }

    public BlockState grownState(Direction TowardParent) {
        return defaultBlockState().setValue(connection(TowardParent), true);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Connections.values().toArray(new BooleanProperty[0]));
    }

    public static boolean keepsLink(BlockState State, Direction Side, BlockState NeighborState) {
        return State.getValue(connection(Side)) && isPipe(NeighborState) && NeighborState.getValue(connection(Side.getOpposite()));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (State.getValue(connection(Side)) && !keepsLink(State, Side, NeighborState)) {
            return State.setValue(connection(Side), false);
        }
        return State;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeByMask[connectionMask(State)];
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (World instanceof ServerLevel ServerWorld) {
            Scp015Growth.get(ServerWorld).seed(ServerWorld, Pos);
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!isPipe(OldState) && World instanceof ServerLevel ServerWorld) {
            Scp015Growth.get(ServerWorld).segmentAdded();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!isPipe(NewState) && World instanceof ServerLevel ServerWorld) {
            Scp015Growth.get(ServerWorld).segmentRemoved();
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block NeighborBlock, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, NeighborBlock, NeighborPos, MovedByPiston);
        if (World instanceof ServerLevel ServerWorld && !Scp015Growth.isWorking() && Scp015Growth.isFree(World.getBlockState(NeighborPos))) {
            Scp015Growth.get(ServerWorld).wake(ServerWorld, Pos);
        }
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        int Mask = connectionMask(State);
        if (Mask == 0) {
            return;
        }
        long Now = World.getGameTime();
        if (Random.nextInt(DripChance) == 0 && World.getBlockState(Pos.below()).isAir()) {
            double Low = dripHeight(Mask);
            double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.4;
            double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.4;
            World.addParticle(ParticleTypes.DRIPPING_WATER, X, Pos.getY() + Low - 0.02, Z, 0.0, 0.0, 0.0);
            if (Now - LastDrip >= DripGap || Now < LastDrip) {
                LastDrip = Now;
                World.playLocalSound(X, Pos.getY() + Low, Z, Scp015Registry.Drip.get(), SoundSource.BLOCKS, 0.8f + Random.nextFloat() * 0.2f,
                        0.85f + Random.nextFloat() * 0.3f, false);
            }
        }
        if (Random.nextInt(GroanChance) == 0 && (Now - LastGroan >= GroanGap || Now < LastGroan)) {
            LastGroan = Now;
            float Pitch = 0.8f + Random.nextFloat() * 0.3f;
            World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Scp015Registry.Groan.get(), SoundSource.BLOCKS, 0.7f, Pitch, false);
            World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Scp015Registry.GroanCreak.get(), SoundSource.BLOCKS, 0.5f, Pitch, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp015_pipe.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp015_pipe.grows").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp015_pipe.breaks").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp015_pipe.mined").withStyle(ChatFormatting.GOLD));
        }
    }
}
