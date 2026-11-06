package net.rai.scp_unleashed.creativeoutlet.integration;

import net.rai.scp_unleashed.creativeoutlet.CreativeOutletBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class CreativeOutletJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(CreativeOutletJadeProvider.Instance, CreativeOutletBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(CreativeOutletJadeProvider.Instance, CreativeOutletBlock.class);
    }
}
