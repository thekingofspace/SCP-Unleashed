package net.rai.scp_unleashed.scp469.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp469.Scp469Angel;
import net.rai.scp_unleashed.scp469.Scp469Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp469ClientEvents {
    public static final double AimShare = 0.3;
    public static final double Reach = 6.0;
    private static boolean Held;

    private Scp469ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp469Registry.Angel.get(), Scp469Renderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp469Registry.FeatherParticle.get(), Scp469FeatherParticle.Provider::new);
    }

    public static boolean embraced(Player User) {
        for (Scp469Angel Creature : User.level().getEntitiesOfClass(Scp469Angel.class, User.getBoundingBox().inflate(Reach))) {
            if (Creature.isAlive() && Creature.embracedId() == User.getId()) {
                return true;
            }
        }
        return false;
    }

    public static double slowed(double Sensitivity) {
        if (!Held) {
            return Sensitivity;
        }
        double Speed = Sensitivity * 0.6 + 0.2;
        return (Speed * Math.cbrt(AimShare) - 0.2) / 0.6;
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        Held = Game.player != null && Game.level != null && Game.player.isAlive() && embraced(Game.player);
    }

    @SubscribeEvent
    public static void turn(CalculatePlayerTurnEvent Event) {
        Event.setMouseSensitivity(slowed(Event.getMouseSensitivity()));
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Held = false;
    }
}
