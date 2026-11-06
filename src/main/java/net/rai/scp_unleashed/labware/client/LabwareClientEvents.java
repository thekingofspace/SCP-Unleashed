package net.rai.scp_unleashed.labware.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.AssayRecord;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabPetriDishBlockEntity;
import net.rai.scp_unleashed.labware.LabVials;
import net.rai.scp_unleashed.labware.LabViralStorageBlockEntity;
import net.rai.scp_unleashed.labware.LabwareRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class LabwareClientEvents {
    public static final ResourceLocation OpenProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "open");
    public static final ResourceLocation FilledProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "filled");
    public static final ResourceLocation UsedProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "used");
    public static final ResourceLocation ResultProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "result");
    public static final int Untinted = 0xFFFFFFFF;

    private LabwareClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        LabViralStorageBlockEntity.setClientEffects(LabwareEffects::tickStorage);
        Event.enqueueWork(() -> {
            ItemProperties.register(LabwareRegistry.Vial.get(), OpenProperty, (Stack, World, Wielder, Seed) -> LabVials.isOpen(Stack) ? 1.0f : 0.0f);
            ItemProperties.register(LabwareRegistry.Vial.get(), FilledProperty, (Stack, World, Wielder, Seed) -> LabVials.hasSample(Stack) ? 1.0f : 0.0f);
            ItemProperties.register(LabwareRegistry.Swab.get(), UsedProperty, (Stack, World, Wielder, Seed) -> LabVials.recordOf(Stack) != null ? 1.0f : 0.0f);
            ItemProperties.register(LabwareRegistry.ResultSlip.get(), ResultProperty, (Stack, World, Wielder, Seed) -> {
                AssayRecord Slip = Stack.get(LabwareRegistry.Assay.get());
                return Slip == null ? 0.0f : Slip.isPositive() ? 1.0f : 0.5f;
            });
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(LabwareRegistry.ViralStorageMenuType.get(), LabViralStorageScreen::new);
        Event.register(LabwareRegistry.AnalyzerMenuType.get(), LabAnalyzerScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(LabwareRegistry.TubeHolderEntity.get(), LabTubeHolderRenderer::new);
        Event.registerBlockEntityRenderer(LabwareRegistry.ViralStorageEntity.get(), LabViralStorageRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : LabwareModels.all()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(LabwareRegistry.VentPlume.get(), LabwareParticles.VentPlumeProvider::new);
    }

    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.Item Event) {
        Event.register((Stack, Layer) -> Layer != 1 ? Untinted : LabOrganisms.vialColourOf(LabVials.sampleOf(Stack)) | 0xFF000000, LabwareRegistry.Vial.get());
    }

    @SubscribeEvent
    public static void registerBlockColours(RegisterColorHandlersEvent.Block Event) {
        Event.register((State, Getter, Pos, Layer) -> {
            if (Layer != 1 || Getter == null || Pos == null) {
                return Untinted;
            }
            return Getter.getBlockEntity(Pos) instanceof LabPetriDishBlockEntity Dish ? Dish.getTint() | 0xFF000000 : LabOrganisms.UnknownColour | 0xFF000000;
        }, LabwareRegistry.PetriDish.get());
    }
}
