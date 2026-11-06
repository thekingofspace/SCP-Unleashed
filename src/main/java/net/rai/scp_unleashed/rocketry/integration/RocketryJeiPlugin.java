package net.rai.scp_unleashed.rocketry.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformKind;

@JeiPlugin
public class RocketryJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "rocketry_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        LaunchPlatformKind Rocket = LaunchPlatformKind.Rocket;
        LaunchPlatformKind Satellite = LaunchPlatformKind.Satellite;
        Registration.addItemStackInfo(new ItemStack(RocketryRegistry.RocketPlatformItem.get()),
                Component.translatable("jei.scp_unleashed.info.rocket_platform.1"),
                Component.translatable("jei.scp_unleashed.info.rocket_platform.2", Rocket.getFuelCost(), Rocket.getPowerCost()),
                Component.translatable("jei.scp_unleashed.info.rocket_platform.3"));
        Registration.addItemStackInfo(new ItemStack(RocketryRegistry.Rocket.get()),
                Component.translatable("jei.scp_unleashed.info.rocket.1"),
                Component.translatable("jei.scp_unleashed.info.rocket.2"),
                Component.translatable("jei.scp_unleashed.info.rocket.3"),
                Component.translatable("jei.scp_unleashed.info.rocket.4"));
        Registration.addItemStackInfo(new ItemStack(RocketryRegistry.PortableLaunchPadItem.get()),
                Component.translatable("jei.scp_unleashed.info.portable_launch_pad.1"),
                Component.translatable("jei.scp_unleashed.info.portable_launch_pad.2"));
        Registration.addItemStackInfo(new ItemStack(RocketryRegistry.SatellitePlatformItem.get()),
                Component.translatable("jei.scp_unleashed.info.satellite_platform.1"),
                Component.translatable("jei.scp_unleashed.info.satellite_platform.2", Satellite.getFuelCost(), Satellite.getPowerCost()));
        Registration.addItemStackInfo(new ItemStack(RocketryRegistry.Satellite.get()),
                Component.translatable("jei.scp_unleashed.info.satellite.1"),
                Component.translatable("jei.scp_unleashed.info.satellite.2"));
        Registration.addItemStackInfo(new ItemStack(RocketryRegistry.RocketEngine.get()),
                Component.translatable("jei.scp_unleashed.info.rocket_engine"));
    }
}
