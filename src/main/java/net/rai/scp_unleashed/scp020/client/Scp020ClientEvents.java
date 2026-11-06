package net.rai.scp_unleashed.scp020.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp020.Scp020Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp020ClientEvents {
    private static Scp020GuiseRenderer Disguise;

    private Scp020ClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp020Eyes.LayerId, Scp020Eyes::render);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        Disguise = new Scp020GuiseRenderer(Event.getContext());
        Scp020GogglesLayer.bake(Event.getEntityModels());
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(Scp020GogglesLayer.Location, Scp020GogglesLayer::create);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, HumanoidModel<?> Original) {
                return Scp020GogglesLayer.model();
            }
        }, Scp020Registry.Goggles.get());
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp020Registry.Spore.get(), Sprites -> {
            Scp020SporeSight.moteSprites(Sprites);
            return new Scp020SporeParticles.MoteProvider(Sprites);
        });
        Event.registerSpriteSet(Scp020Registry.Film.get(), Sprites -> {
            Scp020SporeSight.filmSprites(Sprites);
            return new Scp020SporeParticles.FilmProvider(Sprites);
        });
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Scp020Eyes.tick(Client);
        Scp020Whispers.tick(Client);
        Scp020Guises.tick(Client);
        Scp020SporeSight.tick(Client);
    }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre Event) {
        if (Disguise == null || !(Event.getEntity() instanceof AbstractClientPlayer Target) || !Scp020Guises.isZombie(Target)) {
            return;
        }
        Event.setCanceled(true);
        float Partial = Event.getPartialTick();
        Disguise.render(Target, Mth.lerp(Partial, Target.yRotO, Target.getYRot()), Partial, Event.getPoseStack(), Event.getMultiBufferSource(), Event.getPackedLight());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp020ClientState.reset();
        Scp020Eyes.reset();
        Scp020Whispers.reset();
        Scp020Guises.reset();
        Scp020SporeSight.reset();
    }
}
