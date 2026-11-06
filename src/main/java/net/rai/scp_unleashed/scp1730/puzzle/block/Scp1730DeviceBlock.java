package net.rai.scp_unleashed.scp1730.puzzle.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp1730.puzzle.Scp1730PuzzleDirector;

import java.util.EnumMap;
import java.util.Map;

public abstract class Scp1730DeviceBlock extends HorizontalDirectionalBlock {
    private static final Map<Direction, VoxelShape> Panels = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> Bodies = new EnumMap<>(Direction.class);

    static {
        Panels.put(Direction.NORTH, Block.box(2, 1, 12, 14, 15, 16));
        Panels.put(Direction.SOUTH, Block.box(2, 1, 0, 14, 15, 4));
        Panels.put(Direction.WEST, Block.box(12, 1, 2, 16, 15, 14));
        Panels.put(Direction.EAST, Block.box(0, 1, 2, 4, 15, 14));
        Bodies.put(Direction.NORTH, Block.box(1, 0, 8, 15, 16, 16));
        Bodies.put(Direction.SOUTH, Block.box(1, 0, 0, 15, 16, 8));
        Bodies.put(Direction.WEST, Block.box(8, 0, 1, 16, 16, 15));
        Bodies.put(Direction.EAST, Block.box(0, 0, 1, 8, 16, 15));
    }

    protected Scp1730DeviceBlock(Properties Props) {
        super(Props);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Face = Context.getClickedFace().getAxis().isHorizontal() ? Context.getClickedFace() : Context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, Face);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Bodies.get(State.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Panels.get(State.getValue(FACING));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (World.isClientSide || Stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return Scp1730PuzzleDirector.use((ServerLevel) World, Pos, State, (ServerPlayer) User, Stack) ? ItemInteractionResult.CONSUME
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Scp1730PuzzleDirector.use((ServerLevel) World, Pos, State, (ServerPlayer) User, ItemStack.EMPTY);
        return InteractionResult.CONSUME;
    }
}
