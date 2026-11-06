package net.rai.scp_unleashed.scp148.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp148.Scp148Registry;
import net.rai.scp_unleashed.scp148.Scp148Shield;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp148JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp148Registry.location("scp148_block");
    private static final String Prefix = "jade.scp_unleashed.scp148_block.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        Tooltip.add(Component.translatable(Prefix + "hum").withStyle(ChatFormatting.DARK_GREEN));
        if (Scp148Shield.isWarded(Accessor.getPlayer())) {
            Tooltip.add(Component.translatable(Prefix + "quiet").withStyle(ChatFormatting.GREEN));
        }
        if (Accessor.showDetails()) {
            Tooltip.add(Component.translatable(Prefix + "details").withStyle(ChatFormatting.GRAY));
        }
    }
}
