package net.rai.scp_unleashed.scp076.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp076.Scp076CoffinBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp076CoffinProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp076_coffin");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof Scp076CoffinBlock)) {
            return;
        }
        boolean Open = State.getValue(Scp076CoffinBlock.Open);
        Tooltip.add(Component.translatable(Open ? "jade.scp_unleashed.scp076_coffin.open" : "jade.scp_unleashed.scp076_coffin.closed")
                .withStyle(Open ? ChatFormatting.DARK_RED : ChatFormatting.GRAY));
    }
}
