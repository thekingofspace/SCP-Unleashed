package net.rai.scp_unleashed.props.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.props.DecorationRecipe;
import net.rai.scp_unleashed.props.client.DecorationWorkbenchScreen;

@JeiPlugin
public class PropsJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "props");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new DecorationRecipeCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World == null) {
            return;
        }
        Registration.addRecipes(DecorationRecipeCategory.Type, DecorationRecipe.sorted(World.getRecipeManager()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(DecorationRecipeCategory.catalyst(), DecorationRecipeCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        Registration.addRecipeClickArea(DecorationWorkbenchScreen.class, DecorationWorkbenchScreen.ArrowX, DecorationWorkbenchScreen.ArrowY, DecorationWorkbenchScreen.ArrowWidth,
                DecorationWorkbenchScreen.ArrowHeight, DecorationRecipeCategory.Type);
    }
}
