package net.rai.scp_unleashed.mining.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.mining.MinerRules;
import net.rai.scp_unleashed.mining.MinerStatus;
import net.rai.scp_unleashed.mining.MiningGrid;
import net.rai.scp_unleashed.mining.PointBankBlockEntity;
import net.rai.scp_unleashed.mining.PointMinerBlock;
import net.rai.scp_unleashed.mining.PointMinerBlockEntity;
import net.rai.scp_unleashed.mining.shop.ShopService;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;
import java.util.Locale;

public enum MiningJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Miner("point_miner"),
    Bank("point_bank");

    private static final String DataKey = "Mining";
    private static final String Prefix = "jade.scp_unleashed.mining.";

    private final ResourceLocation Uid;

    MiningJadeProvider(String Path) {
        Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        if (this == Miner && Accessor.getBlockEntity() instanceof PointMinerBlockEntity Miner) {
            MinerRules.Engine Core = Miner.getCore();
            Info.putInt("Index", Miner.getStackIndex());
            Info.putInt("Stack", Miner.getStackSize());
            Info.putInt("Share", (int) Math.round(Core.getShare() * 100.0));
            Info.putBoolean("Mining", Core.isMining());
            Info.putInt("Water", Miner.getTank().getFluidAmount());
            Info.putInt("Heat", Core.getHeat());
            Info.putBoolean("Overheated", Core.isOverheated());
            Info.putInt("Buffer", Core.getBuffer());
            Info.putBoolean("Linked", Miner.isLinked());
        } else if (this == Bank && Accessor.getBlockEntity() instanceof PointBankBlockEntity Bank) {
            Info.putLong("Balance", Bank.getBalance());
            if (Bank.isRunning()) {
                List<PointBankBlockEntity> Pool = MiningGrid.runningBanks(World, Bank.getBlockPos());
                Info.putLong("Pool", Math.max(Bank.getBalance(), MiningGrid.total(Pool)));
                Info.putInt("Banks", Math.max(1, Pool.size()));
            }
        } else {
            return;
        }
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        if (this == Bank) {
            Tooltip.add(Component.translatable(Prefix + "balance", ShopService.points(Info.getLong("Balance"))).withStyle(ChatFormatting.AQUA));
            if (Info.getInt("Banks") > 1) {
                Tooltip.add(Component.translatable(Prefix + "pool", ShopService.points(Info.getLong("Pool")), Info.getInt("Banks")).withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        MinerStatus Status = Accessor.getBlockState().hasProperty(PointMinerBlock.Status) ? Accessor.getBlockState().getValue(PointMinerBlock.Status) : MinerStatus.Off;
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        Tooltip.add(Component.translatable(Prefix + "stack", Info.getInt("Index"), Info.getInt("Stack")).withStyle(ChatFormatting.GRAY));
        int Share = Info.getInt("Share");
        if (Share <= 0) {
            Tooltip.add(Component.translatable(Prefix + "unpowered").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            Tooltip.add(Component.translatable(Share >= 100 ? Prefix + "powered" : Prefix + "underpowered", Share).withStyle(Share >= 100 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        }
        int Water = Info.getInt("Water");
        Tooltip.add(Component.translatable(Prefix + "coolant", Water, MinerRules.TankCapacity).withStyle(Water >= MinerRules.WaterPerSecond ? ChatFormatting.AQUA : ChatFormatting.RED));
        int Heat = Info.getInt("Heat");
        if (Info.getBoolean("Overheated")) {
            Tooltip.add(Component.translatable(Prefix + "overheated", Heat).withStyle(ChatFormatting.RED));
        } else if (Heat > 0) {
            Tooltip.add(Component.translatable(Prefix + "heat", Heat).withStyle(Heat >= 50 ? ChatFormatting.RED : ChatFormatting.GOLD));
        }
        double Rate = Info.getBoolean("Mining") ? MinerRules.pointsPerMinute(Share / 100.0) : 0.0;
        Tooltip.add(Component.translatable(Prefix + "rate", String.format(Locale.ROOT, "%.1f", Rate)).withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Prefix + "buffer", Info.getInt("Buffer"), MinerRules.BufferSize).withStyle(ChatFormatting.GRAY));
        if (!Info.getBoolean("Linked")) {
            Tooltip.add(Component.translatable(Prefix + "no_bank").withStyle(ChatFormatting.GOLD));
        }
    }
}
