package net.rai.scp_unleashed.scp3008.staff.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp3008.staff.Scp3008StaffRegistry;

@JeiPlugin
public class Scp3008StaffJeiPlugin implements IModPlugin {
    public static final int Pages = 5;
    private static final ResourceLocation Uid = Scp3008StaffRegistry.location("scp3008_staff_jei_plugin");
    private static final String EggPrefix = "jei.scp_unleashed.info.scp3008_staff_spawn_egg.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Component[] Lines = new Component[Pages];
        for (int Index = 0; Index < Pages; Index++) {
            Lines[Index] = Component.translatable(EggPrefix + (Index + 1));
        }
        Registration.addItemStackInfo(Scp3008StaffRegistry.Egg.get().getDefaultInstance(), Lines);
    }
}
