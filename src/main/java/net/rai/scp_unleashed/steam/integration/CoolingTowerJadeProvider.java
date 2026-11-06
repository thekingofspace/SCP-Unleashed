package net.rai.scp_unleashed.steam.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.steam.CoolingTowerBlock;
import net.rai.scp_unleashed.steam.CoolingTowerBlockEntity;
import net.rai.scp_unleashed.steam.TowerStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CoolingTowerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "cooling_tower");
    private static final String DataKey = "ScpCoolingTower";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        CoolingTowerBlockEntity Machine = CoolingTowerBlock.machineAt(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Machine.getStatus().getSerializedName());
        Info.putInt("Steam", Machine.getSteamTank().getAmount());
        Info.putInt("Water", Machine.getWaterTank().getFluidAmount());
        Info.putInt("Return", Machine.getReturnPercent());
        Info.putInt("Condensed", Machine.getLastCondensed());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        TowerStatus Status = TowerStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        if (Info.getInt("Condensed") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.cooling_tower.rate", Info.getInt("Condensed") * 20).withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.cooling_tower.return", Info.getInt("Return")).withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable("jade.scp_unleashed.cooling_tower.steam", Info.getInt("Steam"), CoolingTowerBlockEntity.SteamCapacity).withStyle(ChatFormatting.WHITE));
        Tooltip.add(Component.translatable("jade.scp_unleashed.cooling_tower.water", Info.getInt("Water"), CoolingTowerBlockEntity.WaterCapacity).withStyle(ChatFormatting.AQUA));
    }
}
