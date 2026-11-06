package net.rai.scp_unleashed.assembler.integration;

import net.rai.scp_unleashed.assembler.AdvancedAssemblerBlock;
import net.rai.scp_unleashed.integration.jade.ScpPowerProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class AssemblerJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, AdvancedAssemblerBlock.class);
        Registration.registerBlockDataProvider(AssemblerJadeProvider.Instance, AdvancedAssemblerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(AssemblerJadeProvider.Instance, AdvancedAssemblerBlock.class);
    }
}
