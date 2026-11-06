package net.rai.scp_unleashed.computer;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.computer.network.ComputerNetworking;
import net.rai.scp_unleashed.mining.ServerAmbience;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MessageDatabaseBlock extends ComputerDeviceBlock {
    public static final MapCodec<MessageDatabaseBlock> BlockCodec = simpleCodec(MessageDatabaseBlock::new);
    public static final double[][] Boxes = {{0, 0, 0, 16, 16, 16}};

    public MessageDatabaseBlock(Properties Properties) {
        super(Properties, Boxes, Boxes);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected String getKeyName() {
        return "message_database";
    }

    @Override
    protected int getDraw() {
        return MessageDatabaseBlockEntity.Draw;
    }

    @Override
    protected List<String> getDetailKeys() {
        return List.of("block.scp_unleashed.message_database.channel", "block.scp_unleashed.message_database.history",
                "block.scp_unleashed.message_database.keeps", "block.scp_unleashed.computer_device.wiring");
    }

    @Override
    protected void appendStored(ItemStack Stack, List<Component> Lines) {
        CustomData Stored = Stack.get(ComputerRegistry.StorageComponent.get());
        if (Stored != null) {
            Lines.add(Component.translatable("block.scp_unleashed.message_database.stored", MessageDatabaseBlockEntity.storedName(Stored),
                    MessageDatabaseBlockEntity.storedCount(Stored), MessageDatabaseBlockEntity.Capacity).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (User instanceof ServerPlayer Viewer && World.getBlockEntity(Pos) instanceof MessageDatabaseBlockEntity Database) {
            ComputerNetworking.openDatabase(Viewer, Database);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Powered)) {
            ServerAmbience.idle(World, Pos, Random, 0.4f);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new MessageDatabaseBlockEntity(Pos, State);
    }
}
