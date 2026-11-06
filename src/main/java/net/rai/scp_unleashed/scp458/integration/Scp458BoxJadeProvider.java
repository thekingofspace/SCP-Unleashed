package net.rai.scp_unleashed.scp458.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp458.Scp458BoxBlock;
import net.rai.scp_unleashed.scp458.Scp458BoxBlockEntity;
import net.rai.scp_unleashed.scp458.Scp458Contents;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp458BoxJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp458_box");
    private static final String Prefix = "jade.scp_unleashed.scp458_box.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!Scp458BoxBlock.isOpen(Accessor.getBlockState())) {
            Tooltip.add(Component.translatable(Prefix + "closed").withStyle(ChatFormatting.GRAY));
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "open").withStyle(ChatFormatting.GOLD));
        if (Accessor.getBlockEntity() instanceof Scp458BoxBlockEntity Carton) {
            Scp458Contents Held = Carton.contents();
            Held.Pizza().ifPresent(Served -> Tooltip.add(Component.translatable(Prefix + "inside", Served.displayName()).withStyle(ChatFormatting.WHITE)));
            if (Held.empty()) {
                Tooltip.add(Component.translatable(Prefix + "empty").withStyle(ChatFormatting.GRAY));
            } else {
                Tooltip.add(Component.translatable(Prefix + "slices", Held.left(), Scp458Contents.Slices).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
