package net.rai.scp_unleashed.scp093.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp093.Scp093Registry;
import net.rai.scp_unleashed.scp093.Scp093Torso;

@JeiPlugin
public class Scp093JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp093Registry.location("scp093_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp093Registry.Disc.get()),
                Component.translatable("jei.scp_unleashed.info.scp093_disc.1"),
                Component.translatable("jei.scp_unleashed.info.scp093_disc.2"),
                Component.translatable("jei.scp_unleashed.info.scp093_disc.3"));
        Registration.addItemStackInfo(new ItemStack(Scp093Registry.MirrorItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp093_mirror.1"),
                Component.translatable("jei.scp_unleashed.info.scp093_mirror.2"));
        Registration.addItemStackInfo(new ItemStack(Scp093Registry.Tears.get()),
                Component.translatable("jei.scp_unleashed.info.scp093_tears.1"),
                Component.translatable("jei.scp_unleashed.info.scp093_tears.2"),
                Component.translatable("jei.scp_unleashed.info.scp093_tears.3"));
        Registration.addItemStackInfo(new ItemStack(Scp093Registry.TorsoEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp093_torso_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp093_torso_spawn_egg.2", (int) Scp093Torso.Health));
    }
}
