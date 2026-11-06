package net.rai.scp_unleashed.antimemetic.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.antimemetic.AntiMemeticPillsItem;
import net.rai.scp_unleashed.antimemetic.AntiMemeticRegistry;

@JeiPlugin
public class AntiMemeticJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "anti_memetic_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.anti_memetic_pills.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(AntiMemeticRegistry.Pills.get()),
                Component.translatable(Prefix + "1", AntiMemeticPillsItem.Doses),
                Component.translatable(Prefix + "2", AntiMemeticPillsItem.EffectTicks / 1200),
                Component.translatable(Prefix + "3", (AntiMemeticPillsItem.EffectTicks - AntiMemeticPillsItem.OverdoseTicks) / 1200),
                Component.translatable(Prefix + "4"));
    }
}
