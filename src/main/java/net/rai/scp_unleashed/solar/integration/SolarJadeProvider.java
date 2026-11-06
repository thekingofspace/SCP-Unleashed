package net.rai.scp_unleashed.solar.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.solar.SolarBlockEntity;
import net.rai.scp_unleashed.wire.WireHostBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SolarJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "solar_panel");
    private static final String DataKey = "ScpSolar";
    private static final float Whole = 0.995f;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        BlockPos Pos = Accessor.getPosition();
        if (Accessor.getBlock() instanceof WireHostBlock Host) {
            Pos = Host.devicePos(Accessor.getBlockState(), Pos);
        }
        if (!Accessor.getLevel().isLoaded(Pos) || !(Accessor.getLevel().getBlockEntity(Pos) instanceof SolarBlockEntity Collector)) {
            return;
        }
        CompoundTag Solar = new CompoundTag();
        Solar.putBoolean("Day", Collector.isDay());
        Solar.putFloat("Efficiency", Collector.getEfficiency());
        Solar.putFloat("Rate", Collector.getRate());
        Solar.putInt("Peak", Collector.getTier().getPeakOutput());
        Solar.putFloat("Weather", Collector.getWeather());
        Solar.putFloat("Exposure", Collector.getExposure());
        Data.put(DataKey, Solar);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Solar = Data.getCompound(DataKey);
        float Exposure = Solar.getFloat("Exposure");
        float Weather = Solar.getFloat("Weather");
        Tooltip.add(Component.translatable("jade.scp_unleashed.solar.output", String.format("%.1f", Solar.getFloat("Rate")), Solar.getInt("Peak")));
        if (!Solar.getBoolean("Day")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.solar.night").withStyle(ChatFormatting.GRAY));
        } else {
            int Percent = Math.round(Solar.getFloat("Efficiency") * 100.0f);
            ChatFormatting Colour = Percent >= 70 ? ChatFormatting.GREEN : Percent >= 35 ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
            Tooltip.add(Component.translatable("jade.scp_unleashed.solar.efficiency", Percent).withStyle(Colour));
        }
        if (Exposure <= 0.0f) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.solar.blocked").withStyle(ChatFormatting.RED));
        } else if (Exposure < Whole) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.solar.shaded", Math.round(Exposure * 100.0f)).withStyle(ChatFormatting.GOLD));
        }
        if (Weather < Whole) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.solar.weather", Math.round(Weather * 100.0f)).withStyle(ChatFormatting.AQUA));
        }
    }
}
