package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

import java.util.List;
import java.util.Map;

public class Scp4666PuzzleBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp4666PuzzleBlock> BlockCodec = simpleCodec(Scp4666PuzzleBlock::new);
    public static final int Done = 6;
    public static final IntegerProperty Stage = IntegerProperty.create("stage", 0, Done);
    private static final Map<Direction, VoxelShape> Shapes = Scp4666Shapes.table(new double[][]{{0, 13, 0, 16, 16, 16}, {1, 0, 1, 3, 13, 3}, {13, 0, 1, 15, 13, 3}, {1, 0, 13, 3, 13, 15},
            {13, 0, 13, 15, 13, 15}});

    public Scp4666PuzzleBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Stage, 2));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Stage);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int Now = State.getValue(Stage);
        if (User.isSecondaryUseActive()) {
            World.setBlock(Pos, State.setValue(Stage, 0), Block.UPDATE_ALL);
            World.playSound(null, Pos, Scp4666Registry.PuzzlePiece.get(), SoundSource.BLOCKS, 0.8f, 0.7f);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.puzzle_scatter").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        if (Now >= Done) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.puzzle_done").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        World.setBlock(Pos, State.setValue(Stage, Now + 1), Block.UPDATE_ALL);
        World.playSound(null, Pos, Scp4666Registry.PuzzlePiece.get(), SoundSource.BLOCKS, 0.7f, 1.0f + Now * 0.06f);
        if (Now + 1 >= Done) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.puzzle_finished").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_puzzle_table.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
