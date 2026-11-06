package net.rai.scp_unleashed.playerclone.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.client.ControlLooks;
import net.rai.scp_unleashed.dclass.CloningKind;
import net.rai.scp_unleashed.dclass.client.CloningMachineRenderer;
import net.rai.scp_unleashed.playerclone.PlayerCloneRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class PlayerCloneClientEvents {
    private PlayerCloneClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> {
            ControlLooks.register(BuiltInRegistries.ENTITY_TYPE.getKey(PlayerCloneRegistry.CloneType.get()), new PlayerCloneControlLook());
            CloningMachineRenderer.registerSpecimen(CloningKind.PlayerLine, World -> PlayerCloneRegistry.CloneType.get().create(World));
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(PlayerCloneRegistry.CloneType.get(), PlayerCloneRenderer::new);
    }

    @SubscribeEvent
    public static void registerReloads(RegisterClientReloadListenersEvent Event) {
        Event.registerReloadListener((ResourceManagerReloadListener) Manager -> {
            PlayerCloneSkins.reset();
            PlayerCloneControlLook.forgetArms();
        });
    }
}
