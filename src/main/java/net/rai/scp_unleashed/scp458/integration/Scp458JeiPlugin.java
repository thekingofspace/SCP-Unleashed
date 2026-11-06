package net.rai.scp_unleashed.scp458.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp458.Scp458Box;
import net.rai.scp_unleashed.scp458.Scp458Pizza;
import net.rai.scp_unleashed.scp458.Scp458Registry;
import net.rai.scp_unleashed.scp458.Scp458SliceItem;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@JeiPlugin
public class Scp458JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp458Registry.location("scp458_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration Registration) {
        Registration.registerSubtypeInterpreter(Scp458Registry.Slice.get(), new ISubtypeInterpreter<ItemStack>() {
            @Nullable
            @Override
            public Object getSubtypeData(ItemStack Stack, UidContext Context) {
                return Scp458SliceItem.pizza(Stack).id();
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack Stack, UidContext Context) {
                return Scp458SliceItem.pizza(Stack).id();
            }
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp458Registry.BoxItem.get()),
                Component.translatable(Prefix + "scp458_box.1"),
                Component.translatable(Prefix + "scp458_box.2"),
                Component.translatable(Prefix + "scp458_box.3"));
        List<ItemStack> Slices = Arrays.stream(Scp458Pizza.values()).map(Scp458Box::slice).toList();
        Registration.addItemStackInfo(Slices, Component.translatable(Prefix + "scp458_slice.1"));
    }
}
