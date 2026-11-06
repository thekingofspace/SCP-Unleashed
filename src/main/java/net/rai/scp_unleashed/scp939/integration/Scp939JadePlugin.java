package net.rai.scp_unleashed.scp939.integration;

import net.rai.scp_unleashed.scp939.Scp939;
import net.rai.scp_unleashed.scp939.compressor.GasCompressorBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp939JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(GasCompressorJadeProvider.Instance, GasCompressorBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp939JadeProvider.Instance, Scp939.class);
        Registration.registerBlockComponent(GasCompressorJadeProvider.Instance, GasCompressorBlock.class);
        Registration.addTooltipCollectedCallback(Scp939JadeTooltips::collected);
    }
}
