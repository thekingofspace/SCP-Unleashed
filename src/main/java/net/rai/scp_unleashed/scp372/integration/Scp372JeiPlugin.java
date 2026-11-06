package net.rai.scp_unleashed.scp372.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp372.Scp372AnchorBlockEntity;
import net.rai.scp_unleashed.scp372.Scp372Registry;

@JeiPlugin
public class Scp372JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp372Registry.location("scp372_jei_plugin");
    private static final String AnchorPrefix = "jei.scp_unleashed.info." + Scp372Registry.AnchorId + ".";
    private static final String EggPrefix = "jei.scp_unleashed.info." + Scp372Registry.EggId + ".";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp372Registry.AnchorItem.get()),
                Component.translatable(AnchorPrefix + "1"),
                Component.translatable(AnchorPrefix + "2", Scp372AnchorBlockEntity.PowerDraw),
                Component.translatable(AnchorPrefix + "3"));
        Registration.addItemStackInfo(Scp372Registry.InsectEgg.get().getDefaultInstance(),
                Component.translatable(EggPrefix + "1"),
                Component.translatable(EggPrefix + "2"),
                Component.translatable(EggPrefix + "3"));
    }
}
