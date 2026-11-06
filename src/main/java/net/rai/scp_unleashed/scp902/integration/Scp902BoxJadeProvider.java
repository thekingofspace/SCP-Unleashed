package net.rai.scp_unleashed.scp902.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp902.Scp902BoxBlock;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp902BoxJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp902Registry.location("scp902_box");
    private static final String Prefix = "jade.scp_unleashed.scp902_box.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        boolean Open = Scp902BoxBlock.isOpen(Accessor.getBlockState());
        Tooltip.add(Component.translatable(Prefix + (Open ? "open" : "closed")).withStyle(Open ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        if (Open) {
            Tooltip.add(Component.translatable(Prefix + "empty").withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable(Prefix + "ticking").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
