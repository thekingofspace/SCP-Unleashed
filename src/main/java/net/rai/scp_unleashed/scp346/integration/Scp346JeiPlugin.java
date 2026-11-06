package net.rai.scp_unleashed.scp346.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp346.Scp346Pterosaur;
import net.rai.scp_unleashed.scp346.Scp346Registry;
import net.rai.scp_unleashed.scp346.Scp346Variant;

@JeiPlugin
public class Scp346JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp346_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.scp346_spawn_egg.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(Scp346Registry.Egg.get().getDefaultInstance(),
                Component.translatable(Prefix + "1", Scp346Variant.values().length),
                Component.translatable(Prefix + "2", (int) Scp346Pterosaur.Health),
                Component.translatable(Prefix + "3"),
                Component.translatable(Prefix + "4"));
    }
}
