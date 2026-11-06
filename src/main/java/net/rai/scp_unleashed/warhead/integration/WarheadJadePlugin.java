package net.rai.scp_unleashed.warhead.integration;

import net.rai.scp_unleashed.warhead.AlphaWarheadBlock;
import net.rai.scp_unleashed.warhead.WarheadPanelBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class WarheadJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(WarheadJadeProvider.Alpha, AlphaWarheadBlock.class);
        Registration.registerBlockDataProvider(WarheadJadeProvider.Panel, WarheadPanelBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(WarheadJadeProvider.Alpha, AlphaWarheadBlock.class);
        Registration.registerBlockComponent(WarheadJadeProvider.Panel, WarheadPanelBlock.class);
        Registration.addBeforeRenderCallback(WarheadJadeNudge.Instance);
    }
}
