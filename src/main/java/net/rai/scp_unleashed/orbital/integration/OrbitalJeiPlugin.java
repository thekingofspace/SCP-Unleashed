package net.rai.scp_unleashed.orbital.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.orbital.OrbitalPart;
import net.rai.scp_unleashed.orbital.OrbitalRegistry;

@JeiPlugin
public class OrbitalJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "orbital_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        for (OrbitalPart Part : OrbitalPart.values()) {
            Registration.addItemStackInfo(new ItemStack(OrbitalRegistry.part(Part)),
                    Component.translatable("jei.scp_unleashed.info.orbital_part.1"),
                    Component.translatable("jei.scp_unleashed.info.orbital_part.2"),
                    Component.translatable("jei.scp_unleashed.info.orbital_part.3"));
        }
        Registration.addItemStackInfo(new ItemStack(OrbitalRegistry.Designator.get()),
                Component.translatable("jei.scp_unleashed.info.orbital_designator.1"),
                Component.translatable("jei.scp_unleashed.info.orbital_designator.2"),
                Component.translatable("jei.scp_unleashed.info.orbital_designator.3"));
    }
}
