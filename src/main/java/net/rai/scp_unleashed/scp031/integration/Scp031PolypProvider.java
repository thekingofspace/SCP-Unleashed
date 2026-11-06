package net.rai.scp_unleashed.scp031.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp031.Scp031Polyp;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp031PolypProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp031_polyp");
    private static final String Prefix = "jade.scp_unleashed.scp031_polyp.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp031Polyp Polyp)) {
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "growth", Polyp.stage() + 1, Scp031Polyp.Stages, Polyp.progressPercent()).withStyle(ChatFormatting.RED));
        Tooltip.add(Component.translatable(Prefix + "hint").withStyle(ChatFormatting.GRAY));
    }
}
