package net.rai.scp_unleashed.scp111.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp111.Scp111Clutch;
import net.rai.scp_unleashed.scp111.Scp111EggItem;
import net.rai.scp_unleashed.scp111.Scp111Shelf;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp111ShelfJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp111_shelf");
    private static final String Prefix = "jade.scp_unleashed.scp111_shelf.";

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
        if (!Scp111Shelf.handles(Held)) {
            return;
        }
        Scp111Clutch Clutch = Scp111Clutch.of(Held);
        if (Scp111EggItem.is(Held)) {
            Tooltip.add(Component.translatable(Prefix + "eggs", Clutch.size(), Scp111Shelf.capacity(Locker)).withStyle(ChatFormatting.GRAY));
        } else {
            Tooltip.add(Component.translatable(Prefix + "boxed", Clutch.size()).withStyle(ChatFormatting.GRAY));
        }
        if (Clutch.due() > 0) {
            Tooltip.add(Component.translatable(Prefix + "stirring").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else if (Scp111Shelf.rate(Locker.getEnvironment()) == 0) {
            Tooltip.add(Component.translatable(Prefix + "dormant").withStyle(ChatFormatting.AQUA));
        } else {
            Tooltip.add(Component.translatable(Prefix + "incubating").withStyle(ChatFormatting.YELLOW));
        }
    }
}
