package net.rai.scp_unleashed.daevitegod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevitegod.DaeviteGodRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class DaeviteGodClientEvents {
    private DaeviteGodClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(DaeviteGodRegistry.God.get(), DaeviteGodRenderer::new);
        Event.registerEntityRenderer(DaeviteGodRegistry.Spirit.get(), DaeviteGodSpiritRenderer::new);
        Event.registerEntityRenderer(DaeviteGodRegistry.Sigil.get(), DaeviteGodSigilRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(DaeviteGodRegistry.Ember.get(), DaeviteGodParticles.EmberProvider::new);
        Event.registerSpriteSet(DaeviteGodRegistry.Ash.get(), DaeviteGodParticles.AshProvider::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(DaeviteGodCrownLayer.Location, DaeviteGodCrownLayer::create);
    }

    @SubscribeEvent
    public static void bakeLayers(EntityRenderersEvent.AddLayers Event) {
        DaeviteGodCrownLayer.bake(Event.getEntityModels());
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : DaeviteGodModels.all()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, HumanoidModel<?> Original) {
                return DaeviteGodCrownLayer.model();
            }
        }, DaeviteGodRegistry.Crown.get());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(DaeviteGodCrownDisplay::register);
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        DaeviteGodSky.fog(Event);
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent Event) {
        DaeviteGodSky.dome(Event);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        DaeviteGodSky.tick(Minecraft.getInstance());
    }
}
