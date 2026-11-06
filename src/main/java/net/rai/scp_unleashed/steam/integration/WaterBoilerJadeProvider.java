package net.rai.scp_unleashed.steam.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.steam.BoilerStatus;
import net.rai.scp_unleashed.steam.WaterBoilerBlock;
import net.rai.scp_unleashed.steam.WaterBoilerBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum WaterBoilerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "water_boiler");
    private static final String DataKey = "ScpWaterBoiler";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        WaterBoilerBlockEntity Machine = WaterBoilerBlock.machineAt(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Machine.getStatus().getSerializedName());
        Info.putInt("Heat", Math.round(Machine.getHeat() * 100.0f / WaterBoilerBlockEntity.MaxHeat));
        Info.putInt("Water", Machine.getWaterTank().getFluidAmount());
        Info.putInt("Steam", Machine.getSteamTank().getAmount());
        Info.putInt("Boiled", Machine.getLastBoiled());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        BoilerStatus Status = BoilerStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        if (Info.getInt("Boiled") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.water_boiler.rate", Info.getInt("Boiled") * 20, Info.getInt("Heat")).withStyle(ChatFormatting.GRAY));
        } else if (Info.getInt("Heat") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.water_boiler.heat", Info.getInt("Heat")).withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.water_boiler.water", Info.getInt("Water"), WaterBoilerBlockEntity.WaterCapacity).withStyle(ChatFormatting.AQUA));
        Tooltip.add(Component.translatable("jade.scp_unleashed.water_boiler.steam", Info.getInt("Steam"), WaterBoilerBlockEntity.SteamCapacity).withStyle(ChatFormatting.WHITE));
    }
}
