package net.rai.scp_unleashed.playerclone.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.playerclone.PlayerClone;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum PlayerCloneJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "player_clone");
    private static final String Prefix = "jade.scp_unleashed.player_clone.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof PlayerClone Clone)) {
            return;
        }
        String Source = Clone.sourceName();
        if (Source.isEmpty()) {
            Tooltip.add(Component.translatable(Prefix + "unknown").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            Tooltip.add(Component.translatable(Prefix + "source", Source).withStyle(ChatFormatting.GOLD));
        }
    }
}
