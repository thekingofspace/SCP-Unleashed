package net.rai.scp_unleashed.archive.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.ArchiveRegistry;

@JeiPlugin
public class ArchiveJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "archive_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(ArchiveRegistry.DatabaseItem.get()),
                Component.translatable("jei.scp_unleashed.info.archive_database.1"),
                Component.translatable("jei.scp_unleashed.info.archive_database.2"),
                Component.translatable("jei.scp_unleashed.info.archive_database.3"),
                Component.translatable("jei.scp_unleashed.info.archive_database.4"));
        Registration.addItemStackInfo(new ItemStack(ArchiveRegistry.DatabaseTerminalItem.get()),
                Component.translatable("jei.scp_unleashed.info.archive_database_terminal.1"),
                Component.translatable("jei.scp_unleashed.info.archive_database_terminal.2"));
        Registration.addItemStackInfo(new ItemStack(ArchiveRegistry.DatabaseCopy.get()),
                Component.translatable("jei.scp_unleashed.info.archive_database_copy.1"),
                Component.translatable("jei.scp_unleashed.info.archive_database_copy.2"));
    }
}
