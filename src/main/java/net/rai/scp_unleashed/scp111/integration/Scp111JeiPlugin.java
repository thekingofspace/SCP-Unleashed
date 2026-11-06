package net.rai.scp_unleashed.scp111.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp111.Scp111Clutch;
import net.rai.scp_unleashed.scp111.Scp111Egg;
import net.rai.scp_unleashed.scp111.Scp111Registry;
import net.rai.scp_unleashed.scp111.Scp111Shelf;
import net.rai.scp_unleashed.scp111.Scp111Snail;
import net.rai.scp_unleashed.scp111.Scp111Variant;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class Scp111JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp111_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration Registration) {
        Registration.registerSubtypeInterpreter(Scp111Registry.EggItem.get(), new ISubtypeInterpreter<ItemStack>() {
            @Nullable
            @Override
            public Object getSubtypeData(ItemStack Stack, UidContext Context) {
                return Scp111Egg.of(Stack).Variant().getSerializedName();
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack Stack, UidContext Context) {
                return Scp111Egg.of(Stack).Variant().getSerializedName();
            }
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        List<ItemStack> Eggs = new ArrayList<>();
        for (Scp111Variant Variant : Scp111Variant.values()) {
            Eggs.add(new Scp111Egg(Variant, 0).stack(1));
        }
        Registration.addItemStackInfo(Eggs,
                Component.translatable(Prefix + "scp111_egg.1", Scp111Variant.values().length),
                Component.translatable(Prefix + "scp111_egg.2"),
                Component.translatable(Prefix + "scp111_egg.3"),
                Component.translatable(Prefix + "scp111_egg.4", Scp111Shelf.SmallCapacity, Scp111Shelf.TallCapacity));
        Registration.addItemStackInfo(Scp111Registry.BoxItem.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp111_box.1", Scp111Clutch.boxed().size()),
                Component.translatable(Prefix + "scp111_box.2"),
                Component.translatable(Prefix + "scp111_box.3"));
        Registration.addItemStackInfo(Scp111Registry.SnailEgg.get().getDefaultInstance(),
                Component.translatable(Prefix + "scp111_snail_spawn_egg.1", (int) Scp111Snail.Health),
                Component.translatable(Prefix + "scp111_snail_spawn_egg.2"),
                Component.translatable(Prefix + "scp111_snail_spawn_egg.3"),
                Component.translatable(Prefix + "scp111_snail_spawn_egg.4"));
    }
}
