package net.rai.scp_unleashed.scp256.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp256.Scp256Registry;
import net.rai.scp_unleashed.scp256.Scp256TypewriterBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp256JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp256Registry.location("scp256_typewriter");
    private static final String Prefix = "jade.scp_unleashed.scp256_typewriter.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockEntity() instanceof Scp256TypewriterBlockEntity Typewriter)) {
            return;
        }
        if (Typewriter.isTyping(Accessor.getLevel().getGameTime())) {
            Tooltip.add(Component.translatable(Prefix + "typing").withStyle(ChatFormatting.GOLD));
        } else {
            Tooltip.add(Component.translatable(Prefix + "idle").withStyle(ChatFormatting.GRAY));
        }
        if (Accessor.showDetails() && Typewriter.getUses() > 0) {
            Tooltip.add(Component.translatable(Prefix + "pages", Typewriter.getUses()).withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
