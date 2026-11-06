package net.rai.scp_unleashed.scp548.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cooler.CoolerClimate;
import net.rai.scp_unleashed.scp548.Scp548Spider;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp548JadeProvider implements IEntityComponentProvider {
    Instance;

    public static final int Priority = 100;
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp548");
    private static final String Prefix = "jade.scp_unleashed.scp548.";

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
        if (!(Accessor.getEntity() instanceof Scp548Spider Spider)) {
            return;
        }
        if (Spider.isProvoked() && !Spider.isHostile()) {
            Tooltip.add(Component.translatable(Prefix + "provoked").withStyle(ChatFormatting.RED));
        } else {
            switch (Spider.mood()) {
                case Scp548Spider.Contained -> Tooltip.add(Component.translatable(Prefix + "sluggish").withStyle(ChatFormatting.AQUA));
                case Scp548Spider.Hostile -> Tooltip.add(Component.translatable(Prefix + "agitated").withStyle(ChatFormatting.RED));
                default -> Tooltip.add(Component.translatable(Prefix + "stirring").withStyle(ChatFormatting.GOLD));
            }
        }
        if (!Spider.isContained() && CoolerClimate.at(Spider.level(), Spider.blockPosition()) != CoolerClimate.Cold) {
            Tooltip.add(Component.translatable(Prefix + "warm").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
        Tooltip.add(Component.translatable(Prefix + "cold").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
    }
}
