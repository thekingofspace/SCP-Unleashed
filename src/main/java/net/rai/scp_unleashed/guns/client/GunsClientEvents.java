package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunsRegistry;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GunsClientEvents {
    public static final String KeyCategory = "key.categories.scp_unleashed.weapons";
    public static final KeyMapping ReloadKey = new KeyMapping(GunItem.ReloadKey, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KeyCategory);
    public static final ResourceLocation ScopeLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_scope");
    public static final ResourceLocation CrosshairLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_crosshair");
    public static final ResourceLocation AmmoLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_ammo");
    public static final ResourceLocation ChargesProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_charges");
    public static final ResourceLocation OpenProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_open");
    public static final ResourceLocation StageProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_reload_stage");
    public static final ResourceLocation BracedProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_braced");

    private GunsClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(GunsRegistry.ParticleDisruptor.get(), ChargesProperty,
                    (Stack, World, Holder, Seed) -> GunItem.getCharges(Stack) / (float) GunKind.ParticleDisruptor.getStats().getCharges());
            ItemProperties.register(GunsRegistry.DoubleBarrel.get(), OpenProperty, (Stack, World, Holder, Seed) -> GunItem.getReload(Stack) != null ? 1.0F : 0.0F);
            ItemProperties.register(GunsRegistry.CombatShotgun.get(), StageProperty, (Stack, World, Bearer, Seed) -> GunReloadStage.of(Stack));
            ItemProperties.register(GunsRegistry.MachineGun.get(), StageProperty, (Stack, World, Bearer, Seed) -> GunReloadStage.of(Stack));
            ItemProperties.register(GunsRegistry.MachineGun.get(), BracedProperty, (Stack, World, Bearer, Seed) -> GunReloadStage.braced(Stack, Bearer));
        });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent Event) {
        Event.register(ReloadKey);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(GunsRegistry.GunBenchMenuType.get(), GunBenchScreen::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(GunsRegistry.Spark.get(), GunSparkParticle.SparkProvider::new);
        Event.registerSpriteSet(GunsRegistry.EnergySpark.get(), GunSparkParticle.EnergyProvider::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, ScopeLayer, GunHud::renderScope);
        Event.registerAbove(VanillaGuiLayers.CROSSHAIR, CrosshairLayer, GunHud::renderCrosshair);
        Event.registerAbove(VanillaGuiLayers.HOTBAR, AmmoLayer, GunHud::renderAmmo);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        GunClientExtensions Extensions = new GunClientExtensions();
        for (GunKind Kind : GunKind.values()) {
            Event.registerItem(Extensions, GunsRegistry.gunItem(Kind));
        }
    }
}
