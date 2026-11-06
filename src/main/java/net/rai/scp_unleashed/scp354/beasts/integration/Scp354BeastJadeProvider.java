package net.rai.scp_unleashed.scp354.beasts.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp354.beasts.Scp354Beast;
import net.rai.scp_unleashed.scp354.beasts.Scp354Charger;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp354BeastJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp354_pool_beasts");
    private static final String Prefix = "jade.scp_unleashed.scp354_pool_beasts.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp354Beast Creature)) {
            return;
        }
        if (Creature.isEmerging()) {
            Tooltip.add(Component.translatable(Prefix + "emerging").withStyle(ChatFormatting.GOLD));
        } else if (Creature.isDripping()) {
            Tooltip.add(Component.translatable(Prefix + "dripping").withStyle(ChatFormatting.RED));
        }
        if (Creature instanceof Scp354Charger Charger && Charger.isStunned()) {
            Tooltip.add(Component.translatable(Prefix + "stunned").withStyle(ChatFormatting.YELLOW));
        }
    }
}
