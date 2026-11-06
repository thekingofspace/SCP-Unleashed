package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp4666PresentBlock extends BaseEntityBlock {
    public static final MapCodec<Scp4666PresentBlock> BlockCodec = simpleCodec(Scp4666PresentBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape Shape = Block.box(1.0, 0.0, 1.0, 15.0, 11.0, 15.0);

    public Scp4666PresentBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    public static void place(ServerLevel World, BlockPos Pos, ServerPlayer Owner) {
        Direction Facing = Direction.Plane.HORIZONTAL.getRandomDirection(World.getRandom());
        World.setBlock(Pos, Scp4666Registry.Present.get().defaultBlockState().setValue(Scp4666PresentBlock.Facing, Facing), Block.UPDATE_ALL);
        if (World.getBlockEntity(Pos) instanceof Scp4666OwnedBlockEntity Tag) {
            Tag.setOwner(Owner.getUUID(), Owner.getGameProfile().getName());
        }
    }

    public static List<ItemStack> contents() {
        return List.of(new ItemStack(Scp4666Registry.Doll.get()), new ItemStack(Scp4666Registry.Necklace.get()), new ItemStack(Scp4666Registry.Cane.get()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (World.getBlockEntity(Pos) instanceof Scp4666OwnedBlockEntity Tag && !Tag.owns(User.getUUID())) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.present_other", Tag.name()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        World.playSound(null, Pos, Scp4666Registry.PresentOpen.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        for (ItemStack Gift : contents()) {
            if (!User.getInventory().add(Gift)) {
                User.drop(Gift, false);
            }
        }
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.present_open").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp4666OwnedBlockEntity(Pos, State);
    }
}
