package net.rai.scp_unleashed.scp179.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp179.SatelliteTerminalBlockEntity;
import net.rai.scp_unleashed.scp179.Scp179Registry;

@JeiPlugin
public class Scp179JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp179_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp179Registry.TerminalItem.get()),
                Component.translatable(Prefix + "satellite_terminal.1"),
                Component.translatable(Prefix + "satellite_terminal.2", SatelliteTerminalBlockEntity.Draw),
                Component.translatable(Prefix + "satellite_terminal.coordinator"),
                Component.translatable(Prefix + "satellite_terminal.4", SatelliteTerminalBlockEntity.MaxLocations));
        Registration.addItemStackInfo(new ItemStack(Scp179Registry.Coordinator.get()),
                Component.translatable(Prefix + "space_coordinator.1"),
                Component.translatable(Prefix + "space_coordinator.store"));
    }
}
