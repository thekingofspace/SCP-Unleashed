package net.rai.scp_unleashed.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.DuctWallBlock;
import net.rai.scp_unleashed.block.RenderableBlock;
import net.rai.scp_unleashed.block.entity.DuctWallBlockEntity;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.registry.ModMenuTypes;
import net.rai.scp_unleashed.registry.ModParticles;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class ModClientEvents {
    public static final String LayerModelFolder = "block/layer";

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(ModParticles.VentAir.get(), VentAirParticle.Provider::new);
        Event.registerSpecial(ModParticles.PipeLiquid.get(), new PipeLiquidParticle.Provider());
    }

    @SubscribeEvent
    public static void registerLayerModels(ModelEvent.RegisterAdditional Event) {
        String Prefix = "models/";
        String Suffix = ".json";
        Minecraft.getInstance().getResourceManager().listResources(Prefix + LayerModelFolder, Location -> Location.getPath().endsWith(Suffix)).keySet()
                .forEach(Location -> Event.register(ModelResourceLocation.standalone(Location.withPath(Path -> Path.substring(Prefix.length(), Path.length() - Suffix.length())))));
    }

    @SubscribeEvent
    public static void wrapRenderableBlockModels(ModelEvent.ModifyBakingResult Event) {
        for (Block Candidate : BuiltInRegistries.BLOCK) {
            if (Candidate instanceof RenderableBlock) {
                for (BlockState State : Candidate.getStateDefinition().getPossibleStates()) {
                    Event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(State), (Location, Original) -> new RenderableBlockModel(Original));
                }
            }
            if (Candidate instanceof DuctWallBlock) {
                for (BlockState State : Candidate.getStateDefinition().getPossibleStates()) {
                    Event.getModels().computeIfPresent(BlockModelShaper.stateToModelLocation(State), (Location, Original) -> new DuctWallModel(Original));
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block Event) {
        Event.register((State, World, Pos, TintIndex) -> World != null && Pos != null && World.getBlockEntity(Pos) instanceof DuctWallBlockEntity Wall && Wall.hasDisguise()
                ? Event.getBlockColors().getColor(Wall.getDisguise(), World, Pos, TintIndex) : -1, ModBlocks.PipeWallPort.get(), ModBlocks.VentWallPort.get());
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(ModMenuTypes.CoalGenerator.get(), CoalGeneratorScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(ModBlockEntityTypes.Outlet.get(), OutletRenderer::new);
        Event.registerBlockEntityRenderer(ModBlockEntityTypes.Tank.get(), TankRenderer::new);
        Event.registerBlockEntityRenderer(ModBlockEntityTypes.MiniTank.get(), MiniTankRenderer::new);
        Event.registerBlockEntityRenderer(ModBlockEntityTypes.CreativeTank.get(), CreativeTankRenderer::new);
    }
}
