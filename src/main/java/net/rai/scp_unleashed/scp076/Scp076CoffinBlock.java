package net.rai.scp_unleashed.scp076;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class Scp076CoffinBlock extends Scp076SealedBlock implements EntityBlock {
    public static final MapCodec<Scp076CoffinBlock> CoffinCodec = simpleCodec(Scp076CoffinBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Segment = IntegerProperty.create("segment", 0, 2);
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final int Middle = 1;
    public static final double[][] ClosedBoxes = {{1, 0, -13, 15, 15, 29}};
    public static final double[][] OpenBoxes = {{1, 0, -13, 15, 4, 29}, {2, 4, -12, 3, 12, 28}, {13, 4, -12, 14, 12, 28}, {3, 4, -12, 13, 12, -10.5}, {3, 4, 26.5, 13, 12, 28}};
    private static final Map<String, VoxelShape> Shapes0 = new HashMap<>();
    private static boolean Clearing;

    public Scp076CoffinBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.SOUTH).setValue(Segment, Middle).setValue(Open, false));
    }

    @Override
    protected MapCodec<? extends Scp076CoffinBlock> codec() {
        return CoffinCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Segment, Open);
    }

    public static BlockPos partPos(BlockPos MiddlePos, Direction Axis, int Index) {
        return MiddlePos.relative(Axis, Index - Middle);
    }

    public static BlockPos middle(BlockPos Pos, BlockState State) {
        return Pos.relative(State.getValue(Facing), Middle - State.getValue(Segment));
    }

    private static VoxelShape shapeFor(Direction Axis, int Index, boolean Lifted) {
        String Key = Axis.getName() + Index + Lifted;
        VoxelShape Found = Shapes0.get(Key);
        if (Found != null) {
            return Found;
        }
        VoxelShape Result = Shapes.empty();
        Direction Across = Axis.getCounterClockWise();
        double Shift = (Index - Middle) * 16.0;
        for (double[] Box : Lifted ? OpenBoxes : ClosedBoxes) {
            double MinZ = Math.max(0.0, Box[2] - Shift);
            double MaxZ = Math.min(16.0, Box[5] - Shift);
            if (MaxZ <= MinZ) {
                continue;
            }
            double[] X = turn(Box[0], MinZ, Across, Axis);
            double[] Y = turn(Box[3], MaxZ, Across, Axis);
            Result = Shapes.or(Result, Block.box(Math.min(X[0], Y[0]), Box[1], Math.min(X[1], Y[1]), Math.max(X[0], Y[0]), Box[4], Math.max(X[1], Y[1])));
        }
        Shapes0.put(Key, Result);
        return Result;
    }

    private static double[] turn(double X, double Z, Direction Across, Direction Axis) {
        double Dx = X - 8.0;
        double Dz = Z - 8.0;
        return new double[]{8.0 + Dx * Across.getStepX() + Dz * Axis.getStepX(), 8.0 + Dx * Across.getStepZ() + Dz * Axis.getStepZ()};
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Axis = Context.getHorizontalDirection();
        Level World = Context.getLevel();
        BlockPos MiddlePos = Context.getClickedPos();
        for (int Index = 0; Index < 3; Index++) {
            BlockPos Part = partPos(MiddlePos, Axis, Index);
            if (World.isOutsideBuildHeight(Part) || !World.getBlockState(Part).canBeReplaced(Context)) {
                return null;
            }
        }
        return defaultBlockState().setValue(Facing, Axis);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        if (!World.isClientSide) {
            place(World, middle(Pos, State), State.getValue(Facing));
        }
    }

    public static void place(Level World, BlockPos MiddlePos, Direction Axis) {
        Block Coffin = Scp076Registry.Coffin.get();
        for (int Index = 0; Index < 3; Index++) {
            World.setBlock(partPos(MiddlePos, Axis, Index), Coffin.defaultBlockState().setValue(Facing, Axis).setValue(Segment, Index), Block.UPDATE_ALL);
        }
    }

    public static void setOpen(Level World, BlockPos MiddlePos, Direction Axis, boolean Value) {
        for (int Index = 0; Index < 3; Index++) {
            BlockPos Part = partPos(MiddlePos, Axis, Index);
            BlockState State = World.getBlockState(Part);
            if (State.getBlock() instanceof Scp076CoffinBlock && State.getValue(Open) != Value) {
                World.setBlock(Part, State.setValue(Open, Value), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replacement, boolean Moved) {
        if (!State.is(Replacement.getBlock()) && !World.isClientSide && !Clearing) {
            Clearing = true;
            try {
                BlockPos MiddlePos = middle(Pos, State);
                Direction Axis = State.getValue(Facing);
                for (int Index = 0; Index < 3; Index++) {
                    BlockPos Part = partPos(MiddlePos, Axis, Index);
                    BlockState There = World.getBlockState(Part);
                    if (!Part.equals(Pos) && There.getBlock() instanceof Scp076CoffinBlock && middle(Part, There).equals(MiddlePos)) {
                        World.setBlock(Part, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            } finally {
                Clearing = false;
            }
        }
        super.onRemove(State, World, Pos, Replacement, Moved);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return shapeFor(State.getValue(Facing), State.getValue(Segment), false);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return shapeFor(State.getValue(Facing), State.getValue(Segment), State.getValue(Open));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && !State.getValue(Open)) {
            User.displayClientMessage(Component.translatable("block.scp_unleashed.scp076_coffin.heavy").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Segment) == Middle ? new Scp076CoffinBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide || Type != Scp076Registry.CoffinEntity.get()) {
            return null;
        }
        return (Level Where, BlockPos Pos, BlockState Current, T Ticking) -> {
            if (Ticking instanceof Scp076CoffinBlockEntity Coffin) {
                Coffin.serverTick();
            }
        };
    }
}
