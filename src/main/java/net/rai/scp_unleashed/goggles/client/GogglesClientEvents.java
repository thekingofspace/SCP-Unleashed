package net.rai.scp_unleashed.goggles.client;

import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GogglesClientEvents {
    private GogglesClientEvents() {
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        for (PlayerSkin.Model Skin : Event.getSkins()) {
            if (Event.getSkin(Skin) instanceof PlayerRenderer Renderer) {
                Renderer.addLayer(new GogglesWornLayer(Renderer));
            }
        }
    }
}
