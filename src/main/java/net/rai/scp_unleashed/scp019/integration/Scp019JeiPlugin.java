package net.rai.scp_unleashed.scp019.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp019.Scp019Brute;
import net.rai.scp_unleashed.scp019.Scp019Drone;
import net.rai.scp_unleashed.scp019.Scp019PotBlockEntity;
import net.rai.scp_unleashed.scp019.Scp019Registry;

@JeiPlugin
public class Scp019JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp019_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp019Registry.PotItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp019_pot.1"),
                Component.translatable("jei.scp_unleashed.info.scp019_pot.2", Scp019PotBlockEntity.MaxBrutes, Scp019PotBlockEntity.MaxDrones),
                Component.translatable("jei.scp_unleashed.info.scp019_pot.3"),
                Component.translatable("jei.scp_unleashed.info.scp019_pot.4"));
        Registration.addItemStackInfo(new ItemStack(Scp019Registry.DroneEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp019_drone_spawn_egg.1", (int) Scp019Drone.SightRange, Scp019PotBlockEntity.SearchRadius),
                Component.translatable("jei.scp_unleashed.info.scp019_drone_spawn_egg.2"));
        Registration.addItemStackInfo(new ItemStack(Scp019Registry.BruteEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp019_brute_spawn_egg.1", (int) Scp019Brute.Health, (int) Scp019Brute.Damage),
                Component.translatable("jei.scp_unleashed.info.scp019_brute_spawn_egg.2", Scp019PotBlockEntity.BruteLeash, Scp019PotBlockEntity.BruteReformTicks / 20));
    }
}
