package net.rai.scp_unleashed.scp027.client;

import net.minecraft.client.renderer.entity.BatRenderer;
import net.minecraft.client.renderer.entity.EndermiteRenderer;
import net.minecraft.client.renderer.entity.SilverfishRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp027.Scp027Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp027ClientEvents {
    private Scp027ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp027Registry.Subject.get(), Scp027Renderer::new);
        Event.registerEntityRenderer(Scp027Registry.Rat.get(), Scp027RatRenderer::new);
        Event.registerEntityRenderer(Scp027Registry.Silverfish.get(), SilverfishRenderer::new);
        Event.registerEntityRenderer(Scp027Registry.Endermite.get(), EndermiteRenderer::new);
        Event.registerEntityRenderer(Scp027Registry.Bat.get(), BatRenderer::new);
    }
}
