package net.rai.scp_unleashed.scp076.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp076.Scp076Abel;
import net.rai.scp_unleashed.scp076.Scp076Registry;

@JeiPlugin
public class Scp076JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp076_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp076Registry.AbelEgg.get()),
                Component.translatable("jei.scp_unleashed.info.scp076_spawn_egg.1"),
                Component.translatable("jei.scp_unleashed.info.scp076_spawn_egg.2"),
                Component.translatable("jei.scp_unleashed.info.scp076_spawn_egg.3"),
                Component.translatable("jei.scp_unleashed.info.scp076_spawn_egg.4", (int) Scp076Abel.MaxHealth));
        Registration.addItemStackInfo(new ItemStack(Scp076Registry.VaultStoneItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp076_vault_stone.1"),
                Component.translatable("jei.scp_unleashed.info.scp076_vault_stone.2"));
        Registration.addItemStackInfo(new ItemStack(Scp076Registry.VaultDoorItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp076_vault_door.1"),
                Component.translatable("jei.scp_unleashed.info.scp076_vault_door.2"),
                Component.translatable("jei.scp_unleashed.info.scp076_vault_door.3"));
        Registration.addItemStackInfo(new ItemStack(Scp076Registry.CoffinItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp076_coffin.1"),
                Component.translatable("jei.scp_unleashed.info.scp076_coffin.2"));
    }
}
