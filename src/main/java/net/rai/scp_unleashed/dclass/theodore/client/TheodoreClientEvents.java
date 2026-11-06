package net.rai.scp_unleashed.dclass.theodore.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.CloningKind;
import net.rai.scp_unleashed.dclass.client.CloningMachineRenderer;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.dclass.theodore.TheodoreRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class TheodoreClientEvents {
    private TheodoreClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> CloningMachineRenderer.registerSpecimen(CloningKind.TheodoreLine, World -> TheodoreRegistry.TheodoreType.get().create(World)));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(TheodoreRegistry.TheodoreType.get(), DClassRenderer::new);
        Event.registerEntityRenderer(TheodoreRegistry.AmalgamationType.get(), AmalgamationRenderer::new);
    }
}
