package net.rai.scp_unleashed.scp035.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp035.Scp035Lure;
import net.rai.scp_unleashed.scp035.Scp035Mask;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp035MaskJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp035_mask");
    private static final String Prefix = "jade.scp_unleashed.scp035.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp035Mask)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "resting").withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Prefix + "gaze", (int) Scp035Lure.PullRange).withStyle(ChatFormatting.RED));
    }
}
