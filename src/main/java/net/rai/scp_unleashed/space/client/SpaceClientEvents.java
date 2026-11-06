package net.rai.scp_unleashed.space.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.space.AirCanisterItem;
import net.rai.scp_unleashed.space.AirInfuserBlockEntity;
import net.rai.scp_unleashed.space.SpaceAir;
import net.rai.scp_unleashed.space.SpaceRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class SpaceClientEvents {
    public static final ResourceLocation AirProperty = SpaceRegistry.location("air");
    public static final float FogStart = 0.92F;

    private SpaceClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        AirInfuserBlockEntity.setClientEffects(AirInfuserSound::tick);
        Event.enqueueWork(() -> ItemProperties.register(SpaceRegistry.AirCanister.get(), AirProperty,
                (Stack, World, Holder, Seed) -> SpaceAir.canisterAir(Stack) / (float) AirCanisterItem.Capacity));
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(SpaceRegistry.MoonEffects, new MoonSkyEffects());
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, SpaceVisorOverlay.LayerId, SpaceVisorOverlay::render);
        Event.registerAbove(VanillaGuiLayers.AIR_LEVEL, SpaceAirHud.LayerId, SpaceAirHud::render);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(SpaceRegistry.AirInfuserMenuType.get(), AirInfuserScreen::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        SpaceSuitModels.locations().forEach((Piece, Location) -> Event.registerLayerDefinition(Location, () -> SpaceSuitLayers.create(Piece)));
    }

    @SubscribeEvent
    public static void bakeLayers(EntityRenderersEvent.AddLayers Event) {
        SpaceSuitModels.bake(Event.getEntityModels());
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        SpaceSuitModels.locations().keySet().forEach(Piece -> Event.registerItem(new SpaceSuitModels.Extensions(Piece), SpaceRegistry.suitItem(Piece).get()));
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Event.getType() != FogType.NONE || !SpaceAir.isAirless(Client.level) || blinded(Event.getCamera().getEntity())) {
            return;
        }
        Event.setNearPlaneDistance(Event.getFarPlaneDistance() * FogStart);
        Event.setCanceled(true);
    }

    private static boolean blinded(Entity Viewer) {
        return Viewer instanceof LivingEntity Living && (Living.hasEffect(MobEffects.DARKNESS) || Living.hasEffect(MobEffects.BLINDNESS));
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Event.getCamera().getFluidInCamera() != FogType.NONE || !SpaceAir.isAirless(Client.level)) {
            return;
        }
        Event.setRed(0.0F);
        Event.setGreen(0.0F);
        Event.setBlue(0.0F);
    }
}
