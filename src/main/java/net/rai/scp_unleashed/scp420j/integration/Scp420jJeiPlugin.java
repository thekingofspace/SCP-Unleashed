package net.rai.scp_unleashed.scp420j.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp420j.Scp420jRegistry;

@JeiPlugin
public class Scp420jJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp420jRegistry.location("scp420j_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp420jRegistry.Joint.get()),
                Component.translatable("jei.scp_unleashed.info.scp420j_joint.1"),
                Component.translatable("jei.scp_unleashed.info.scp420j_joint.2"),
                Component.translatable("jei.scp_unleashed.info.scp420j_joint.3"));
        Registration.addItemStackInfo(new ItemStack(Scp420jRegistry.Seeds.get()),
                Component.translatable("jei.scp_unleashed.info.scp420j_seeds.1"),
                Component.translatable("jei.scp_unleashed.info.scp420j_seeds.2"));
        Registration.addItemStackInfo(new ItemStack(Scp420jRegistry.Leaf.get()),
                Component.translatable("jei.scp_unleashed.info.scp420j_leaf.1"));
    }
}
