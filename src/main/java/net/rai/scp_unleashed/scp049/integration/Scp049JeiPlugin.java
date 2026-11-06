package net.rai.scp_unleashed.scp049.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp049.Scp049Cured;
import net.rai.scp_unleashed.scp049.Scp049Doctor;
import net.rai.scp_unleashed.scp049.Scp049Registry;

@JeiPlugin
public class Scp049JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp049Registry.DoctorEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp049_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp049_spawn_egg.2", (int) Scp049Doctor.AreaRadius),
                Component.translatable("jei.scp_unleashed.info.scp049_spawn_egg.3", (int) Scp049Doctor.SightRange),
                Component.translatable("jei.scp_unleashed.info.scp049_spawn_egg.4", (int) Scp049Doctor.RescueThreshold, Scp049Doctor.RescueWindow / 20),
                Component.translatable("jei.scp_unleashed.info.scp049_spawn_egg.5", (int) Scp049Doctor.MaxHealth));
        Registration.addItemStackInfo(new ItemStack(Scp049Registry.CuredEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp049_2_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp049_2_spawn_egg.2", (int) Scp049Cured.Health, (int) Scp049Cured.Damage),
                Component.translatable("jei.scp_unleashed.info.scp049_2_spawn_egg.3"));
        Registration.addItemStackInfo(new ItemStack(Scp049Registry.TrayItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp049_surgery_tray.1"),
                Component.translatable("jei.scp_unleashed.info.scp049_surgery_tray.2"),
                Component.translatable("jei.scp_unleashed.info.scp049_surgery_tray.3"));
    }
}
