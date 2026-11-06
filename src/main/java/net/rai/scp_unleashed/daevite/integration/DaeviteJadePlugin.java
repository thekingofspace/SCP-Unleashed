package net.rai.scp_unleashed.daevite.integration;

import net.rai.scp_unleashed.daevite.block.DaeviteAltarBlock;
import net.rai.scp_unleashed.daevite.entity.DaeviteMob;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class DaeviteJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerEntityDataProvider(DaeviteMemberProvider.Instance, DaeviteMob.class);
        Registration.registerBlockDataProvider(DaeviteAltarProvider.Instance, DaeviteAltarBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(DaeviteMemberProvider.Instance, DaeviteMob.class);
        Registration.registerBlockComponent(DaeviteAltarProvider.Instance, DaeviteAltarBlock.class);
    }
}
