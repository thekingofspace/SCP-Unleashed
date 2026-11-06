package net.rai.scp_unleashed.computer;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.mining.ServerAmbience;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChatServerBlock extends ComputerDeviceBlock {
    public static final MapCodec<ChatServerBlock> BlockCodec = simpleCodec(ChatServerBlock::new);
    public static final double[][] Boxes = {{0, 0, 0, 16, 16, 16}};

    public ChatServerBlock(Properties Properties) {
        super(Properties, Boxes, Boxes);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected String getKeyName() {
        return "chat_server";
    }

    @Override
    protected int getDraw() {
        return ChatServerBlockEntity.Draw;
    }

    @Override
    protected List<String> getDetailKeys() {
        return List.of("block.scp_unleashed.chat_server.network", "block.scp_unleashed.chat_server.buffer", "block.scp_unleashed.computer_device.wiring");
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World instanceof ServerLevel Server && World.getBlockEntity(Pos) instanceof ChatServerBlockEntity Chat) {
            if (!Chat.isRunning()) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.computer_device.offline").withStyle(ChatFormatting.GRAY), true);
            } else {
                ComputerGrid.Reach Reach = ComputerGrid.reach(Server, Pos);
                User.displayClientMessage(Component.translatable("message.scp_unleashed.chat_server.status", Reach.runningComputers(), Reach.runningDatabases().size() + 1)
                        .withStyle(ChatFormatting.GREEN), true);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Powered)) {
            ServerAmbience.idle(World, Pos, Random, 0.5f);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new ChatServerBlockEntity(Pos, State);
    }
}
