package net.rai.scp_unleashed.matrix.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.matrix.InstabilityMatrixBlock;
import net.rai.scp_unleashed.matrix.InstabilityMatrixBlockEntity;
import net.rai.scp_unleashed.matrix.MatrixStatus;
import net.rai.scp_unleashed.matrix.MatrixTuning;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum MatrixJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "instability_matrix");
    private static final String DataKey = "InstabilityMatrix";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        InstabilityMatrixBlockEntity Matrix = InstabilityMatrixBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Matrix == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Matrix.getStatus().getSerializedName());
        Info.putInt("Tier", Matrix.getTier().getTier());
        Info.putFloat("Flow", Matrix.getFlow());
        Info.putFloat("Resonance", Matrix.getResonance());
        Info.putInt("Nearby", Matrix.getNearby());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        MatrixStatus Status = MatrixStatus.byName(Info.getString("Status"));
        FragmentRarity Tier = FragmentRarity.byTier(Info.getInt("Tier"));
        Tooltip.add(Component.translatable("jade.scp_unleashed.instability_matrix.status", Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour())));
        Component Name = Component.translatable(InstabilityMatrixBlock.tierKey(Tier)).withStyle(Style.EMPTY.withColor(Tier.getColour() & 0xFFFFFF));
        Tooltip.add(Component.translatable("jade.scp_unleashed.instability_matrix.tier", Name, MatrixTuning.grouped(MatrixTuning.drawFor(Tier))));
        float Resonance = Info.getFloat("Resonance");
        Tooltip.add(Component.translatable("jade.scp_unleashed.instability_matrix.resonance", Math.round(Resonance * 100.0f)).withStyle(ChatFormatting.GRAY));
        if (Status.isRunning()) {
            float Flow = Info.getFloat("Flow");
            int Odds = (int) Math.round(MatrixTuning.oddsPerMinute(Tier, Flow, Resonance) * 100.0);
            Tooltip.add(Component.translatable("jade.scp_unleashed.instability_matrix.odds", Odds, MatrixTuning.clock(MatrixTuning.expectedSeconds(Tier, Flow, Resonance))));
        } else if (Status == MatrixStatus.Saturated) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.instability_matrix.saturated", Info.getInt("Nearby")).withStyle(ChatFormatting.AQUA));
        }
    }
}
