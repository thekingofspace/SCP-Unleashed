package net.rai.scp_unleashed.scp409.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp409.Scp409CaseBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp409CaseJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp409_case");
    private static final String Prefix = "jade.scp_unleashed.scp409_case.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (Scp409CaseBlock.isOpen(Accessor.getBlockState())) {
            Tooltip.add(Component.translatable(Prefix + "open").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            Tooltip.add(Component.translatable(Prefix + "exposed").withStyle(ChatFormatting.GOLD));
        } else {
            Tooltip.add(Component.translatable(Prefix + "closed").withStyle(ChatFormatting.GRAY));
        }
    }
}
