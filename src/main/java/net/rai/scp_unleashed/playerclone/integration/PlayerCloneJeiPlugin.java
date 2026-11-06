package net.rai.scp_unleashed.playerclone.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.CloningMachineBlockEntity;
import net.rai.scp_unleashed.playerclone.PlayerCloneRegistry;

@JeiPlugin
public class PlayerCloneJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "player_clone");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(PlayerCloneRegistry.SamplingNeedle.get()),
                Component.translatable("jei.scp_unleashed.sampling_needle"),
                Component.translatable("jei.scp_unleashed.sampling_needle.self"),
                Component.translatable("jei.scp_unleashed.sampling_needle.limits"));
        Registration.addItemStackInfo(new ItemStack(PlayerCloneRegistry.BloodSample.get()),
                Component.translatable("jei.scp_unleashed.player_blood_sample"),
                Component.translatable("jei.scp_unleashed.player_blood_sample.cycle", CloningMachineBlockEntity.CycleTicks / 20),
                Component.translatable("jei.scp_unleashed.player_blood_sample.clone"));
    }
}
