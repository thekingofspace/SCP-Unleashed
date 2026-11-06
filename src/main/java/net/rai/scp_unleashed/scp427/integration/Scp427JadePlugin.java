package net.rai.scp_unleashed.scp427.integration;

import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp427.Scp427Flesh;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp427JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerEntityComponent(Scp427LocketJadeProvider.Instance, LivingEntity.class);
        Registration.registerEntityComponent(Scp427FleshJadeProvider.Instance, Scp427Flesh.class);
    }
}
