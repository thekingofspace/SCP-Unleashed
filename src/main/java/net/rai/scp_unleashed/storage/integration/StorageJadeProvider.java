package net.rai.scp_unleashed.storage.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageEnvironment;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElementHelper;

public enum StorageJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "storage_container");
    private static final String Prefix = "jade.scp_unleashed.storage.";

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
        if (Held.isEmpty()) {
            Tooltip.add(Component.translatable(Prefix + "contents", Component.translatable(Prefix + "empty")).withStyle(ChatFormatting.GRAY));
        } else {
            Tooltip.add(IElementHelper.get().smallItem(Held));
            Tooltip.append(Component.translatable(Prefix + "contents", Held.getHoverName()).withStyle(ChatFormatting.GREEN));
        }
        boolean Open = Locker.isOpen();
        Tooltip.add(Component.translatable(Prefix + (Open ? "open" : "closed")).withStyle(Open ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        if (Locker.hasGlassFront()) {
            Tooltip.add(Component.translatable(Prefix + "glass", Locker.peekGlass().getHoverName()).withStyle(ChatFormatting.AQUA));
        }
        StorageEnvironment Environment = Locker.getEnvironment();
        Tooltip.add(Component.translatable(Prefix + "environment", Environment.getDisplayName()).withStyle(Environment.getColour()));
    }
}
