package net.rai.scp_unleashed.scp215.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp215.Scp215GlassesItem;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp215JadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp215_glasses");
    private static final String Prefix = "jade.scp_unleashed.scp215.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        Entity Target = Accessor.getEntity();
        if (!(Target instanceof LivingEntity Living)) {
            return;
        }
        Player Viewer = Minecraft.getInstance().player;
        if (Viewer != null && Viewer != Living && Scp215GlassesItem.isWorn(Viewer) && !Viewer.isSpectator()) {
            Tooltip.add(Component.translatable(Prefix + "wrong").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
        if (Scp215GlassesItem.isWorn(Living)) {
            Tooltip.add(Component.translatable(Prefix + "wearing").withStyle(ChatFormatting.GRAY));
        }
    }
}
