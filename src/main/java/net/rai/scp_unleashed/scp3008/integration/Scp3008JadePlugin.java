package net.rai.scp_unleashed.scp3008.integration;

import net.rai.scp_unleashed.scp3008.block.Scp3008CounterBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008EntranceBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008ExitDoorBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp3008JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp3008DoorProvider.Instance, Scp3008EntranceBlock.class);
        Registration.registerBlockComponent(Scp3008DoorProvider.Instance, Scp3008ExitDoorBlock.class);
        Registration.registerBlockComponent(Scp3008CounterProvider.Instance, Scp3008CounterBlock.class);
    }
}
