package net.rai.scp_unleashed.processing.integration;

import net.rai.scp_unleashed.processing.ProcessingMachineBlock;
import net.rai.scp_unleashed.processing.ProcessingMachineBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class ProcessingJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ProcessingJadeProvider.Instance, ProcessingMachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(ProcessingJadeProvider.Instance, ProcessingMachineBlock.class);
    }
}
