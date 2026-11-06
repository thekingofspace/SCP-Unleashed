package net.rai.scp_unleashed.raid.hostile.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.serpentshand.SerpentPodItem;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class HostileClientEvents {
    public static final ResourceLocation FilledProperty = HostileRegistry.location("filled");

    private HostileClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(HostileRegistry.AgentType.get(), HostileRaiderRenderer::new);
        Event.registerEntityRenderer(HostileRegistry.InsurgentType.get(), HostileRaiderRenderer::new);
        Event.registerEntityRenderer(HostileRegistry.SealType.get(), SerpentSealRenderer::new);
        Event.registerEntityRenderer(HostileRegistry.HelicopterType.get(), ChaosHelicopterRenderer::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> ItemProperties.register(HostileRegistry.Pod.get(), FilledProperty,
                (Stack, World, Holder, Seed) -> SerpentPodItem.filled(Stack) ? 1.0f : 0.0f));
    }
}
