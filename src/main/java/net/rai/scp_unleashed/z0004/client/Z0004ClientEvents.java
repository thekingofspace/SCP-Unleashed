package net.rai.scp_unleashed.z0004.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0004.Z0004Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Z0004ClientEvents {
    private Z0004ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Z0004Registry.Ship.get(), Z0004ShipRenderer::new);
        Event.registerEntityRenderer(Z0004Registry.Crew.get(), Z0004PirateRenderer::new);
        Event.registerEntityRenderer(Z0004Registry.Captain.get(), Z0004PirateRenderer::new);
        Event.registerEntityRenderer(Z0004Registry.Cannonball.get(), Z0004CannonballRenderer::new);
        Event.registerEntityRenderer(Z0004Registry.Shot.get(), NoopRenderer::new);
    }
}
