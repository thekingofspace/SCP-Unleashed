package net.rai.scp_unleashed.scp087.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp087.Scp087Hunter;
import net.rai.scp_unleashed.scp087.Scp087Registry;

@JeiPlugin
public class Scp087JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp087_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp087Registry.DoorItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp087_door.1"),
                Component.translatable("jei.scp_unleashed.info.scp087_door.2"));
        Registration.addItemStackInfo(new ItemStack(Scp087Registry.ConcreteItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp087_concrete.1"));
        Registration.addItemStackInfo(new ItemStack(Scp087Registry.FloorItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp087_concrete.1"));
        Registration.addItemStackInfo(new ItemStack(Scp087Registry.StairsItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp087_stairs.1"));
        Registration.addItemStackInfo(new ItemStack(Scp087Registry.RailItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp087_rail.1"));
        Registration.addItemStackInfo(new ItemStack(Scp087Registry.HunterEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp087_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp087_spawn_egg.2"),
                Component.translatable("jei.scp_unleashed.info.scp087_spawn_egg.3", (int) Scp087Hunter.MaxHealth));
    }
}
