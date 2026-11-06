package net.rai.scp_unleashed.advmaterials.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.advmaterials.MagnetizedDoorBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum MagnetizedProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "magnetized_steel");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (State.getBlock() instanceof MagnetizedDoorBlock) {
            boolean Open = State.getValue(MagnetizedDoorBlock.Open);
            Tooltip.add(Component.translatable(Open ? "jade.scp_unleashed.magnetized_bulkhead_door.open" : "jade.scp_unleashed.magnetized_bulkhead_door.sealed")
                    .withStyle(Open ? ChatFormatting.GOLD : ChatFormatting.AQUA));
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.magnetized_block.charged").withStyle(ChatFormatting.AQUA));
    }
}
