package net.rai.scp_unleashed.mtf.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.mtf.MtfHelicopter;
import net.rai.scp_unleashed.mtf.MtfRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class MtfClientEvents {
    public static final float RotorVolume = 1.0f;
    public static final float TurbineVolume = 0.55f;
    public static final float RumbleVolume = 0.8f;

    private MtfClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(MtfRegistry.OperativeType.get(), MtfOperativeRenderer::new);
        Event.registerEntityRenderer(MtfRegistry.HelicopterType.get(), MtfHelicopterRenderer::new);
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (!Event.getLevel().isClientSide() || !(Event.getEntity() instanceof MtfHelicopter Ride) || Ride.heard()) {
            return;
        }
        Ride.markHeard();
        Minecraft Game = Minecraft.getInstance();
        Game.getSoundManager().play(new MtfHelicopterSound(Ride, MtfRegistry.Rotor.get(), RotorVolume, 1.0f, true));
        Game.getSoundManager().play(new MtfHelicopterSound(Ride, MtfRegistry.Turbine.get(), TurbineVolume, 1.0f, false));
        Game.getSoundManager().play(new MtfHelicopterSound(Ride, MtfRegistry.Rumble.get(), RumbleVolume, 1.0f, false));
    }
}
