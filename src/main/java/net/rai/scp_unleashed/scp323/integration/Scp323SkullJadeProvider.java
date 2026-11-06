package net.rai.scp_unleashed.scp323.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp323.Scp323Registry;
import net.rai.scp_unleashed.scp323.Scp323Skull;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp323SkullJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp323Registry.location("scp323_skull");
    private static final String Prefix = "jade.scp_unleashed.scp323.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp323Skull Skull)) {
            return;
        }
        if (!Skull.isRestless()) {
            Tooltip.add(Component.translatable(Prefix + "dormant").withStyle(ChatFormatting.GRAY));
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "restless").withStyle(ChatFormatting.DARK_RED));
        Tooltip.add(Component.translatable(Prefix + "gaze").withStyle(ChatFormatting.RED));
    }
}
