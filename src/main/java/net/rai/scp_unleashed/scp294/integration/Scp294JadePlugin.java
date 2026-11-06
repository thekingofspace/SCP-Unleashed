package net.rai.scp_unleashed.scp294.integration;

import net.rai.scp_unleashed.scp294.Scp294MachineBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp294JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp294MachineJadeProvider.Instance, Scp294MachineBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp294MachineJadeProvider.Instance, Scp294MachineBlock.class);
    }
}
