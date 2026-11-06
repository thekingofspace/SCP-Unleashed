package net.rai.scp_unleashed.disperser.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.disperser.DisperserRegistry;
import net.rai.scp_unleashed.disperser.OrganicGrenadeItem;
import net.rai.scp_unleashed.disperser.OrganicGrenadeRecipe;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class DisperserJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "disperser_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration Registration) {
        Registration.registerSubtypeInterpreter(DisperserRegistry.OrganicGrenade.get(), new ISubtypeInterpreter<>() {
            @Nullable
            @Override
            public Object getSubtypeData(ItemStack Stack, UidContext Context) {
                ResourceLocation Culture = OrganicGrenadeItem.cultureOf(Stack);
                return Culture == null ? null : Culture.toString();
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack Stack, UidContext Context) {
                ResourceLocation Culture = OrganicGrenadeItem.cultureOf(Stack);
                return Culture == null ? "" : Culture.toString();
            }
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        List<RecipeHolder<CraftingRecipe>> Loading = new ArrayList<>();
        List<ItemStack> Grenades = new ArrayList<>();
        Grenades.add(new ItemStack(DisperserRegistry.OrganicGrenade.get()));
        for (LabOrganism Organism : LabOrganisms.all()) {
            ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "jei/organic_grenade/" + Organism.getId().getNamespace() + "_" + Organism.getId().getPath());
            Loading.add(new RecipeHolder<>(Id, OrganicGrenadeRecipe.display(Organism)));
            Grenades.add(OrganicGrenadeItem.loaded(Organism.getId()));
        }
        Registration.addRecipes(RecipeTypes.CRAFTING, Loading);
        Registration.addItemStackInfo(Grenades,
                Component.translatable(Prefix + "organic_grenade.1"),
                Component.translatable(Prefix + "organic_grenade.2"),
                Component.translatable(Prefix + "organic_grenade.3"));
        Registration.addItemStackInfo(new ItemStack(DisperserRegistry.DisperserItem.get()),
                Component.translatable(Prefix + "disperser.1"),
                Component.translatable(Prefix + "disperser.2"),
                Component.translatable(Prefix + "disperser.3"));
    }
}
