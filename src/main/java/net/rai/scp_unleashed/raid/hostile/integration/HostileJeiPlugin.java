package net.rai.scp_unleashed.raid.hostile.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;

@JeiPlugin
public class HostileJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = HostileRegistry.location("hostile_raids_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(HostileRegistry.Pod.get()),
                Component.translatable("jei.scp_unleashed.info.serpents_hand_stasis_pod.1"),
                Component.translatable("jei.scp_unleashed.info.serpents_hand_stasis_pod.2"),
                Component.translatable("jei.scp_unleashed.info.serpents_hand_stasis_pod.3"));
    }
}
