package net.rai.scp_unleashed.computer.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.ChatServerBlockEntity;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import net.rai.scp_unleashed.computer.MessageDatabaseBlockEntity;
import net.rai.scp_unleashed.computer.UploadPortBlockEntity;
import net.rai.scp_unleashed.computer.data.FloppyContents;

@JeiPlugin
public class ComputerJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "computer_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(ComputerRegistry.ComputerItem.get()),
                Component.translatable(Prefix + "computer.1"),
                Component.translatable(Prefix + "computer.2", ComputerBlockEntity.Draw),
                Component.translatable(Prefix + "computer.3"),
                Component.translatable(Prefix + "computer.4", ComputerBlockEntity.MaxFiles),
                Component.translatable(Prefix + "computer.5"),
                Component.translatable(Prefix + "computer.6"));
        Registration.addItemStackInfo(new ItemStack(ComputerRegistry.ChatServerItem.get()),
                Component.translatable(Prefix + "chat_server.1"),
                Component.translatable(Prefix + "chat_server.2", ChatServerBlockEntity.Draw),
                Component.translatable(Prefix + "chat_server.3"));
        Registration.addItemStackInfo(new ItemStack(ComputerRegistry.MessageDatabaseItem.get()),
                Component.translatable(Prefix + "message_database.1"),
                Component.translatable(Prefix + "message_database.2", MessageDatabaseBlockEntity.Capacity, MessageDatabaseBlockEntity.Draw),
                Component.translatable(Prefix + "message_database.3"));
        Registration.addItemStackInfo(new ItemStack(ComputerRegistry.UploadPortItem.get()),
                Component.translatable(Prefix + "network_upload_port.1"),
                Component.translatable(Prefix + "network_upload_port.2"),
                Component.translatable(Prefix + "network_upload_port.3", UploadPortBlockEntity.Draw));
        Registration.addItemStackInfo(new ItemStack(ComputerRegistry.FloppyDisc.get()),
                Component.translatable(Prefix + "floppy_disc.1", FloppyContents.Capacity),
                Component.translatable(Prefix + "floppy_disc.2"));
    }
}
