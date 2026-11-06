package net.rai.scp_unleashed.computer;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UploadPortBlock extends ComputerDeviceBlock {
    public static final MapCodec<UploadPortBlock> BlockCodec = simpleCodec(UploadPortBlock::new);
    public static final EnumProperty<PortMedia> Media = EnumProperty.create("media", PortMedia.class);
    public static final double[][] Boxes = {{1, 0, 1, 15, 7, 13}, {0, 0, 13, 16, 13, 16}};
    public static final double[][] Supports = {{0, 0, 15, 16, 16, 16}};

    public UploadPortBlock(Properties Properties) {
        super(Properties, Boxes, Supports);
        registerDefaultState(defaultBlockState().setValue(Media, PortMedia.Empty));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Media);
    }

    @Override
    protected String getKeyName() {
        return "network_upload_port";
    }

    @Override
    protected int getDraw() {
        return UploadPortBlockEntity.Draw;
    }

    @Override
    protected List<String> getDetailKeys() {
        return List.of("block.scp_unleashed.network_upload_port.insert", "block.scp_unleashed.network_upload_port.eject",
                "block.scp_unleashed.network_upload_port.drive", "block.scp_unleashed.network_upload_port.wiring");
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof UploadPortBlockEntity Port)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (Stack.is(ArchiveRegistry.SealedDocument.get())) {
            if (!World.isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.network_upload_port.sealed").withStyle(ChatFormatting.GRAY), true);
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!UploadPortBlockEntity.accepts(Stack) || !Port.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            Port.insert(Stack);
            if (!User.getAbilities().instabuild) {
                Stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof UploadPortBlockEntity Port)) {
            return InteractionResult.PASS;
        }
        if (Port.isEmpty()) {
            if (!World.isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.network_upload_port.empty").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!World.isClientSide) {
            ItemStack Taken = Port.eject();
            if (!User.addItem(Taken)) {
                User.drop(Taken, false);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World.getBlockEntity(Pos) instanceof UploadPortBlockEntity Port) {
            ItemStack Dropped = Port.takeForDrop();
            if (!Dropped.isEmpty()) {
                Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Dropped);
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return State.getValue(Media) == PortMedia.Empty ? 0 : 15;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new UploadPortBlockEntity(Pos, State);
    }
}
