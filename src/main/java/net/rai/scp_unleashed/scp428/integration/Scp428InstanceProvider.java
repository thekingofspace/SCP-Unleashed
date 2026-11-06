package net.rai.scp_unleashed.scp428.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp428.Scp428Instance;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp428InstanceProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp428_instance");
    private static final String Prefix = "jade.scp_unleashed.scp428_instance.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp428Instance Body)) {
            return;
        }
        String Former = Body.formerName();
        if (!Former.isEmpty()) {
            Tooltip.add(Component.translatable(Prefix + "former", Former).withStyle(ChatFormatting.GRAY));
        }
        if (Body.holdingId() >= 0) {
            Tooltip.add(Component.translatable(Prefix + "holding").withStyle(ChatFormatting.DARK_RED));
        } else if (Body.huddle() == Scp428Instance.Guarding) {
            Tooltip.add(Component.translatable(Prefix + "guarding").withStyle(ChatFormatting.DARK_RED));
        } else if (Body.huddle() != Scp428Instance.Loose) {
            Tooltip.add(Component.translatable(Prefix + "huddled").withStyle(ChatFormatting.GRAY));
        }
    }
}
