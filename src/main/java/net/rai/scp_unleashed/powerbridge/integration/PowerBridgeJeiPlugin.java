package net.rai.scp_unleashed.powerbridge.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.powerbridge.PowerBridgeBlockEntity;
import net.rai.scp_unleashed.powerbridge.PowerBridgeConversion;
import net.rai.scp_unleashed.powerbridge.PowerBridgeKind;
import net.rai.scp_unleashed.powerbridge.PowerBridgeRegistry;

@JeiPlugin
public class PowerBridgeJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = PowerBridgeRegistry.location(PowerBridgeRegistry.BridgeId + "_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info." + PowerBridgeRegistry.BridgeId + ".";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        info(Registration, PowerBridgeRegistry.ReceiverItem.get(), PowerBridgeRegistry.ReceiverKind);
        info(Registration, PowerBridgeRegistry.EmitterItem.get(), PowerBridgeRegistry.EmitterKind);
    }

    private static void info(IRecipeRegistration Registration, Item Bridge, PowerBridgeKind Kind) {
        String Side = Prefix + Kind.getSerializedName() + ".";
        Registration.addItemStackInfo(new ItemStack(Bridge),
                Component.translatable(Side + "1"),
                Component.translatable(Side + "2"),
                Component.translatable(Side + "3", PowerBridgeConversion.Sample, PowerBridgeConversion.sampleOut(Kind)),
                Component.translatable(Prefix + "4"),
                Component.translatable(Prefix + "5", String.format("%,d", PowerBridgeBlockEntity.BufferCapacity)));
    }
}
