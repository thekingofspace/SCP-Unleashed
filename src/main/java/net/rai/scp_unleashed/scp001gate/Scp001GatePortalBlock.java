package net.rai.scp_unleashed.scp001gate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Scp001GatePortalBlock extends Block implements EntityBlock {
    public static final MapCodec<Scp001GatePortalBlock> PortalCodec = simpleCodec(Scp001GatePortalBlock::new);
    public static final EnumProperty<Direction.Axis> Axis = BlockStateProperties.HORIZONTAL_AXIS;

    public Scp001GatePortalBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Axis, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Scp001GatePortalBlock> codec() {
        return PortalCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Axis);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Axis, Context.getHorizontalDirection().getClockWise().getAxis());
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Body) {
        if (World instanceof ServerLevel Server && Body instanceof ServerPlayer User && !User.isPassenger()) {
            Scp001GateGarden.passThrough(User, Server, Pos);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader World, BlockPos Pos, BlockState State) {
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp001GatePortalBlockEntity(Pos, State);
    }
}
