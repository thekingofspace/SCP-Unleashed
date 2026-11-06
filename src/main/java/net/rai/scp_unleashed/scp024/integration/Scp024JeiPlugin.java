package net.rai.scp_unleashed.scp024.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp024.Scp024Registry;

@JeiPlugin
public class Scp024JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp024Registry.StageDoorItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp024_stage_door.1"),
                Component.translatable("jei.scp_unleashed.info.scp024_stage_door.2"),
                Component.translatable("jei.scp_unleashed.info.scp024_stage_door.3"));
        Registration.addItemStackInfo(new ItemStack(Scp024Registry.DollHairs.get()),
                Component.translatable("jei.scp_unleashed.info.scp024_doll_hairs.1"),
                Component.translatable("jei.scp_unleashed.info.scp024_doll_hairs.2"));
        Registration.addItemStackInfo(new ItemStack(Scp024Registry.Ribbon.get()),
                Component.translatable("jei.scp_unleashed.info.scp024_participation_ribbon.1"),
                Component.translatable("jei.scp_unleashed.info.scp024_participation_ribbon.2"));
    }
}
