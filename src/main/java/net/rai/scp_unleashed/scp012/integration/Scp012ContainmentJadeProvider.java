package net.rai.scp_unleashed.scp012.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp012.Scp012CompositionItem;
import net.rai.scp_unleashed.scp012.Scp012Containment;
import net.rai.scp_unleashed.scp012.Scp012Exposure;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp012ContainmentJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp012_containment");
    private static final String Prefix = "jade.scp_unleashed.scp012_containment.";

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
        if (!Scp012CompositionItem.is(Held)) {
            return;
        }
        if (Scp012Containment.isExposed(Locker)) {
            Tooltip.add(Component.translatable(Prefix + "exposed").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            Tooltip.add(Component.translatable(Prefix + "range", Scp012Exposure.WindowRange).withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable(Prefix + "inert").withStyle(ChatFormatting.AQUA));
        }
        Tooltip.add(Component.translatable(Prefix + "meter", Scp012CompositionItem.completeness(Held)).withStyle(ChatFormatting.GRAY));
    }
}
