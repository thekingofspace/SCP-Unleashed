package net.rai.scp_unleashed.scp465.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp465.Scp465BoxBlock;
import net.rai.scp_unleashed.scp465.Scp465BoxBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp465BoxJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp465_box");
    private static final String Prefix = "jade.scp_unleashed.scp465_box.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!Scp465BoxBlock.isOpen(Accessor.getBlockState())) {
            Tooltip.add(Component.translatable(Prefix + "closed").withStyle(ChatFormatting.GRAY));
            return;
        }
        Tooltip.add(Component.translatable(Prefix + "open").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (Accessor.getBlockEntity() instanceof Scp465BoxBlockEntity Carton && Carton.partying()) {
            long Now = Accessor.getLevel().getGameTime();
            String State = Now < Carton.musicStart() ? "starting" : "running";
            Tooltip.add(Component.translatable(Prefix + State).withStyle(ChatFormatting.GOLD));
            Tooltip.add(Component.translatable(Prefix + "celebrants", Carton.celebrants()).withStyle(ChatFormatting.GRAY));
        }
    }
}
