package net.rai.scp_unleashed.scp5001.event.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventData;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class Scp5001DeviceBlock extends Block {
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;

    protected Scp5001DeviceBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
    }

    protected abstract Map<Direction, VoxelShape> shapes();

    @Nullable
    public Scp5001EventData.Kind kind() {
        return null;
    }

    protected boolean facesClickedWall() {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Clicked = Context.getClickedFace();
        if (facesClickedWall() && Clicked.getAxis().isHorizontal()) {
            return defaultBlockState().setValue(Facing, Clicked);
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return shapes().get(State.getValue(Facing));
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Next, boolean Moved) {
        if (!State.is(Next.getBlock()) && kind() != null && World instanceof ServerLevel Server && Server == Server.getServer().overworld()) {
            Scp5001EventData.get(Server.getServer()).forget(kind(), Pos);
        }
        super.onRemove(State, World, Pos, Next, Moved);
    }
}
