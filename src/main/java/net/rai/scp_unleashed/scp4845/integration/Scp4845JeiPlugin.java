package net.rai.scp_unleashed.scp4845.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;
import net.rai.scp_unleashed.scp4845.item.Scp4845MenuItem;

import java.util.Arrays;
import java.util.List;

@JeiPlugin
public class Scp4845JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp4845Registry.location("scp4845_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        List<ItemStack> Foods = Arrays.stream(Scp4845MenuItem.values()).map(Entry -> new ItemStack(Scp4845Registry.food(Entry))).toList();
        Registration.addItemStackInfo(Foods, Component.translatable(Prefix + "scp4845_food.1"), Component.translatable(Prefix + "scp4845_food.2"));
        Registration.addItemStackInfo(List.of(new ItemStack(Scp4845Registry.Register.get()), new ItemStack(Scp4845Registry.Counter.get())),
                Component.translatable(Prefix + "scp4845_register.1"), Component.translatable(Prefix + "scp4845_register.2"));
        Registration.addItemStackInfo(new ItemStack(Scp4845Registry.Egg.get()), Component.translatable(Prefix + "scp4845_employee_spawn_egg.1"),
                Component.translatable(Prefix + "scp4845_employee_spawn_egg.2"));
        List<ItemStack> Fixtures = Scp4845Registry.Placeables.stream().map(Entry -> new ItemStack(Entry.get()))
                .filter(Stack -> !Stack.is(Scp4845Registry.Register.get().asItem()) && !Stack.is(Scp4845Registry.Counter.get().asItem())).toList();
        Registration.addItemStackInfo(Fixtures, Component.translatable(Prefix + "scp4845_fixture.1"));
    }
}
