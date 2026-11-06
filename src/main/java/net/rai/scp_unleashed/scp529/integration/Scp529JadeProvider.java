package net.rai.scp_unleashed.scp529.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp529.Scp529Cat;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp529JadeProvider implements IEntityComponentProvider {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp529");
    private static final String Prefix = "jade.scp_unleashed.scp529.";

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
        if (!(Accessor.getEntity() instanceof Scp529Cat Josie)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "designation").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (Josie.isDistressed()) {
            Tooltip.add(Component.translatable(Prefix + "restless").withStyle(ChatFormatting.RED));
        } else if (Josie.rest() == Scp529Cat.RestSleep || Josie.isLying()) {
            Tooltip.add(Component.translatable(Prefix + "asleep").withStyle(ChatFormatting.GRAY));
        } else if (Josie.rest() == Scp529Cat.RestLoaf) {
            Tooltip.add(Component.translatable(Prefix + "loaf").withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable(Prefix + "stray").withStyle(ChatFormatting.GRAY));
    }
}
