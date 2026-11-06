package net.rai.scp_unleashed.scp131.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp131.Scp131Cues;
import net.rai.scp_unleashed.scp131.Scp131Grief;
import net.rai.scp_unleashed.scp131.Scp131Registry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp131ClientEvents {
    private Scp131ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            Scp131Cues.Handler = (Pod, Kind) -> Scp131Face.of(Pod).cue(Kind);
            Scp131Cues.Renderer = Accept -> Accept.accept(new GeoRenderProvider() {
                @Nullable
                private Scp131ItemRenderer Drawer;

                @Override
                public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                    if (Drawer == null) {
                        Drawer = new Scp131ItemRenderer();
                    }
                    return Drawer;
                }
            });
            Scp131Display.register();
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp131Registry.Pod.get(), Scp131Renderer::new);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp131Grief.setClientUntil(0L);
    }
}
