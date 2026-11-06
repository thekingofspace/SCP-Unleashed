package net.rai.scp_unleashed.scp129.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp129.Scp129Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp129ClientEvents {
    public static final float FovSwim = 0.025f;
    public static final float FovRate = 0.3f;
    public static final double SensitivityScale = 0.6;
    public static final double SensitivityFloor = 0.2;

    private Scp129ClientEvents() {
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp129Registry.Spore.get(), Scp129Particles.SporeProvider::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(Scp129Haze.LayerId, Scp129Haze::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp129ClientState.tick(Client);
        Scp129Haze.tick(Client);
    }

    @SubscribeEvent
    public static void movementInput(MovementInputUpdateEvent Event) {
        Scp129ClientState.steer(Event.getEntity(), Event.getInput());
    }

    @SubscribeEvent
    public static void playerTurn(CalculatePlayerTurnEvent Event) {
        float Share = Scp129ClientState.aim(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        if (Share >= 1.0f) {
            return;
        }
        double Base = Event.getMouseSensitivity() * SensitivityScale + SensitivityFloor;
        Event.setMouseSensitivity((Base * Math.cbrt(Share) - SensitivityFloor) / SensitivityScale);
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov Event) {
        if (!Event.usedConfiguredFov()) {
            return;
        }
        float Partial = (float) Event.getPartialTick();
        float Shown = Scp129ClientState.blur(Partial);
        if (Shown <= 0.0f) {
            return;
        }
        double Scale = Minecraft.getInstance().options.fovEffectScale().get();
        Event.setFOV(Event.getFOV() * (1.0 + FovSwim * Mth.clamp(Shown, 0.0f, 1.5f) * Mth.sin((Scp129ClientState.clock() + Partial) * FovRate) * Scale));
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles Event) {
        float Dip = Scp129ClientState.jolt((float) Event.getPartialTick());
        if (Dip != 0.0f) {
            Event.setPitch(Event.getPitch() + Dip);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp129ClientState.reset();
        Scp129Haze.reset();
    }
}
