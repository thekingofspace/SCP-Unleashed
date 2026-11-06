package net.rai.scp_unleashed.upgrade.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModItems;
import net.rai.scp_unleashed.upgrade.SoundMuffling;

@JeiPlugin
public class MufflerJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "sound_muffler_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(ModItems.SoundMufflerUpgrade.get()),
                Component.translatable("jei.scp_unleashed.info.sound_muffler_upgrade.1"),
                Component.translatable("jei.scp_unleashed.info.sound_muffler_upgrade.2", Math.round(SoundMuffling.Factor * 100.0f)),
                Component.translatable("jei.scp_unleashed.info.sound_muffler_upgrade.3"));
    }
}
