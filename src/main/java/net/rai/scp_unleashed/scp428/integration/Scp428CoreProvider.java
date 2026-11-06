package net.rai.scp_unleashed.scp428.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp428.Scp428Core;
import net.rai.scp_unleashed.scp428.Scp428Size;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp428CoreProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp428");
    private static final String Prefix = "jade.scp_unleashed.scp428.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp428Core Core)) {
            return;
        }
        int Count = Core.count();
        ChatFormatting Colour = Count >= Scp428Size.Capacity ? ChatFormatting.DARK_RED : Count > Scp428Size.Capacity / 2 ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE;
        Tooltip.add(Component.translatable(Prefix + "count", Count, Scp428Size.Capacity).withStyle(Colour));
        if (Core.state() == Scp428Core.StateGripping) {
            Tooltip.add(Component.translatable(Prefix + "gripping").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
