package net.rai.scp_unleashed.extraction.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.extraction.ExtractionRegistry;
import net.rai.scp_unleashed.extraction.RiftConjurerItem;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class ExtractionClientEvents {
    public static final ResourceLocation ExtractingProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "extracting");
    public static final ResourceLocation CrouchingProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "crouching");
    public static final ResourceLocation HudLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "extraction_hud");
    public static final ResourceLocation SelectionProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "conjurer_selection");
    public static final ResourceLocation DetectingProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "detecting");
    public static final ResourceLocation DetectorLayer = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "extraction_detector_hud");

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ItemProperties.register(ExtractionRegistry.RiftExtractor.get(), ExtractingProperty,
                    (Stack, World, Holder, Seed) -> isExtracting(Holder, Stack) ? 1.0F : 0.0F);
            ItemProperties.register(ExtractionRegistry.RiftExtractor.get(), CrouchingProperty,
                    (Stack, World, Holder, Seed) -> Holder != null && Holder.isCrouching() ? 1.0F : 0.0F);
            ItemProperties.register(ExtractionRegistry.RiftConjurer.get(), SelectionProperty,
                    (Stack, World, Holder, Seed) -> RiftConjurerItem.selection(Stack));
            ItemProperties.register(ExtractionRegistry.RiftDetector.get(), DetectingProperty,
                    (Stack, World, Holder, Seed) -> detected(Holder));
        });
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new RiftExtractorClientExtensions(), ExtractionRegistry.RiftExtractor.get());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CROSSHAIR, HudLayer, ExtractionHud::render);
        Event.registerAbove(VanillaGuiLayers.HOTBAR, DetectorLayer, DetectorHud::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        ExtractionTracker.tick(Minecraft.getInstance());
        DetectorTracker.tick(Minecraft.getInstance());
    }

    private static float detected(@Nullable LivingEntity Holder) {
        if (Holder == null) {
            return 0.0F;
        }
        DetectorTracker.Reading Found = DetectorTracker.readingFor(Holder);
        return Found == null ? 0.0F : Found.Grade().getTier();
    }

    private static boolean isExtracting(@Nullable LivingEntity Holder, ItemStack Stack) {
        return Holder != null && Holder.isUsingItem() && (Holder.getUseItem() == Stack || Holder.getItemInHand(Holder.getUsedItemHand()) == Stack)
                && !RiftExtractorClientExtensions.unseenShadowCaster(Holder);
    }
}
