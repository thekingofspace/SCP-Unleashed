package net.rai.scp_unleashed.scp1762.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1762.Scp1762BoxBlock;
import net.rai.scp_unleashed.scp1762.Scp1762BoxBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp1762BoxJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp1762_box");
    private static final String Prefix = "jade.scp_unleashed.scp1762_box.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!Scp1762BoxBlock.isOpen(Accessor.getBlockState())) {
            Tooltip.add(Component.translatable(Prefix + "closed").withStyle(ChatFormatting.GRAY));
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "open").withStyle(ChatFormatting.GRAY));
        if (Accessor.getBlockEntity() instanceof Scp1762BoxBlockEntity Box && Box.guarded()) {
            Tooltip.add(Component.translatable(Prefix + "out").withStyle(ChatFormatting.GOLD));
        }
    }
}
