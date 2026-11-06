package net.rai.scp_unleashed.speaker.client;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.OutletRenderer;
import net.rai.scp_unleashed.speaker.SpeakerRegistry;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;
import net.rai.scp_unleashed.speaker.cassette.CassetteItem;
import net.rai.scp_unleashed.speaker.cassette.CassetteRecording;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public class SpeakerClientEvents {
    public static final int UntintedColour = 0xFFFFFFFF;
    public static final int LabelLayer = 1;

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(SpeakerRegistry.SpeakerEntity.get(), OutletRenderer::new);
    }

    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.Item Event) {
        Event.register(SpeakerClientEvents::cassetteTint, SpeakerRegistry.CassetteTape.get());
    }

    @SubscribeEvent
    public static void tickLoops(ClientTickEvent.Post Event) {
        SpeakerLoops.tick();
    }

    @SubscribeEvent
    public static void forgetLibrary(ClientPlayerNetworkEvent.LoggingOut Event) {
        AnomalousLibrary.clearClient();
        SpeakerLoops.clear();
    }

    private static int cassetteTint(ItemStack Stack, int TintIndex) {
        if (TintIndex != LabelLayer) {
            return UntintedColour;
        }
        CassetteRecording Recording = CassetteItem.recordingOf(Stack);
        return (Recording == null ? CassetteRecording.DefaultLabel : Recording.labelColour()) | 0xFF000000;
    }
}
