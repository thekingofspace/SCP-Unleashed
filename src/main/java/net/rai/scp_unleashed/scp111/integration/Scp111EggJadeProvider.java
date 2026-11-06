package net.rai.scp_unleashed.scp111.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp111.Scp111EggBlock;
import net.rai.scp_unleashed.scp111.Scp111EggItem;
import net.rai.scp_unleashed.scp111.Scp111Variant;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp111EggJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp111_eggs");
    private static final String Prefix = "jade.scp_unleashed.scp111_eggs.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (!(State.getBlock() instanceof Scp111EggBlock)) {
            return;
        }
        Scp111Variant Variant = State.getValue(Scp111EggBlock.Variant);
        Tooltip.add(Variant.displayName());
        Tooltip.add(Component.translatable(Prefix + "count", State.getValue(Scp111EggBlock.Eggs)).withStyle(ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Scp111EggItem.Key + ".stage." + State.getValue(Scp111EggBlock.Hatch)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
