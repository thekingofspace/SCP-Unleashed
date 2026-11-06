package net.rai.scp_unleashed.scp018.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp018.Scp018Registry;

@JeiPlugin
public class Scp018JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp018_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp018Registry.BallItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp018_ball.1"),
                Component.translatable("jei.scp_unleashed.info.scp018_ball.2"),
                Component.translatable("jei.scp_unleashed.info.scp018_ball.3"),
                Component.translatable("jei.scp_unleashed.info.scp018_ball.4"));
    }
}
