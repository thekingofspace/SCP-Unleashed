package net.rai.scp_unleashed.scp131.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp131.Scp131Colour;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import net.rai.scp_unleashed.scp131.Scp131PodData;
import net.rai.scp_unleashed.scp131.Scp131Registry;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class Scp131JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp131_jei_plugin");
    private static final String PodPrefix = "jei.scp_unleashed.info.scp131_eye_pod.";
    private static final String EggPrefix = "jei.scp_unleashed.info.scp131_pod_spawn_egg.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        List<ItemStack> Pods = new ArrayList<>();
        for (Scp131Colour Colour : Scp131Colour.values()) {
            Pods.add(new Scp131PodData(Colour, (float) Scp131Pod.Health, 0).stack());
        }
        Registration.addIngredientInfo(Pods, VanillaTypes.ITEM_STACK,
                Component.translatable(PodPrefix + "1"),
                Component.translatable(PodPrefix + "2"),
                Component.translatable(PodPrefix + "3"),
                Component.translatable(PodPrefix + "4"));
        Registration.addItemStackInfo(Scp131Registry.PodEgg.get().getDefaultInstance(),
                Component.translatable(EggPrefix + "1", Scp131Colour.values().length),
                Component.translatable(EggPrefix + "2", (int) Scp131Pod.Health),
                Component.translatable(EggPrefix + "3"),
                Component.translatable(EggPrefix + "4"));
    }
}
