package net.rai.scp_unleashed.scp163.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp163.Scp163Castaway;
import net.rai.scp_unleashed.scp163.Scp163Registry;

import java.util.List;

@JeiPlugin
public class Scp163JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp163_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    private static void info(IRecipeRegistration Registration, ItemLike Thing, String Key, int Lines, Object... Arguments) {
        Component[] Text = new Component[Lines];
        for (int Index = 0; Index < Lines; Index++) {
            Text[Index] = Index == 0 ? Component.translatable(Prefix + Key + "." + (Index + 1), Arguments) : Component.translatable(Prefix + Key + "." + (Index + 1));
        }
        Registration.addItemStackInfo(new ItemStack(Thing), Text);
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        info(Registration, Scp163Registry.CastawayEgg.get(), "scp163_castaway_spawn_egg", 4, (int) Scp163Castaway.Health,
                (int) Scp163Castaway.Armour, (int) (Scp163Castaway.Armour + Scp163Castaway.SuitArmour));
        info(Registration, Scp163Registry.Suit.get(), "scp163_suit", 2);
        info(Registration, Scp163Registry.ApparatusItem.get(), "scp163_apparatus", 2);
        info(Registration, Scp163Registry.ConsoleItem.get(), "scp163_console", 1);
        info(Registration, Scp163Registry.SignallerItem.get(), "scp163_signaller", 1);
        for (ItemLike Piece : List.<ItemLike>of(Scp163Registry.HullItem.get(), Scp163Registry.HullSeamItem.get(), Scp163Registry.HullLightItem.get(),
                Scp163Registry.DeckItem.get(), Scp163Registry.HatchItem.get())) {
            Registration.addItemStackInfo(new ItemStack(Piece), Component.translatable(Prefix + "scp163_hull.1"));
        }
    }
}
