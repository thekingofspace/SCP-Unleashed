package net.rai.scp_unleashed.scp207.integration;

import net.rai.scp_unleashed.scp207.Scp207BottlesBlock;
import net.rai.scp_unleashed.scp207.Scp207CrateBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp207JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp207CrateJadeProvider.Instance, Scp207CrateBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp207CrateJadeProvider.Instance, Scp207CrateBlock.class);
        Registration.registerBlockComponent(Scp207BottlesJadeProvider.Instance, Scp207BottlesBlock.class);
    }
}
