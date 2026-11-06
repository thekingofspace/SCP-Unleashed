package net.rai.scp_unleashed.scp035.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp035.Scp035Host;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp035HostJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp035_host");
    private static final String Prefix = "jade.scp_unleashed.scp035.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp035Host Host)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "possessed").withStyle(ChatFormatting.DARK_RED));
        if (!Host.getSkinName().isEmpty()) {
            Tooltip.add(Component.translatable(Prefix + "was", Host.getSkinName()).withStyle(ChatFormatting.GRAY));
        }
        int Decay = Math.round(Host.decay() * 100.0f);
        if (Decay > 0) {
            Tooltip.add(Component.translatable(Prefix + "decay", Decay).withStyle(ChatFormatting.DARK_GRAY));
        }
        Tooltip.add(Component.translatable(Prefix + "recover").withStyle(ChatFormatting.GOLD));
    }
}
