package net.rai.scp_unleashed.scp215.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp215.Scp215Phantom;
import net.rai.scp_unleashed.scp215.Scp215Registry;
import software.bernie.geckolib.event.GeoRenderEvent;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp215ClientEvents {
    private Scp215ClientEvents() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent Event) {
        Scp215Phantom.ClientViewer = () -> Minecraft.getInstance().player;
        Event.enqueueWork(Scp215Display::register);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp215Registry.Phantom.get(), Scp215PhantomRenderer::new);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        Scp215Visions.tick(Game);
        Scp215Disguises.tick(Game);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderPlayer(RenderPlayerEvent.Pre Event) {
        if (Scp215Disguises.render(Event.getEntity(), Event.getPartialTick(), Event.getPoseStack(), Event.getMultiBufferSource(), Event.getPackedLight())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderGeo(GeoRenderEvent.Entity.Pre Event) {
        if (Event.getEntity() instanceof DClassLike Body && ControlBodies.isPersonLike(Body) && !Body.isPuppet()
                && Scp215Disguises.render(Body, Event.getPartialTick(), Event.getPoseStack(), Event.getBufferSource(), Event.getPackedLight())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp215Visions.reset();
        Scp215Disguises.reset();
    }
}
