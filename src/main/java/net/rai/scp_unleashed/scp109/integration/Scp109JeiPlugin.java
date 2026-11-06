package net.rai.scp_unleashed.scp109.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp109.Scp109Registry;
import net.rai.scp_unleashed.scp109.Scp109TapBlockEntity;

@JeiPlugin
public class Scp109JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = Scp109Registry.location("scp109_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp109Registry.Canteen.get()),
                Component.translatable("jei.scp_unleashed.info.scp109_canteen.1"),
                Component.translatable("jei.scp_unleashed.info.scp109_canteen.2"),
                Component.translatable("jei.scp_unleashed.info.scp109_canteen.3"));
        Registration.addItemStackInfo(new ItemStack(Scp109Registry.TapItem.get()),
                Component.translatable("jei.scp_unleashed.info.scp109_canteen_tap.1"),
                Component.translatable("jei.scp_unleashed.info.scp109_canteen_tap.2", Scp109TapBlockEntity.MaxPerSecond),
                Component.translatable("jei.scp_unleashed.info.scp109_canteen_tap.3"));
    }
}
