package net.rai.scp_unleashed.scp149.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp149.Scp149Fly;
import net.rai.scp_unleashed.scp149.Scp149Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp149ClientEvents {
    public static final float WhineWeight = 1.0f;
    public static final float DroneWeight = 0.45f;
    public static final float DroneTilt = 0.9f;

    private Scp149ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp149Registry.Fly.get(), Scp149FlyRenderer::new);
        Event.registerEntityRenderer(Scp149Registry.Larva.get(), Scp149LarvaRenderer::new);
        Event.registerBlockEntityRenderer(Scp149Registry.CocoonEntity.get(), Scp149CocoonRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp149Registry.Mote.get(), Scp149Particles.MoteProvider::new);
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent Event) {
        if (Event.getLevel().isClientSide && Event.getEntity() instanceof Scp149Fly Fly) {
            Minecraft.getInstance().getSoundManager().queueTickingSound(new Scp149Whine(Fly, Scp149Registry.Whine.get(), WhineWeight, 1.0f));
            Minecraft.getInstance().getSoundManager().queueTickingSound(new Scp149Whine(Fly, Scp149Registry.WhineDrone.get(), DroneWeight, DroneTilt));
        }
    }
}
