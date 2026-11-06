package net.rai.scp_unleashed.steam.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.steam.SteamTurbineBlock;
import net.rai.scp_unleashed.steam.SteamTurbineBlockEntity;
import net.rai.scp_unleashed.steam.TurbineStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SteamTurbineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "steam_turbine");
    private static final String DataKey = "ScpSteamTurbine";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        SteamTurbineBlockEntity Machine = SteamTurbineBlock.machineAt(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Machine.getStatus().getSerializedName());
        Info.putInt("Generated", Machine.getLastGenerated());
        Info.putInt("Pressure", Machine.getPressurePercent());
        Info.putInt("Exhaust", Machine.getExhaustTank().getAmount());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        TurbineStatus Status = TurbineStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        Tooltip.add(Component.translatable("jade.scp_unleashed.steam_turbine.output", Info.getInt("Generated"), SteamTurbineBlockEntity.MaxOutput).withStyle(ChatFormatting.GOLD));
        Tooltip.add(Component.translatable("jade.scp_unleashed.steam_turbine.pressure", Info.getInt("Pressure")).withStyle(ChatFormatting.WHITE));
        Tooltip.add(Component.translatable("jade.scp_unleashed.steam_turbine.exhaust", Info.getInt("Exhaust"), SteamTurbineBlockEntity.ExhaustCapacity).withStyle(ChatFormatting.GRAY));
    }
}
