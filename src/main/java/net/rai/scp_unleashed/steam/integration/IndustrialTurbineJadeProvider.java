package net.rai.scp_unleashed.steam.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlock;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlockEntity;
import net.rai.scp_unleashed.steam.TurbineStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum IndustrialTurbineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "industrial_turbine");
    private static final String DataKey = "ScpIndustrialTurbine";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        IndustrialTurbineBlockEntity Machine = IndustrialTurbineBlock.machineAt(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Machine.getStatus().getSerializedName());
        Info.putInt("Generated", Machine.getLastGenerated());
        Info.putInt("Speed", Machine.getSpeedPercent());
        Info.putIntArray("Pressure", new int[]{Machine.getPressurePercent(0), Machine.getPressurePercent(1), Machine.getPressurePercent(2)});
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
        int[] Pressure = Info.getIntArray("Pressure");
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        Tooltip.add(Component.translatable("jade.scp_unleashed.steam_turbine.output", Info.getInt("Generated"), IndustrialTurbineBlockEntity.MaxOutput).withStyle(ChatFormatting.GOLD));
        Tooltip.add(Component.translatable("jade.scp_unleashed.industrial_turbine.speed", Info.getInt("Speed")).withStyle(ChatFormatting.WHITE));
        if (Pressure.length == IndustrialTurbineBlockEntity.InletCount) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.industrial_turbine.inlets", Pressure[0], Pressure[1], Pressure[2]).withStyle(ChatFormatting.WHITE));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.steam_turbine.exhaust", Info.getInt("Exhaust"), IndustrialTurbineBlockEntity.ExhaustCapacity).withStyle(ChatFormatting.GRAY));
    }
}
