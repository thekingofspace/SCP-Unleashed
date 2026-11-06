package net.rai.scp_unleashed.advmaterials.integration;

import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsBenchBlock;
import net.rai.scp_unleashed.advmaterials.MagnetizedBlock;
import net.rai.scp_unleashed.advmaterials.MagnetizedDoorBlock;
import net.rai.scp_unleashed.integration.jade.ScpPowerProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class AdvancedMaterialsJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(ScpPowerProvider.Instance, AdvancedMaterialsBenchBlock.class);
        Registration.registerBlockDataProvider(AdvancedMaterialsBenchProvider.Instance, AdvancedMaterialsBenchBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(AdvancedMaterialsBenchProvider.Instance, AdvancedMaterialsBenchBlock.class);
        Registration.registerBlockComponent(MagnetizedProvider.Instance, MagnetizedDoorBlock.class);
        Registration.registerBlockComponent(MagnetizedProvider.Instance, MagnetizedBlock.class);
    }
}
