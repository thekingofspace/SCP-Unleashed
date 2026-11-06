package net.rai.scp_unleashed.scp610.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Registry;

import java.util.List;

@JeiPlugin
public class Scp610JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        for (DeferredItem<DeferredSpawnEggItem> Egg : Scp610Registry.eggs()) {
            String Name = BuiltInRegistries.ITEM.getKey(Egg.get()).getPath();
            Scp610Form Form = Scp610Form.byId(Name.replace("scp610_", "").replace("_spawn_egg", ""));
            int Health = Form == null ? 0 : (int) Form.health();
            Registration.addItemStackInfo(Egg.get().getDefaultInstance(), Component.translatable(Prefix + Name + ".1", Health), Component.translatable(Prefix + "scp610_spawn_egg.shared"));
        }
        Registration.addItemStackInfo(List.of(new ItemStack(Scp610Registry.FleshItem.get()), new ItemStack(Scp610Registry.VeinedFleshItem.get()),
                new ItemStack(Scp610Registry.WoundItem.get()), new ItemStack(Scp610Registry.PustuleItem.get()), new ItemStack(Scp610Registry.SinewItem.get()),
                new ItemStack(Scp610Registry.TendrilsItem.get())), Component.translatable(Prefix + "scp610_flesh.1"));
        Registration.addItemStackInfo(new ItemStack(Scp610Registry.CommunicatorItem.get()), Component.translatable(Prefix + "pheromone_communicator.1"));
    }
}
