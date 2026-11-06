package net.rai.scp_unleashed.scp4666.christmas.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666DoorNote;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp4666YuletideClient {
    private Scp4666YuletideClient() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Scp4666DoorNote.Reader = Scp4666YuletideClient::openNote;
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp4666Registry.DoorNote.get(), Scp4666DoorNoteRenderer::new);
        Event.registerEntityRenderer(Scp4666Registry.Sighting.get(), Scp4666SightingRenderer::new);
    }

    public static void openNote() {
        Minecraft.getInstance().setScreen(new Scp4666NoteScreen());
    }
}
