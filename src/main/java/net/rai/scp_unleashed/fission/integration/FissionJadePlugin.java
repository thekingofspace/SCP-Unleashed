package net.rai.scp_unleashed.fission.integration;

import net.rai.scp_unleashed.fission.FissionReactorBlock;
import net.rai.scp_unleashed.fission.GasDissolverBlock;
import net.rai.scp_unleashed.fission.HeatExchangerBlock;
import net.rai.scp_unleashed.fission.SonicVibratorBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class FissionJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(FissionReactorJadeProvider.Instance, FissionReactorBlock.class);
        Registration.registerBlockDataProvider(HeatExchangerJadeProvider.Instance, HeatExchangerBlock.class);
        Registration.registerBlockDataProvider(SonicVibratorJadeProvider.Instance, SonicVibratorBlock.class);
        Registration.registerBlockDataProvider(GasDissolverJadeProvider.Instance, GasDissolverBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(FissionReactorJadeProvider.Instance, FissionReactorBlock.class);
        Registration.registerBlockComponent(HeatExchangerJadeProvider.Instance, HeatExchangerBlock.class);
        Registration.registerBlockComponent(SonicVibratorJadeProvider.Instance, SonicVibratorBlock.class);
        Registration.registerBlockComponent(GasDissolverJadeProvider.Instance, GasDissolverBlock.class);
    }
}
