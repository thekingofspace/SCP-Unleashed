package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Map;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp4666BoxBlock extends BaseEntityBlock {
    public static final MapCodec<Scp4666BoxBlock> BlockCodec = simpleCodec(Scp4666BoxBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    private static final VoxelShape Shape = Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0);

    public Scp4666BoxBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.SOUTH).setValue(Open, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    public static void deliver(ServerLevel World, Scp4666Map Layout, int Food, long Seed) {
        World.setBlock(Layout.BoxSpot, Scp4666Registry.Box.get().defaultBlockState().setValue(Facing, Direction.SOUTH), Block.UPDATE_ALL);
        if (World.getBlockEntity(Layout.BoxSpot) instanceof Scp4666BoxBlockEntity Box) {
            Box.fill(Food > 0, Seed);
        }
        World.playSound(null, Layout.BoxSpot, Scp4666Registry.BoxSet.get(), SoundSource.BLOCKS, 0.8f, 0.9f);
    }

    public static void clear(ServerLevel World, Scp4666Map Layout) {
        if (World.getBlockState(Layout.BoxSpot).is(Scp4666Registry.Box.get())) {
            World.setBlock(Layout.BoxSpot, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(World.getBlockEntity(Pos) instanceof Scp4666BoxBlockEntity Box)) {
            return InteractionResult.PASS;
        }
        List<ItemStack> Share = Box.take(User);
        if (Share.isEmpty()) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.box_taken").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        for (ItemStack Stack : Share) {
            if (!User.getInventory().add(Stack)) {
                User.drop(Stack, false);
            }
        }
        if (!State.getValue(Open)) {
            World.setBlock(Pos, State.setValue(Open, true), Block.UPDATE_ALL);
        }
        World.playSound(null, Pos, Scp4666Registry.BoxSet.get(), SoundSource.BLOCKS, 0.6f, 1.3f);
        User.displayClientMessage(Component.translatable(Share.size() > 1 ? "message.scp_unleashed.scp4666.box_open" : "message.scp_unleashed.scp4666.box_open_cocoa")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp4666BoxBlockEntity(Pos, State);
    }
}
