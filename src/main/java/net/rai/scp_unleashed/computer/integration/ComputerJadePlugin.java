package net.rai.scp_unleashed.computer.integration;

import net.rai.scp_unleashed.computer.ComputerDeviceBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ComputerJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ComputerJadeProvider.Instance, ComputerDeviceBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(ComputerJadeProvider.Instance, ComputerDeviceBlock.class);
    }
}
