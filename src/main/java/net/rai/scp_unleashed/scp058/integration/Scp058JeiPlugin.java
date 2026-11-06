package net.rai.scp_unleashed.scp058.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp058.Scp058Heart;
import net.rai.scp_unleashed.scp058.Scp058Registry;

@JeiPlugin
public class Scp058JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp058_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.scp058_heart_spawn_egg.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp058Registry.HeartEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "1", (int) Scp058Heart.Health, (int) Scp058Heart.Armour),
                Component.translatable(Prefix + "2"),
                Component.translatable(Prefix + "3", (int) Scp058Heart.LashReach, (int) Scp058Heart.StingDamage),
                Component.translatable(Prefix + "4"));
    }
}
