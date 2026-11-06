package net.rai.scp_unleashed.scp939.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import net.rai.scp_unleashed.scp939.compressor.CompressorStatus;
import net.rai.scp_unleashed.scp939.compressor.GasCompressorBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum GasCompressorJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = Scp939Registry.location(Scp939Registry.CompressorId);
    private static final String Prefix = "jade.scp_unleashed.gas_compressor.";
    private static final String DataKey = "Compressor";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (Accessor.getBlockEntity() instanceof GasCompressorBlockEntity Compressor) {
            CompoundTag Status = new CompoundTag();
            Status.putInt("Status", Compressor.status().ordinal());
            Status.putInt("Received", Compressor.lastReceived());
            Status.putFloat("Progress", Compressor.progress());
            Data.put(DataKey, Status);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Status = Accessor.getServerData().getCompound(DataKey);
        CompressorStatus Shown = CompressorStatus.byIndex(Status.getInt("Status"));
        Tooltip.add(Component.translatable(Shown.getTranslationKey()).withStyle(Shown.getColour()));
        if (Shown == CompressorStatus.Running || Shown == CompressorStatus.Underpowered) {
            Tooltip.add(Component.translatable(Prefix + "progress", Math.round(Status.getFloat("Progress") * 100.0f)).withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable(Prefix + "demand", GasCompressorBlockEntity.WorkDraw).withStyle(ChatFormatting.GRAY));
    }
}
