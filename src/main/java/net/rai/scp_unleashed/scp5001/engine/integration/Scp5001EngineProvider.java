package net.rai.scp_unleashed.scp5001.engine.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.engine.Scp5001EngineBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.text.NumberFormat;
import java.util.Locale;

public enum Scp5001EngineProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp5001_engine");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockState().getBlock() instanceof Scp5001EngineBlock)) {
            return;
        }
        String Output = NumberFormat.getIntegerInstance(Locale.ROOT).format(Scp5001EngineBlock.Output);
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp5001_engine.output", Output).withStyle(ChatFormatting.LIGHT_PURPLE));
        Tooltip.add(Component.translatable("jade.scp_unleashed.scp5001_engine.coupling").withStyle(ChatFormatting.GRAY));
    }
}
