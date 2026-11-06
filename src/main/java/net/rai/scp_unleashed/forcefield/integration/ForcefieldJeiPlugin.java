package net.rai.scp_unleashed.forcefield.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.forcefield.ForcefieldEmitterBlockEntity;
import net.rai.scp_unleashed.forcefield.ForcefieldRegistry;

@JeiPlugin
public class ForcefieldJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ForcefieldRegistry.location(ForcefieldRegistry.EmitterId + "_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info." + ForcefieldRegistry.EmitterId + ".";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(ForcefieldRegistry.EmitterItem.get()),
                Component.translatable(Prefix + "1", ForcefieldEmitterBlockEntity.MaxGap),
                Component.translatable(Prefix + "2", ForcefieldEmitterBlockEntity.MaxColumns),
                Component.translatable(Prefix + "3", ForcefieldEmitterBlockEntity.BaseDraw, ForcefieldEmitterBlockEntity.CellDraw),
                Component.translatable(Prefix + "4"),
                Component.translatable(Prefix + "5"));
    }
}
