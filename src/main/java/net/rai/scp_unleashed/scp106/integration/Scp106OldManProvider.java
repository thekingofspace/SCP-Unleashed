package net.rai.scp_unleashed.scp106.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp106.Scp106Modes;
import net.rai.scp_unleashed.scp106.Scp106OldMan;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp106OldManProvider implements IEntityComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp106_old_man");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, EntityAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getEntity() instanceof Scp106OldMan Creature)) {
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp106.class").withStyle(ChatFormatting.RED));
        String Mode = Scp106Modes.key(Creature.mode());
        ChatFormatting Colour = switch (Creature.mode()) {
            case Scp106OldMan.ModeHunt, Scp106OldMan.ModeBreaching, Scp106OldMan.ModeEmerging -> ChatFormatting.DARK_RED;
            case Scp106OldMan.ModeContained -> ChatFormatting.AQUA;
            default -> ChatFormatting.GRAY;
        };
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp106.mode." + Mode).withStyle(Colour));
    }
}
