package net.rai.scp_unleashed.scp178.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp178.Scp178Events;
import net.rai.scp_unleashed.scp178.Scp178Registry;
import net.rai.scp_unleashed.scp178.Scp178Watcher;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp178ClientEvents {
    private Scp178ClientEvents() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent Event) {
        Scp178Watcher.ClientSight = Scp178Vision::sees;
        Scp178Watcher.ClientTick = Scp178Voices::tick;
        Scp178Events.ClientHandler = Scp178ClientWatchers::handle;
        Event.enqueueWork(Scp178Display::register);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp178Registry.Watcher.get(), Scp178WatcherRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(Scp178GlassesLayer.Location, Scp178GlassesLayer::create);
    }

    @SubscribeEvent
    public static void bakeLayers(EntityRenderersEvent.AddLayers Event) {
        Scp178GlassesLayer.bake(Event.getEntityModels());
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, HumanoidModel<?> Original) {
                return Scp178GlassesLayer.model();
            }
        }, Scp178Registry.Glasses.get());
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerBelowAll(Scp178Vision.LayerId, Scp178Vision::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp178Vision.tick(Client);
        Scp178ClientWatchers.tick(Client);
    }

    @SubscribeEvent
    public static void attack(InputEvent.InteractionKeyMappingTriggered Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!Event.isAttack() || Client.player == null || !(Client.hitResult instanceof EntityHitResult Hit) || !(Hit.getEntity() instanceof Scp178Watcher Body)) {
            return;
        }
        Event.setCanceled(true);
        Event.setSwingHand(true);
        if (Scp178ClientWatchers.struck(Body)) {
            Client.player.resetAttackStrengthTicker();
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp178Vision.reset();
        Scp178ClientWatchers.reset();
        Scp178Voices.reset();
    }
}
