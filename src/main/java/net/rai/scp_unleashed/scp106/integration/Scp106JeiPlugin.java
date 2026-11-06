package net.rai.scp_unleashed.scp106.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp106.Scp106OldMan;
import net.rai.scp_unleashed.scp106.Scp106Registry;

@JeiPlugin
public class Scp106JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp106_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp106Registry.OldManEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp106_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp106_spawn_egg.2"),
                Component.translatable("jei.scp_unleashed.info.scp106_spawn_egg.3"),
                Component.translatable("jei.scp_unleashed.info.scp106_spawn_egg.4", (int) Scp106OldMan.MaxHealth));
        Registration.addItemStackInfo(new ItemStack(Scp106Registry.FemurBreakerItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp106_femur_breaker.1"),
                Component.translatable("jei.scp_unleashed.info.scp106_femur_breaker.2"));
        Registration.addItemStackInfo(new ItemStack(Scp106Registry.DoorwayItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp106_doorway.1"));
        for (ItemStack Stack : new ItemStack[]{new ItemStack(Scp106Registry.PocketStoneItem.get()), new ItemStack(Scp106Registry.CorrodedStoneItem.get()),
                new ItemStack(Scp106Registry.PocketTilesItem.get()), new ItemStack(Scp106Registry.PocketSeepItem.get())}) {
            Registration.addItemStackInfo(Stack, Component.translatable("jei.scp_unleashed.info.scp106_pocket_stone.1"));
        }
    }
}
