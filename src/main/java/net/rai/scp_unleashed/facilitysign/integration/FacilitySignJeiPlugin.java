package net.rai.scp_unleashed.facilitysign.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.facilitysign.FacilitySignDesign;
import net.rai.scp_unleashed.facilitysign.FacilitySignRegistry;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class FacilitySignJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "facility_sign_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        List<ItemStack> Signs = new ArrayList<>();
        List<ItemStack> Plaques = new ArrayList<>();
        for (FacilitySignDesign Design : FacilitySignDesign.values()) {
            Signs.add(new ItemStack(FacilitySignRegistry.SignItems.get(Design).get()));
            Plaques.add(new ItemStack(FacilitySignRegistry.PlaqueItems.get(Design).get()));
        }
        Registration.addItemStackInfo(Signs,
                Component.translatable("jei.scp_unleashed.info.facility_sign.1"),
                Component.translatable("jei.scp_unleashed.info.facility_sign.2"),
                Component.translatable("jei.scp_unleashed.info.facility_sign.3"));
        List<ItemStack> Headers = new ArrayList<>();
        for (FacilitySignDesign Design : FacilitySignDesign.values()) {
            if (Design.hasHeader()) {
                Headers.add(new ItemStack(FacilitySignRegistry.HeaderItems.get(Design).get()));
            }
        }
        Registration.addItemStackInfo(Headers,
                Component.translatable("jei.scp_unleashed.info.facility_sign_header.1"),
                Component.translatable("jei.scp_unleashed.info.facility_sign_header.2"));
        Registration.addItemStackInfo(Plaques,
                Component.translatable("jei.scp_unleashed.info.facility_sign_small.1"),
                Component.translatable("jei.scp_unleashed.info.facility_sign.3"));
    }
}
