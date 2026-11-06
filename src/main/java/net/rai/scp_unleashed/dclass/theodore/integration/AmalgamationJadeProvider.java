package net.rai.scp_unleashed.dclass.theodore.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.theodore.Theodore;
import net.rai.scp_unleashed.dclass.theodore.TheodoreAmalgamation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum AmalgamationJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "theodore_amalgamation");
    private static final String Prefix = "jade.scp_unleashed.theodore_amalgamation.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof TheodoreAmalgamation Mass)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "absorbed", Theodore.ClusterSize).withStyle(ChatFormatting.DARK_RED));
        if (Mass.action() == TheodoreAmalgamation.ActionRise) {
            Tooltip.add(Component.translatable(Prefix + "rising").withStyle(ChatFormatting.RED));
        } else if (Mass.action() == TheodoreAmalgamation.ActionSlam) {
            Tooltip.add(Component.translatable(Prefix + "slam").withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable(Prefix + "hunting").withStyle(ChatFormatting.GRAY));
        }
    }
}
