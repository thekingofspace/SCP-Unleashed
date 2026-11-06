package net.rai.scp_unleashed.scp330.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp330ClientEvents {
    private Scp330ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp330Display::register);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderArm(RenderArmEvent Event) {
        AbstractClientPlayer User = Event.getPlayer();
        if (!Scp330ClientState.isGreedy(User)) {
            return;
        }
        EntityRenderer<?> Found = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(User);
        if (!(Found instanceof PlayerRenderer Renderer)) {
            return;
        }
        Event.setCanceled(true);
        Scp330Stumps.firstPerson(Event.getPoseStack(), Event.getMultiBufferSource(), Event.getPackedLight(), User, Renderer.getModel(), Event.getArm());
    }

    @SubscribeEvent
    public static void renderLiving(RenderLivingEvent.Post<?, ?> Event) {
        LivingEntity Shown = Event.getEntity();
        if (Shown.isInvisible() || !Scp330ClientState.isShielded(Shown)) {
            return;
        }
        Scp330Bubble.render(Event.getPoseStack(), Event.getMultiBufferSource(), Shown, Event.getPartialTick());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp330ClientState.clear();
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        for (PlayerSkin.Model Skin : Event.getSkins()) {
            if (Event.getSkin(Skin) instanceof PlayerRenderer Renderer) {
                Renderer.addLayer(new Scp330StumpLayer(Renderer));
            }
        }
    }
}
