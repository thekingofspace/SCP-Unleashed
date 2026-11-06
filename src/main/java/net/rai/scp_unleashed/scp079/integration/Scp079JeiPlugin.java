package net.rai.scp_unleashed.scp079.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp079.Scp079BlockEntity;
import net.rai.scp_unleashed.scp079.Scp079Registry;

@JeiPlugin
public class Scp079JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp079_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp079Registry.ComputerItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp079_computer.1"),
                Component.translatable("jei.scp_unleashed.info.scp079_computer.2", Scp079BlockEntity.BaseDraw),
                Component.translatable("jei.scp_unleashed.info.scp079_computer.3"),
                Component.translatable("jei.scp_unleashed.info.scp079_computer.4"));
        Registration.addItemStackInfo(new ItemStack(Scp079Registry.AnomalousUpgrade.get()),
                Component.translatable("jei.scp_unleashed.info.scp079_anomalous_upgrade.1"),
                Component.translatable("jei.scp_unleashed.info.scp079_anomalous_upgrade.2"),
                Component.translatable("jei.scp_unleashed.info.scp079_anomalous_upgrade.3", Scp079BlockEntity.UpgradedDraw));
    }
}
