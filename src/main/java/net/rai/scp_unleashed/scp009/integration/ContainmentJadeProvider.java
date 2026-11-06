package net.rai.scp_unleashed.scp009.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp009.Scp009Containment;
import net.rai.scp_unleashed.scp009.Scp009CrystalItem;
import net.rai.scp_unleashed.scp009.Scp009Exposure;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum ContainmentJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp009_containment");
    private static final String Prefix = "jade.scp_unleashed.scp009_containment.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(Accessor.getLevel(), Accessor.getPosition());
        if (Locker == null) {
            return;
        }
        ItemStack Held = Locker.peekContents();
        if (!Scp009CrystalItem.is(Held)) {
            return;
        }
        if (Scp009Containment.isContained(Locker)) {
            Tooltip.add(Component.translatable(Prefix + "contained").withStyle(ChatFormatting.AQUA));
        } else {
            Tooltip.add(Component.translatable(Prefix + "breach").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            Tooltip.add(Component.translatable(Prefix + "radius", (int) Scp009Exposure.Radius).withStyle(ChatFormatting.RED));
            Tooltip.add(Component.translatable(Prefix + "advice").withStyle(ChatFormatting.GOLD));
        }
    }
}
