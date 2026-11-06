package net.rai.scp_unleashed.scp066.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp066.Scp066Entity;
import net.rai.scp_unleashed.scp066.Scp066Registry;

@JeiPlugin
public class Scp066JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp066_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp066Registry.BallItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp066_ball.1"),
                Component.translatable("jei.scp_unleashed.info.scp066_ball.2", Scp066Entity.TreatCooldown / 20),
                Component.translatable("jei.scp_unleashed.info.scp066_ball.3"),
                Component.translatable("jei.scp_unleashed.info.scp066_ball.4", (int) Scp066Entity.Damage),
                Component.translatable("jei.scp_unleashed.info.scp066_ball.5"));
        Registration.addItemStackInfo(new ItemStack(Scp066Registry.SpawnEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp066_spawn_egg.1", (int) Scp066Entity.Health),
                Component.translatable("jei.scp_unleashed.info.scp066_spawn_egg.2"));
        Registration.addItemStackInfo(new ItemStack(Scp066Registry.Candy.get()),
                Component.translatable("jei.scp_unleashed.info.scp066_candy.1", Scp066Registry.CandyNutrition));
        Registration.addItemStackInfo(new ItemStack(Scp066Registry.Cupcake.get()),
                Component.translatable("jei.scp_unleashed.info.scp066_cupcake.1", Scp066Registry.CupcakeNutrition, Scp066Registry.CupcakeCheerTicks / 20));
    }
}
