package net.rai.scp_unleashed.semanticblocker.integration;

import net.rai.scp_unleashed.semanticblocker.SemanticBlockerBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class SemanticBlockerJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(SemanticBlockerJadeProvider.Instance, SemanticBlockerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(SemanticBlockerJadeProvider.Instance, SemanticBlockerBlock.class);
    }
}
