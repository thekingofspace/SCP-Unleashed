package net.rai.scp_unleashed.scp428.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp428.Scp428Registry;
import software.bernie.geckolib.event.GeoRenderEvent;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp428ClientEvents {
    private Scp428ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp428Registry.Core.get(), Scp428CoreRenderer::new);
        Event.registerEntityRenderer(Scp428Registry.Instance.get(), Scp428InstanceRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CROSSHAIR, Scp428ClientHold.LayerId, Scp428ClientHold::render);
    }

    @SubscribeEvent
    public static void key(InputEvent.Key Event) {
        Scp428ClientHold.press(Event.getAction(), Minecraft.getInstance().options.keyShift.matches(Event.getKey(), Event.getScanCode()));
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Pre Event) {
        Scp428ClientHold.press(Event.getAction(), Minecraft.getInstance().options.keyShift.matchesMouse(Event.getButton()));
    }

    @SubscribeEvent
    public static void turn(CalculatePlayerTurnEvent Event) {
        Event.setMouseSensitivity(Scp428ClientHold.slowed(Event.getMouseSensitivity()));
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp428ClientHold.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void geckoRendered(GeoRenderEvent.Entity.Post Event) {
        Scp428Tendrils.capture(Event.getEntity(), Event.getModel());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp428ClientHold.reset();
        Scp428Tendrils.clear();
    }
}
