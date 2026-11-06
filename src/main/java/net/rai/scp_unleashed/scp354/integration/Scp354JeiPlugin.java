package net.rai.scp_unleashed.scp354.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.rai.scp_unleashed.scp354.Scp354Registry;

@JeiPlugin
public class Scp354JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp354Registry.location("scp354_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        info(Registration, Scp354Registry.TentacleEgg.get(), "scp354_tentacle_spawn_egg", 3);
        info(Registration, Scp354Registry.LionEgg.get(), "scp354_lion_spawn_egg", 3);
        info(Registration, Scp354Registry.LizardEgg.get(), "scp354_lizard_spawn_egg", 3);
        info(Registration, Scp354Registry.StainedEarth.get().asItem(), "scp354_stained_earth", 1);
        info(Registration, Scp354Registry.StainedStone.get().asItem(), "scp354_stained_stone", 1);
        info(Registration, Scp354Registry.DeadGrass.get().asItem(), "scp354_dead_grass", 1);
    }

    private static void info(IRecipeRegistration Registration, Item Shown, String Key, int Lines) {
        Component[] Text = new Component[Lines];
        for (int Index = 0; Index < Lines; Index++) {
            Text[Index] = Component.translatable(Prefix + Key + "." + (Index + 1));
        }
        Registration.addItemStackInfo(Shown.getDefaultInstance(), Text);
    }
}
