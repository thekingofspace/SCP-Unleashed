package net.rai.scp_unleashed.scpsign;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ScpSmallSignBlock extends ScpSignBlock {
    public static final MapCodec<ScpSmallSignBlock> BlockCodec = simpleCodec(ScpSmallSignBlock::new);
    private static final Map<Direction, VoxelShape> Outlines = PropShapes.horizontal(PropShapes.box(0.5, 3, 13.5, 15.5, 13, 16));

    public ScpSmallSignBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Lit, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    public ScpSignKind getKind() {
        return ScpSignKind.Small;
    }

    @Override
    public BlockPos mainPos(BlockState State, BlockPos Pos) {
        return Pos;
    }

    @Override
    public List<BlockPos> parts(BlockState State, BlockPos Pos) {
        return List.of(Pos);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Clicked = Context.getClickedPos();
        for (Direction Facing : facingChoices(Context)) {
            if (onWall(World, Clicked, Facing)) {
                return defaultBlockState().setValue(FACING, Facing).setValue(Lit, World.hasNeighborSignal(Clicked));
            }
        }
        refuse(Context, "no_wall");
        return null;
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (!World.isClientSide) {
            refreshPower(World, State, Pos);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new ScpSignBlockEntity(Pos, State);
    }
}
