package net.rai.scp_unleashed.scp3008.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;

@JeiPlugin
public class Scp3008JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp3008_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    private static void info(IRecipeRegistration Registration, ItemLike Item, String Key, int Lines) {
        Component[] Text = new Component[Lines];
        for (int Index = 0; Index < Lines; Index++) {
            Text[Index] = Component.translatable("jei.scp_unleashed.info." + Key + "." + (Index + 1));
        }
        Registration.addItemStackInfo(new ItemStack(Item), Text);
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        info(Registration, Scp3008Registry.Entrance.get(), "scp3008_entrance", 2);
        info(Registration, Scp3008Registry.Floor.get(), "scp3008_floor", 1);
        info(Registration, Scp3008Registry.Pillar.get(), "scp3008_pillar", 1);
        info(Registration, Scp3008Registry.Ceiling.get(), "scp3008_ceiling", 1);
        info(Registration, Scp3008Registry.ExitDoor.get(), "scp3008_exit_door", 1);
        info(Registration, Scp3008Registry.EmergencyLight.get(), "scp3008_emergency_light", 1);
        info(Registration, Scp3008Registry.Counter.get(), "scp3008_food_counter", 1);
        info(Registration, Scp3008Registry.Crate.get(), "scp3008_crate", 1);
        info(Registration, Scp3008Registry.Meatballs.get(), "scp3008_meatballs", 1);
        info(Registration, Scp3008Registry.DepartmentSign.get(), "scp3008_department_sign", 1);
    }
}
