package net.rai.scp_unleashed.scp553.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp553.Scp553Butterfly;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp553JadeProvider implements IEntityComponentProvider {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp553");
    private static final String Prefix = "jade.scp_unleashed.scp553.";

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
        if (!(Accessor.getEntity() instanceof Scp553Butterfly Butterfly)) {
            return;
        }
        if (Butterfly.isAgitated()) {
            Tooltip.add(Component.translatable(Prefix + "agitated").withStyle(ChatFormatting.RED));
        } else if (Butterfly.isPerched()) {
            Tooltip.add(Component.translatable(Prefix + "resting").withStyle(ChatFormatting.AQUA));
        } else {
            Tooltip.add(Component.translatable(Prefix + "calm").withStyle(ChatFormatting.GRAY));
        }
        if (Butterfly.getHealth() < Butterfly.getMaxHealth()) {
            Tooltip.add(Component.translatable(Prefix + "chipped").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
