package net.rai.scp_unleashed.scp407.client;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp407.Scp407Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp407ClientEvents {
    private Scp407ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp407Display::register);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp407Registry.Sprout.get(), Scp407SproutRenderer::new);
        Event.registerEntityRenderer(Scp407Registry.Titan.get(), Scp407TitanRenderer::new);
    }

    @SubscribeEvent
    public static void wrapCarpets(ModelEvent.ModifyBakingResult Event) {
        Scp407BlendedModel.forget();
        Map<ModelResourceLocation, BakedModel> Models = Event.getModels();
        List<DeferredBlock<? extends Block>> Carpeted = new ArrayList<>(List.of(Scp407Registry.Mat, Scp407Registry.Sprig, Scp407Registry.Trunk, Scp407Registry.Bloom,
                Scp407Registry.Pod));
        Carpeted.addAll(Scp407Registry.Plants);
        for (DeferredBlock<? extends Block> Holder : Carpeted) {
            for (BlockState State : Holder.get().getStateDefinition().getPossibleStates()) {
                ModelResourceLocation Key = BlockModelShaper.stateToModelLocation(State);
                BakedModel Found = Models.get(Key);
                if (Found != null && !(Found instanceof Scp407BlendedModel)) {
                    Models.put(Key, new Scp407BlendedModel(Found));
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp407Registry.Spore.get(), Scp407Particles.SporeProvider::new);
        Event.registerSpriteSet(Scp407Registry.Leaf.get(), Scp407Particles.LeafProvider::new);
        Event.registerSpriteSet(Scp407Registry.Ash.get(), Scp407Particles.AshProvider::new);
        Event.registerSpriteSet(Scp407Registry.Mote.get(), Scp407Particles.MoteProvider::new);
        Event.registerSpriteSet(Scp407Registry.Glint.get(), Scp407Particles.GlintProvider::new);
    }
}
