package net.rai.scp_unleashed.processing.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.processing.MachineStatus;
import net.rai.scp_unleashed.processing.ProcessingMachineBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum ProcessingJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "processing_machine");
    private static final String DataKey = "ScpProcessing";
    private static final int BarColour = 0xFFE0902C;
    private static final int BarShadeColour = 0xFF7A4612;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof ProcessingMachineBlockEntity Machine)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putByte("Status", (byte) Machine.getStatus().ordinal());
        Info.putFloat("Progress", Machine.getProgressFraction());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        MachineStatus Status = MachineStatus.byIndex(Info.getByte("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        float Progress = Info.getFloat("Progress");
        if (Status != MachineStatus.Idle && Progress > 0.0f) {
            IElementHelper Helper = IElementHelper.get();
            Tooltip.add(Helper.progress(Progress, Component.translatable("jade.scp_unleashed.processing.progress", Math.round(Progress * 100.0f)),
                    Helper.progressStyle().color(BarColour, BarShadeColour).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
        }
    }
}
