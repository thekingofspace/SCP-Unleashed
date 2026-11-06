package net.rai.scp_unleashed.gas.integration;

import net.rai.scp_unleashed.block.CreativePressurizerBlock;
import net.rai.scp_unleashed.block.DuctBlock;
import net.rai.scp_unleashed.block.PressurizerBlock;
import net.rai.scp_unleashed.gas.GasTankBlock;
import net.rai.scp_unleashed.gas.VaporizerBlock;
import net.rai.scp_unleashed.plumbing.DrainBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class GasJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(GasTankJadeProvider.Instance, GasTankBlock.class);
        Registration.registerBlockDataProvider(VaporizerJadeProvider.Instance, VaporizerBlock.class);
        Registration.registerBlockDataProvider(DrainJadeProvider.Instance, DrainBlock.class);
        Registration.registerBlockDataProvider(PressureNoticeJadeProvider.Instance, DuctBlock.class);
        Registration.registerBlockDataProvider(PressureNoticeJadeProvider.Instance, PressurizerBlock.class);
        Registration.registerBlockDataProvider(PressureNoticeJadeProvider.Instance, CreativePressurizerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(GasTankJadeProvider.Instance, GasTankBlock.class);
        Registration.registerBlockComponent(VaporizerJadeProvider.Instance, VaporizerBlock.class);
        Registration.registerBlockComponent(DrainJadeProvider.Instance, DrainBlock.class);
        Registration.registerBlockComponent(PressureNoticeJadeProvider.Instance, DuctBlock.class);
        Registration.registerBlockComponent(PressureNoticeJadeProvider.Instance, PressurizerBlock.class);
        Registration.registerBlockComponent(PressureNoticeJadeProvider.Instance, CreativePressurizerBlock.class);
    }
}
