package net.rai.scp_unleashed.fluiddump.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fluiddump.DryPour;
import net.rai.scp_unleashed.fluiddump.FluidDumpBlockEntity;
import net.rai.scp_unleashed.fluiddump.FluidDumpRegistry;

@JeiPlugin
public class FluidDumpJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fluid_dump");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(FluidDumpRegistry.FluidDumpItem.get()),
                Component.translatable("jei.scp_unleashed.info.fluid_dump.1", DryPour.Reach, DryPour.Depth),
                Component.translatable("jei.scp_unleashed.info.fluid_dump.2", FluidDumpBlockEntity.FlowPerTick * 20),
                Component.translatable("jei.scp_unleashed.info.fluid_dump.3"));
    }
}
