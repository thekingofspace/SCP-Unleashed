package net.rai.scp_unleashed.scp513.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp513.Scp513Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp513ClientEvents {
    public static final double SensitivityScale = 0.6;
    public static final double SensitivityFloor = 0.2;

    private Scp513ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp513Display::register);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp513Registry.Stalker.get(), Scp513StalkerRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(Scp513Dread.LayerId, Scp513Dread::render);
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp513Dread.MiasmaLayerId, Scp513Dread::renderMiasma);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp513ClientState.tick(Client);
        Scp513Dread.tick(Client);
        Scp513Visions.tick(Client);
    }

    @SubscribeEvent
    public static void movementInput(MovementInputUpdateEvent Event) {
        Scp513ClientState.steer(Event.getEntity(), Event.getInput());
    }

    @SubscribeEvent
    public static void playerTurn(CalculatePlayerTurnEvent Event) {
        float Share = Scp513ClientState.aim(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        if (Share >= 1.0f) {
            return;
        }
        double Base = Event.getMouseSensitivity() * SensitivityScale + SensitivityFloor;
        Event.setMouseSensitivity((Base * Math.cbrt(Share) - SensitivityFloor) / SensitivityScale);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp513ClientState.reset();
        Scp513Dread.reset();
        Scp513Visions.reset();
    }
}
