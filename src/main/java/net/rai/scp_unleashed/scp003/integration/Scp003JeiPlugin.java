package net.rai.scp_unleashed.scp003.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp003.Scp003BoardBlockEntity;
import net.rai.scp_unleashed.scp003.Scp003Crawler;
import net.rai.scp_unleashed.scp003.Scp003Host;
import net.rai.scp_unleashed.scp003.Scp003Registry;

@JeiPlugin
public class Scp003JeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp003");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(Scp003Registry.PlateItem.get()),
                Component.translatable("jei.scp_unleashed.scp003_plate"),
                Component.translatable("jei.scp_unleashed.scp003_plate.lift"));
        Registration.addItemStackInfo(new ItemStack(Scp003Registry.BoardItem.get()),
                Component.translatable("jei.scp_unleashed.scp003_board"),
                Component.translatable("jei.scp_unleashed.scp003_board.incubation", Scp003BoardBlockEntity.MinIncubation / 1200, Scp003BoardBlockEntity.MaxIncubation / 1200),
                Component.translatable("jei.scp_unleashed.scp003_board.stages", Scp003Crawler.KillsToGrow),
                Component.translatable("jei.scp_unleashed.scp003_board.ritual", Scp003Host.MaxBuilds),
                Component.translatable("jei.scp_unleashed.scp003_board.kill"));
    }
}
