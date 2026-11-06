package net.rai.scp_unleashed.scp149.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp149.Scp149CocoonBlock;
import net.rai.scp_unleashed.scp149.Scp149CocoonBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp149CocoonJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp149_cocoon");
    private static final String Prefix = "jade.scp_unleashed.scp149_cocoon.";
    private static final ChatFormatting[] Colours = {ChatFormatting.GRAY, ChatFormatting.GRAY, ChatFormatting.RED, ChatFormatting.DARK_RED};

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockEntity() instanceof Scp149CocoonBlockEntity Cocoon)) {
            return;
        }
        if (Cocoon.held()) {
            Tooltip.add(Component.translatable(Prefix + "dormant").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            int Stage = Accessor.getBlockState().getValue(Scp149CocoonBlock.Stage);
            Tooltip.add(Component.translatable(Prefix + "stage." + Stage).withStyle(Colours[Stage]));
        }
        if (Cocoon.waiting() >= Scp149CocoonBlockEntity.Crowded) {
            Tooltip.add(Component.translatable(Prefix + "crowded").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }
}
