package net.rai.scp_unleashed.scp149.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp149.Scp149Larva;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp149LarvaJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp149_larva");
    private static final String Prefix = "jade.scp_unleashed.scp149_larva.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp149Larva Creature)) {
            return;
        }
        if (Creature.gathering()) {
            Tooltip.add(Component.translatable(Prefix + "gathering").withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable(Prefix + "crawling").withStyle(ChatFormatting.GRAY));
        }
    }
}
