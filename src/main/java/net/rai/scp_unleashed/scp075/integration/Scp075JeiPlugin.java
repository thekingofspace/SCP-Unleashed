package net.rai.scp_unleashed.scp075.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp075.Scp075Registry;
import net.rai.scp_unleashed.scp075.Scp075Snail;

@JeiPlugin
public class Scp075JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp075_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.scp075_snail_spawn_egg.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp075Registry.SnailEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "1", (int) Scp075Snail.Health),
                Component.translatable(Prefix + "2", (int) Scp075Snail.HoldRange),
                Component.translatable(Prefix + "3"),
                Component.translatable(Prefix + "4"));
    }
}
