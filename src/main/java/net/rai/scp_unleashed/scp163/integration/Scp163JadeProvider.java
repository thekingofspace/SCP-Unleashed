package net.rai.scp_unleashed.scp163.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp163.Scp163Castaway;
import net.rai.scp_unleashed.scp163.Scp163Mood;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp163JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp163_castaway");
    private static final String Prefix = "jade.scp_unleashed.scp163.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp163Castaway Creature)) {
            return;
        }
        Scp163Mood Mood = Creature.mood();
        Component Feeling = Creature.isSettled() && Mood == Scp163Mood.Calm
                ? Component.translatable(Scp163Mood.SettledKey).withStyle(ChatFormatting.AQUA)
                : Component.translatable(Mood.translationKey()).withStyle(Mood.colour());
        Tooltip.add(Component.translatable(Prefix + "mood", Feeling).withStyle(ChatFormatting.GRAY));
        if (Creature.isPlaying(Scp163Castaway.ActionLookUp) || Creature.isPlaying(Scp163Castaway.ActionTap)) {
            Tooltip.add(Component.translatable(Prefix + "surveying").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
        Tooltip.add(Component.translatable(Prefix + (Creature.isSuited() ? "suited" : "unsuited")).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
