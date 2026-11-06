package net.rai.scp_unleashed.scp005.integration;

import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp005JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp005JadeProvider.Instance, Block.class);
    }
}
