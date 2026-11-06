package net.rai.scp_unleashed.scp5001.mekhane.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneProtocol;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneValues;
import net.rai.scp_unleashed.scp5001.mekhane.block.MekhaneStatusBlockEntity;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001EmergencyIntakeBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum MekhaneJade implements IBlockComponentProvider {
    Intake("scp5001_emergency_intake", true),
    Console("scp5001_emergency_console", false);

    private final ResourceLocation Uid;
    private final boolean Totals;

    MekhaneJade(String Name, boolean Totals) {
        this.Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
        this.Totals = Totals;
    }

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockEntity() instanceof MekhaneStatusBlockEntity Status)) {
            return;
        }
        MekhaneProtocol.Stage Stage = Status.stage();
        if (Stage == MekhaneProtocol.Stage.Idle && !MekhaneStatusBlockEntity.lit(Stage, Status.phase())) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp5001_emergency.inert").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (Stage == MekhaneProtocol.Stage.Idle) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp5001_emergency.awaiting").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (Stage != MekhaneProtocol.Stage.Engaged || !Totals) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp5001_emergency.stage." + Stage.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GOLD));
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp5001_emergency.iron", Status.iron() / MekhaneValues.Scale, Status.ironNeed() / MekhaneValues.Scale,
                Scp5001EmergencyIntakeBlock.percent(Status.iron(), Status.ironNeed())).withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp5001_emergency.electronics", Status.electronics() / MekhaneValues.Scale, Status.electronicsNeed() / MekhaneValues.Scale,
                Scp5001EmergencyIntakeBlock.percent(Status.electronics(), Status.electronicsNeed())).withStyle(ChatFormatting.GRAY));
    }
}
