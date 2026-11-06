package net.rai.scp_unleashed.scp492.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp492.Scp492Figure;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp492JadeProvider implements IEntityComponentProvider {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp492");
    private static final String Prefix = "jade.scp_unleashed.scp492.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public int getDefaultPriority() {
        return Priority;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp492Figure Figure)) {
            return;
        }
        if (Figure.isTorn()) {
            Tooltip.add(Component.translatable(Prefix + "torn").withStyle(ChatFormatting.GOLD));
            return;
        }
        if (Figure.isAfraid()) {
            Tooltip.add(Component.translatable(Prefix + "afraid").withStyle(ChatFormatting.RED));
        }
        if (Figure.isTame()) {
            Tooltip.add(Component.translatable(Figure.isInSittingPose() ? Prefix + "watch" : Prefix + "crew").withStyle(ChatFormatting.GREEN));
        } else {
            Tooltip.add(Component.translatable(Prefix + "wary").withStyle(ChatFormatting.GRAY));
        }
        if (Figure.getHealth() < Figure.getMaxHealth()) {
            Tooltip.add(Component.translatable(Prefix + "frayed").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
