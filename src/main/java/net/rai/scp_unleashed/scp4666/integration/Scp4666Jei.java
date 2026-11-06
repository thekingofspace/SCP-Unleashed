package net.rai.scp_unleashed.scp4666.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

import java.util.List;

@JeiPlugin
public class Scp4666Jei implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp4666_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    private static void info(IRecipeRegistration Registration, ItemStack Stack, String Name, int Lines) {
        Component[] Text = new Component[Lines];
        for (int Index = 0; Index < Lines; Index++) {
            Text[Index] = Component.translatable(Prefix + Name + "." + (Index + 1));
        }
        Registration.addItemStackInfo(Stack, Text);
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        info(Registration, Scp4666Registry.Egg.get().getDefaultInstance(), "scp4666_spawn_egg", 3);
        info(Registration, Scp4666Registry.Cocoa.get().getDefaultInstance(), "scp4666_hot_cocoa", 2);
        info(Registration, Scp4666Registry.Doll.get().getDefaultInstance(), "scp4666_flesh_doll", 2);
        info(Registration, Scp4666Registry.Necklace.get().getDefaultInstance(), "scp4666_tooth_necklace", 2);
        info(Registration, Scp4666Registry.Cane.get().getDefaultInstance(), "scp4666_bone_cane", 2);
        info(Registration, Scp4666Registry.FrozenGatewayItem.get().getDefaultInstance(), "scp4666_frozen_gateway", 1);
        info(Registration, Scp4666Registry.CellarDoorItem.get().getDefaultInstance(), "scp4666_cellar_door", 2);
        List<ItemStack> Furniture = Scp4666Registry.Furniture.stream().map(Entry -> Entry.get().getDefaultInstance()).toList();
        Registration.addItemStackInfo(Furniture, Component.translatable(Prefix + "scp4666_furniture.1"), Component.translatable(Prefix + "scp4666_furniture.2"));
    }
}
