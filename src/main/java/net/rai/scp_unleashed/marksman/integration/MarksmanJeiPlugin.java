package net.rai.scp_unleashed.marksman.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.marksman.CoinRicochet;
import net.rai.scp_unleashed.marksman.CoinToss;
import net.rai.scp_unleashed.marksman.MarksmanRevolverItem;
import net.rai.scp_unleashed.marksman.ability.ToolAbilityItem;

@JeiPlugin
public class MarksmanJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "marksman_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.marksman_revolver.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(MarksmanRevolverItem.loaded(),
                Component.translatable(Prefix + "1"),
                Component.translatable(Prefix + "2", Component.keybind(ToolAbilityItem.AbilityKey), CoinToss.MaxCoins, CoinToss.RechargeTicks / 20.0F),
                Component.translatable(Prefix + "3", CoinRicochet.BaseBonus));
    }
}
