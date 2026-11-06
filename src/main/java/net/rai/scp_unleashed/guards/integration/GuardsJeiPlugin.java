package net.rai.scp_unleashed.guards.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.guards.GuardsRegistry;
import net.rai.scp_unleashed.guards.PatrolManagerBlockEntity;
import net.rai.scp_unleashed.guards.PatrolRoute;

@JeiPlugin
public class GuardsJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = GuardsRegistry.location("guards_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(GuardsRegistry.PatrolNodeItem.get()),
                Component.translatable("jei.scp_unleashed.info.patrol_node.1"),
                Component.translatable("jei.scp_unleashed.info.patrol_node.2"));
        Registration.addItemStackInfo(new ItemStack(GuardsRegistry.PatrolManagerItem.get()),
                Component.translatable("jei.scp_unleashed.info.patrol_manager.1", PatrolRoute.MaxPoints, (int) PatrolManagerBlockEntity.LinkRange),
                Component.translatable("jei.scp_unleashed.info.patrol_manager.2"),
                Component.translatable("jei.scp_unleashed.info.patrol_manager.3"));
        Registration.addItemStackInfo(new ItemStack(GuardsRegistry.PatrolLinker.get()),
                Component.translatable("jei.scp_unleashed.info.patrol_linker.1"),
                Component.translatable("jei.scp_unleashed.info.patrol_linker.2"));
        Registration.addItemStackInfo(new ItemStack(GuardsRegistry.PatrolOrders.get()),
                Component.translatable("jei.scp_unleashed.info.patrol_orders.1"),
                Component.translatable("jei.scp_unleashed.info.patrol_orders.2"),
                Component.translatable("jei.scp_unleashed.info.patrol_orders.3"));
    }
}
