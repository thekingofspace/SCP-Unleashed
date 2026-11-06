package net.rai.scp_unleashed.scp001scarlet.king.client;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ScarletKingClientEvents {
    private ScarletKingClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(ScarletKingRegistry.KingType.get(), ScarletKingRenderer::new);
        Event.registerEntityRenderer(ScarletKingRegistry.ThornType.get(), ScarletThornRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(ScarletArmorModels.Outer, ScarletArmorModel::outer);
        Event.registerLayerDefinition(ScarletArmorModels.Inner, ScarletArmorModel::inner);
    }

    @SubscribeEvent
    public static void bakeLayers(EntityRenderersEvent.AddLayers Event) {
        ScarletArmorModels.bake(Event.getEntityModels());
        for (PlayerSkin.Model Skin : Event.getSkins()) {
            if (Event.getSkin(Skin) instanceof PlayerRenderer Renderer) {
                Renderer.addLayer(new ScarletArmorGlowLayer<>(Renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new ScarletArmorExtensions(), ScarletKingRegistry.Helmet.get(), ScarletKingRegistry.Chestplate.get(), ScarletKingRegistry.Leggings.get(),
                ScarletKingRegistry.Boots.get());
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        ScarletRiteFx.fog(Event);
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        ScarletRiteFx.shake(Event);
        ScarletKingDeathFx.shake(Event);
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent Event) {
        ScarletRiteFx.render(Event);
    }
}
