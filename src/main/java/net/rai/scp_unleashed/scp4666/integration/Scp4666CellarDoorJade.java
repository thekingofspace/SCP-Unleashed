package net.rai.scp_unleashed.scp4666.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DoorBlock;
import net.rai.scp_unleashed.Scp_unleashed;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp4666CellarDoorJade implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp4666_cellar_door");
    private static final String Prefix = "jade.scp_unleashed.scp4666_cellar_door.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        boolean Open = Accessor.getBlockState().hasProperty(DoorBlock.OPEN) && Accessor.getBlockState().getValue(DoorBlock.OPEN);
        Tooltip.add(Component.translatable(Prefix + (Open ? "open" : "shut")).withStyle(Open ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
    }
}
