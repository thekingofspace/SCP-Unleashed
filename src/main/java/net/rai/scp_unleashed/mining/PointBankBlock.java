package net.rai.scp_unleashed.mining;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.computer.ComputerDeviceBlock;
import net.rai.scp_unleashed.mining.shop.ShopService;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PointBankBlock extends ComputerDeviceBlock {
    public static final MapCodec<PointBankBlock> BlockCodec = simpleCodec(PointBankBlock::new);
    public static final double[][] Boxes = {{0, 0, 0, 16, 16, 16}};

    public PointBankBlock(Properties Properties) {
        super(Properties, Boxes, Boxes);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected String getKeyName() {
        return "point_bank";
    }

    @Override
    protected int getDraw() {
        return PointBankBlockEntity.Draw;
    }

    @Override
    protected List<String> getDetailKeys() {
        return List.of("block.scp_unleashed.point_bank.collects", "block.scp_unleashed.point_bank.pool", "block.scp_unleashed.point_bank.keeps",
                "block.scp_unleashed.computer_device.wiring");
    }

    @Override
    protected void appendStored(ItemStack Stack, List<Component> Lines) {
        Long Stored = Stack.get(MiningRegistry.BalanceComponent.get());
        if (Stored != null && Stored > 0L) {
            Lines.add(Component.translatable("block.scp_unleashed.point_bank.stored", ShopService.points(Stored)).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World instanceof ServerLevel Server && World.getBlockEntity(Pos) instanceof PointBankBlockEntity Bank) {
            if (!Bank.isRunning()) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.point_bank.offline", ShopService.points(Bank.getBalance()))
                        .withStyle(ChatFormatting.GRAY), true);
            } else {
                List<PointBankBlockEntity> Pool = MiningGrid.runningBanks(Server, Pos);
                long Total = Math.max(Bank.getBalance(), MiningGrid.total(Pool));
                User.displayClientMessage(Component.translatable("message.scp_unleashed.point_bank.status", ShopService.points(Bank.getBalance()),
                        ShopService.points(Total), Math.max(1, Pool.size())).withStyle(ChatFormatting.AQUA), true);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Powered)) {
            ServerAmbience.idle(World, Pos, Random, 0.35f);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new PointBankBlockEntity(Pos, State);
    }
}
