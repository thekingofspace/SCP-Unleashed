package net.rai.scp_unleashed.scp538.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp538.Scp538Spider;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp538JadeProvider implements IEntityComponentProvider {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp538");
    private static final String Prefix = "jade.scp_unleashed.scp538.";

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
        if (!(Accessor.getEntity() instanceof Scp538Spider Spider)) {
            return;
        }
        if (Spider.fade() > 0) {
            Tooltip.add(Component.translatable(Prefix + "thinning").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }
        switch (Spider.mood()) {
            case Scp538Spider.Hunting -> Tooltip.add(Component.translatable(Prefix + "hunting").withStyle(ChatFormatting.RED));
            case Scp538Spider.Restless -> Tooltip.add(Component.translatable(Prefix + "restless").withStyle(ChatFormatting.GOLD));
            case Scp538Spider.Fleeing -> Tooltip.add(Component.translatable(Prefix + "scattering").withStyle(ChatFormatting.GRAY));
            default -> Tooltip.add(Component.translatable(Prefix + "still").withStyle(ChatFormatting.GRAY));
        }
        Direction Side = Spider.surface();
        if (Side == Direction.UP) {
            Tooltip.add(Component.translatable(Prefix + "overhead").withStyle(ChatFormatting.DARK_GRAY));
        } else if (Side.getAxis().isHorizontal()) {
            Tooltip.add(Component.translatable(Prefix + "wall").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
