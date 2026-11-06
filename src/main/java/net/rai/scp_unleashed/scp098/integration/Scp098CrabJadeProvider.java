package net.rai.scp_unleashed.scp098.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp098.Scp098Crab;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp098CrabJadeProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp098_crab");
    private static final String Prefix = "jade.scp_unleashed.scp098_crab.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp098Crab Crab)) {
            return;
        }
        String State = switch (Crab.mode()) {
            case Scp098Crab.ModeRush -> "rushing";
            case Scp098Crab.ModeHunt -> "hunting";
            case Scp098Crab.ModeFeed -> "feeding";
            case Scp098Crab.ModeReturn -> "returning";
            case Scp098Crab.ModeForage -> "foraging";
            default -> "lost";
        };
        Tooltip.add(Component.translatable(Prefix + State).withStyle(ChatFormatting.GRAY));
        if (Crab.ripeness() < Scp098Crab.PaleBelow) {
            Tooltip.add(Component.translatable(Prefix + "pale").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
        }
    }
}
