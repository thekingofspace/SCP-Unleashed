package net.rai.scp_unleashed.food.integration;

import net.rai.scp_unleashed.food.VendingMachineBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class FoodJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(VendingJadeProvider.Instance, VendingMachineBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(VendingJadeProvider.Instance, VendingMachineBlock.class);
    }
}
