package net.rai.scp_unleashed.scp001day.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.gateway.GatewayRegistry;
import net.rai.scp_unleashed.scp001day.Scp001DayDestination;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;

@JeiPlugin
public class Scp001DayJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp001DayRegistry.location("scp001_day_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp001DayRegistry.Tin.get()),
                Component.translatable(Prefix + "scp001_day_tin.1"),
                Component.translatable(Prefix + "scp001_day_tin.2"));
        Component[] Wrap = {Component.translatable(Prefix + "scp001_day_sun_wrap.1"), Component.translatable(Prefix + "scp001_day_sun_wrap.2"),
                Component.translatable(Prefix + "scp001_day_sun_wrap.3")};
        Registration.addItemStackInfo(new ItemStack(Scp001DayRegistry.SunHat.get()), Wrap);
        Registration.addItemStackInfo(new ItemStack(Scp001DayRegistry.SunWraps.get()), Wrap);
        Registration.addItemStackInfo(new ItemStack(Scp001DayRegistry.SunLegWraps.get()), Wrap);
        Registration.addItemStackInfo(new ItemStack(Scp001DayRegistry.SunBoots.get()), Wrap);
        Registration.addItemStackInfo(GatewayRegistry.shardFor(Scp001DayDestination.Id),
                Component.translatable(Prefix + "scp001_day_shard.1"),
                Component.translatable(Prefix + "scp001_day_shard.2"));
    }
}
