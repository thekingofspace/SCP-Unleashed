package net.rai.scp_unleashed.scp939.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import net.rai.scp_unleashed.scp939.compressor.GasCompressorBlockEntity;

@JeiPlugin
public class Scp939JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp939Registry.location("scp939_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp939Registry.Egg.get()),
                Component.translatable(Prefix + "scp939_spawn_egg.1"),
                Component.translatable(Prefix + "scp939_spawn_egg.2"),
                Component.translatable(Prefix + "scp939_spawn_egg.3"),
                Component.translatable(Prefix + "scp939_spawn_egg.4"),
                Component.translatable(Prefix + "scp939_spawn_egg.5"));
        Registration.addItemStackInfo(new ItemStack(Scp939Registry.CompressorItem.get()),
                Component.translatable(Prefix + "gas_compressor.1"),
                Component.translatable(Prefix + "gas_compressor.2", GasCompressorBlockEntity.WorkDraw),
                Component.translatable(Prefix + "gas_compressor.3"),
                Component.translatable(Prefix + "gas_compressor.4"));
        Registration.addItemStackInfo(new ItemStack(Scp939Registry.Canister.get()),
                Component.translatable(Prefix + "amnestic_canister.1"),
                Component.translatable(Prefix + "amnestic_canister.2"),
                Component.translatable(Prefix + "amnestic_canister.3"));
        Registration.addItemStackInfo(new ItemStack(Scp939Registry.EmptyCanister.get()),
                Component.translatable(Prefix + "empty_canister.1"));
    }
}
