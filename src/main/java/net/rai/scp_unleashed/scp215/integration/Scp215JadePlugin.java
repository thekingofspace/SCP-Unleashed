package net.rai.scp_unleashed.scp215.integration;

import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.theodore.Theodore;
import net.rai.scp_unleashed.mtf.MtfOperative;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp215JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp215JadeProvider.Instance, Player.class);
        Registration.registerEntityComponent(Scp215JadeProvider.Instance, DClass.class);
        Registration.registerEntityComponent(Scp215JadeProvider.Instance, Theodore.class);
        Registration.registerEntityComponent(Scp215JadeProvider.Instance, MtfOperative.class);
    }
}
