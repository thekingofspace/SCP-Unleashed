package net.rai.scp_unleashed.scp049.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp049.Scp049TrayBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp049TrayProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_tray");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockEntity() instanceof Scp049TrayBlockEntity Tray)) {
            return;
        }
        if (Tray.isLinked()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp049_tray.linked").withStyle(ChatFormatting.GOLD));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp049_tray.unlinked").withStyle(ChatFormatting.GRAY));
        }
        if (Tray.operations() > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.scp049_tray.operations", Tray.operations()).withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
