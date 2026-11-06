package net.rai.scp_unleashed.scp902;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.PropWater;

import java.util.List;
import java.util.Map;

public class Scp902PropBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final Vec3 Snag = new Vec3(0.55, 0.7, 0.55);
    private final Scp902Prop Kind;
    private final Map<Direction, VoxelShape> Outlines;
    private final Map<Direction, VoxelShape> Collisions;

    public Scp902PropBlock(Scp902Prop Kind, Properties Settings) {
        super(Settings);
        this.Kind = Kind;
        this.Outlines = PropShapes.horizontal(Kind.shape());
        this.Collisions = PropShapes.horizontal(Kind.collision());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PropWater.Waterlogged, false));
    }

    public Scp902Prop kind() {
        return Kind;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(Settings -> new Scp902PropBlock(Kind, Settings));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, PropWater.Waterlogged);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return PropWater.placed(defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite()), Context);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Collisions.get(State.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return Kind.snags() ? false : super.isPathfindable(State, Type);
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Visitor) {
        if (Kind.snags()) {
            Visitor.makeStuckInBlock(State, Snag);
        }
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        PropWater.update(State, World, Pos);
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return PropWater.fluid(State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed." + Kind.id() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
