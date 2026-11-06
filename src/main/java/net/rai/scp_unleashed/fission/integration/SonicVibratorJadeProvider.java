package net.rai.scp_unleashed.fission.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.SonicVibratorBlockEntity;
import net.rai.scp_unleashed.fission.VibratorStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum SonicVibratorJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "sonic_vibrator");
    private static final String DataKey = "SonicVibrator";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof SonicVibratorBlockEntity Machine)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Machine.getStatus().getSerializedName());
        Info.putInt("Input", Machine.getInputTank().getFluidAmount());
        Info.putInt("Output", Machine.getOutputTank().getFluidAmount());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        VibratorStatus Status = VibratorStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        Tooltip.add(Component.translatable("jade.scp_unleashed.sonic_vibrator.input", Info.getInt("Input"), SonicVibratorBlockEntity.TankCapacity).withStyle(ChatFormatting.GREEN));
        Tooltip.add(Component.translatable("jade.scp_unleashed.sonic_vibrator.output", Info.getInt("Output"), SonicVibratorBlockEntity.TankCapacity).withStyle(ChatFormatting.AQUA));
    }
}
