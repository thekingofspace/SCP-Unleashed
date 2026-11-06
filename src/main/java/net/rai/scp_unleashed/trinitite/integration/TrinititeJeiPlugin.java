package net.rai.scp_unleashed.trinitite.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.entity.ChargeDisperserBlockEntity;
import net.rai.scp_unleashed.trinitite.TrinititeRegistry;
import net.rai.scp_unleashed.wire.ChargingRedstone;

import java.util.List;

@JeiPlugin
public class TrinititeJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "trinitite_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(List.of(new ItemStack(TrinititeRegistry.TrinititeOreItem.get()), new ItemStack(TrinititeRegistry.DeepslateTrinititeOreItem.get())),
                Component.translatable("jei.scp_unleashed.info.trinitite_ore.1"),
                Component.translatable("jei.scp_unleashed.info.trinitite_ore.2"));
        Registration.addItemStackInfo(new ItemStack(TrinititeRegistry.TrinititeShard.get()),
                Component.translatable("jei.scp_unleashed.info.trinitite_shard"));
        Registration.addItemStackInfo(new ItemStack(TrinititeRegistry.TrinititeItem.get()),
                Component.translatable("jei.scp_unleashed.info.trinitite_block.1", TrinititeRegistry.ChargeCost, ChargingRedstone.ChargeRate),
                Component.translatable("jei.scp_unleashed.info.trinitite_block.2", ChargeDisperserBlockEntity.PowerDraw));
        Registration.addItemStackInfo(new ItemStack(TrinititeRegistry.ChargedTrinititeItem.get()),
                Component.translatable("jei.scp_unleashed.info.charged_trinitite_block.1"),
                Component.translatable("jei.scp_unleashed.info.charged_trinitite_block.2"));
    }
}
