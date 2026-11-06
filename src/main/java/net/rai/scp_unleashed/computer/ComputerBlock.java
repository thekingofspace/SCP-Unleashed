package net.rai.scp_unleashed.computer;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.computer.net.ComputerSessions;
import net.rai.scp_unleashed.computer.network.ComputerNetworking;
import net.rai.scp_unleashed.speaker.cassette.CassetteItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ComputerBlock extends ComputerDeviceBlock {
    public static final MapCodec<ComputerBlock> BlockCodec = simpleCodec(ComputerBlock::new);
    public static final double[][] Boxes = {{0, 0, 3, 5, 13, 15}, {6, 0, 4, 16, 12, 16}, {2, 0, 0, 14, 1.5, 3}};
    public static final double[][] Supports = {{0, 0, 0, 1, 16, 16}, {15, 0, 0, 16, 16, 16}};

    public ComputerBlock(Properties Properties) {
        super(Properties, Boxes, Supports);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected String getKeyName() {
        return "computer";
    }

    @Override
    protected int getDraw() {
        return ComputerBlockEntity.Draw;
    }

    @Override
    protected List<String> getDetailKeys() {
        return List.of("block.scp_unleashed.computer.apps", "block.scp_unleashed.computer.storage", "block.scp_unleashed.computer.keeps",
                "block.scp_unleashed.computer.wiring");
    }

    @Override
    protected void appendStored(ItemStack Stack, List<Component> Lines) {
        CustomData Stored = Stack.get(ComputerRegistry.StorageComponent.get());
        if (Stored != null) {
            Lines.add(Component.translatable("block.scp_unleashed.computer.stored", ComputerBlockEntity.storedCount(Stored, "Files"),
                    ComputerBlockEntity.storedCount(Stored, "Buttons") + ComputerBlockEntity.storedCount(Stored, "Sounds"),
                    ComputerBlockEntity.storedCount(Stored, "Actions")).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (CassetteItem.canInsert(Stack)) {
            return CassetteItem.insertInto(World, User, Hand);
        }
        return super.useItemOn(Stack, State, World, Pos, User, Hand, Hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (User instanceof ServerPlayer Viewer && World.getBlockEntity(Pos) instanceof ComputerBlockEntity Computer) {
            if (Computer.isRunning()) {
                ComputerNetworking.openComputer(Viewer, Computer);
            } else {
                Viewer.displayClientMessage(Component.translatable("message.scp_unleashed.computer.dark").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel Server) {
            if (Server.getBlockEntity(Pos) instanceof ComputerBlockEntity Computer) {
                Computer.releaseOutputs(Server);
            }
            ComputerSessions.closeAll(Server, Pos, ComputerSessions.CloseReason.Removed);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new ComputerBlockEntity(Pos, State);
    }
}
