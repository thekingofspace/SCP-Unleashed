package net.rai.scp_unleashed.labware.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.AssayRecord;
import net.rai.scp_unleashed.labware.LabAnalyzerBlockEntity;
import net.rai.scp_unleashed.labware.LabAnalyzerStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum LabAnalyzerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "lab_analyzer");
    private static final String DataKey = "LabAnalyzer";
    private static final String Prefix = "jade.scp_unleashed.lab_analyzer.";
    private static final int CultureColour = 0xFF3CC48C;
    private static final int CultureShadeColour = 0xFF14604A;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof LabAnalyzerBlockEntity Machine)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putByte("Status", (byte) Machine.getStatus().ordinal());
        Info.putFloat("Progress", Machine.getProgressFraction());
        AssayRecord Result = Machine.getLastResult();
        Info.putInt("Found", Result == null ? -1 : Result.Findings().size());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        LabAnalyzerStatus Status = LabAnalyzerStatus.byIndex(Info.getByte("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        float Progress = Info.getFloat("Progress");
        if (Status.isRunning() && Progress > 0.0f) {
            IElementHelper Helper = IElementHelper.get();
            Tooltip.add(Helper.progress(Progress, Component.translatable(Prefix + "progress", Math.round(Progress * 100.0f)),
                    Helper.progressStyle().color(CultureColour, CultureShadeColour).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
        }
        int Found = Info.getInt("Found");
        if (Found == 0) {
            Tooltip.add(Component.translatable(Prefix + "clean").withStyle(ChatFormatting.GREEN));
        } else if (Found > 0) {
            Tooltip.add(Component.translatable(Prefix + "positive", Found).withStyle(ChatFormatting.RED));
        }
    }
}
