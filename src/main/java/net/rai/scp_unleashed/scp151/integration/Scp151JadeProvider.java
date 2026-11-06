package net.rai.scp_unleashed.scp151.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp151.Scp151PaintingBlock;
import net.rai.scp_unleashed.scp151.Scp151Sheet;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp151JadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp151_painting");
    private static final String Prefix = "jade.scp_unleashed.scp151_painting.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof Scp151PaintingBlock)) {
            return;
        }
        Scp151Sheet Sheet = State.getValue(Scp151PaintingBlock.Cover);
        if (Sheet.isDrawn()) {
            Tooltip.add(Component.translatable(Prefix + "covered", Sheet.colourName()).withStyle(ChatFormatting.GRAY));
        } else {
            Tooltip.add(Component.translatable(Prefix + "uncovered").withStyle(ChatFormatting.AQUA));
        }
    }
}
