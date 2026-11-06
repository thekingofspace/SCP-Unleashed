package net.rai.scp_unleashed.cooler.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.cooler.CoolerBlock;
import net.rai.scp_unleashed.cooler.CoolerBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerClimate;
import net.rai.scp_unleashed.cooler.CoolerRegistry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CoolerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = CoolerRegistry.location(CoolerRegistry.CoolerId);
    private static final String Prefix = "jade.scp_unleashed." + CoolerRegistry.CoolerId + ".";
    private static final String DataKey = "Cooler";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (Accessor.getBlockEntity() instanceof CoolerBlockEntity Cooler) {
            CompoundTag Status = new CompoundTag();
            Status.putInt("Received", Cooler.getLastReceived());
            Data.put(DataKey, Status);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        boolean Active = Accessor.getBlockState().getValue(CoolerBlock.Powered);
        int Received = Accessor.getServerData().getCompound(DataKey).getInt("Received");
        if (Active) {
            Tooltip.add(Component.translatable(Prefix + "powered").withStyle(ChatFormatting.AQUA));
        } else if (Received > 0) {
            Tooltip.add(Component.translatable(Prefix + "underpowered").withStyle(ChatFormatting.GOLD));
        } else {
            Tooltip.add(Component.translatable(Prefix + "unpowered").withStyle(ChatFormatting.RED));
        }
        CoolerClimate Climate = CoolerClimate.at(Accessor.getLevel(), Accessor.getPosition());
        int Reach = (int) CoolerBlockEntity.effectiveRadius(Climate);
        Tooltip.add(Component.translatable(Prefix + "radius", Reach).withStyle(Active ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        if (Climate == CoolerClimate.Hot) {
            Tooltip.add(Component.translatable(Prefix + "heat").withStyle(ChatFormatting.GOLD));
        }
        if (Active) {
            Tooltip.add(Component.translatable(Prefix + "freezing").withStyle(ChatFormatting.DARK_AQUA));
        }
        Tooltip.add(Component.translatable(Prefix + "demand", CoolerBlockEntity.PowerDraw).withStyle(ChatFormatting.GRAY));
    }
}
