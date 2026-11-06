package net.rai.scp_unleashed.dclass.duty.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.duty.DClassDutyRegistry;

@JeiPlugin
public class DClassDutyJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dclass_duty");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(DClassDutyRegistry.Manager.get()),
                Component.translatable("jei.scp_unleashed.dclass_manager"),
                Component.translatable("jei.scp_unleashed.dclass_manager.roles"),
                Component.translatable("jei.scp_unleashed.dclass_manager.orders"));
    }
}
