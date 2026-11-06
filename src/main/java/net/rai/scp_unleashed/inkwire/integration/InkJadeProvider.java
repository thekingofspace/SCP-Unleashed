package net.rai.scp_unleashed.inkwire.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.inkwire.InkMarks;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum InkJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "invisible_ink");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Lines, BlockAccessor Accessor, IPluginConfig Config) {
        if (InkMarks.isHidden(Accessor.getBlockState())) {
            Lines.add(Component.translatable("jade.scp_unleashed.invisible_ink.hidden").withStyle(ChatFormatting.AQUA));
        }
    }
}
