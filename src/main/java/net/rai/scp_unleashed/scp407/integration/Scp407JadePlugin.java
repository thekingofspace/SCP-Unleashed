package net.rai.scp_unleashed.scp407.integration;

import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp407.Scp407PodBlock;
import net.rai.scp_unleashed.scp407.Scp407Titan;
import net.rai.scp_unleashed.speaker.SpeakerBlock;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class Scp407JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration Registration) {
        Registration.registerBlockDataProvider(Scp407SpeakerProvider.Instance, SpeakerBlockEntity.class);
        Registration.registerEntityDataProvider(Scp407TitanProvider.Instance, Scp407Titan.class);
        Registration.registerEntityDataProvider(Scp407FusedProvider.Instance, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.registerBlockComponent(Scp407SpeakerProvider.Instance, SpeakerBlock.class);
        Registration.registerBlockComponent(Scp407PodProvider.Instance, Scp407PodBlock.class);
        Registration.registerEntityComponent(Scp407TitanProvider.Instance, Scp407Titan.class);
        Registration.registerEntityComponent(Scp407FusedProvider.Instance, LivingEntity.class);
    }
}
