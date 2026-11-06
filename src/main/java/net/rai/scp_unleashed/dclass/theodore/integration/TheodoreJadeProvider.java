package net.rai.scp_unleashed.dclass.theodore.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClassScores;
import net.rai.scp_unleashed.dclass.theodore.Theodore;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum TheodoreJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "theodore");
    private static final String Prefix = "jade.scp_unleashed.theodore.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Theodore Subject)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "scores", Subject.iq(), Subject.athletic(), DClassScores.band(Subject.athletic())).withStyle(ChatFormatting.GRAY));
        switch (Subject.state()) {
            case Theodore.StateSeeking -> Tooltip.add(Component.translatable(Prefix + "seeking").withStyle(ChatFormatting.GOLD));
            case Theodore.StateEating -> Tooltip.add(Component.translatable(Prefix + "eating").withStyle(ChatFormatting.RED));
            case Theodore.StateCloning -> Tooltip.add(Component.translatable(Prefix + "cloning", Subject.clusterCount(), Theodore.ClusterSize).withStyle(ChatFormatting.RED));
            case Theodore.StateClone -> Tooltip.add(Component.translatable(Prefix + "clone").withStyle(ChatFormatting.GOLD));
            case Theodore.StateMerging -> Tooltip.add(Component.translatable(Prefix + "merging").withStyle(ChatFormatting.DARK_RED));
            default -> Tooltip.add(Component.translatable(Prefix + "idle").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
