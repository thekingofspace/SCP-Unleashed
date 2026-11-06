package net.rai.scp_unleashed.transferpod.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.transferpod.TransferPodContent;
import net.rai.scp_unleashed.transferpod.TransferPodRegistry;
import net.rai.scp_unleashed.transferpod.TransferPods;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class TransferPodClientEvents {
    public static final ResourceLocation LoadedProperty = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "transfer_pod_loaded");
    public static final int CoreTint = 0;
    public static final int SpentCore = 0xFF5A5E66;

    private TransferPodClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> ItemProperties.register(TransferPodRegistry.Pod.get(), LoadedProperty,
                (Stack, World, Holder, Seed) -> TransferPods.isLoaded(Stack) ? 1.0F : 0.0F));
    }

    @SubscribeEvent
    public static void itemColours(RegisterColorHandlersEvent.Item Event) {
        Event.register((Stack, TintIndex) -> {
            if (TintIndex != CoreTint) {
                return -1;
            }
            TransferPodContent Held = TransferPods.contentOf(Stack);
            return Held == null ? SpentCore : 0xFF000000 | Held.rgb();
        }, TransferPodRegistry.Pod.get());
    }
}
