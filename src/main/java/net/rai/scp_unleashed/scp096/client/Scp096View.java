package net.rai.scp_unleashed.scp096.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp096.network.Scp096SeenPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp096View {
    public static final int SendGap = 10;
    private static final Map<Integer, Long> LastSent = new HashMap<>();
    private static long Ticks;

    private Scp096View() {
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent Event) {
        boolean Entities = Event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES;
        boolean Fallback = Event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL && !Scp096Faces.evaluatedThisFrame();
        Minecraft Client = Minecraft.getInstance();
        if ((Entities || Fallback) && Client.level != null && Client.player != null) {
            Scp096Faces.evaluate(Client, Event.getCamera().getPosition(), Event.getModelViewMatrix(), Event.getProjectionMatrix(), RenderSystem.getShaderFogEnd());
        }
    }

    @SubscribeEvent
    public static void frameStart(RenderFrameEvent.Pre Event) {
        Scp096Faces.beginFrame();
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        Ticks++;
        Set<Integer> Seen = Scp096Faces.takeSeen();
        if (Client.level == null || Client.player == null || Client.getConnection() == null) {
            return;
        }
        for (Integer Id : Seen) {
            Long Last = LastSent.get(Id);
            if (Last != null && Ticks - Last < SendGap) {
                continue;
            }
            LastSent.put(Id, Ticks);
            PacketDistributor.sendToServer(new Scp096SeenPayload(Id));
        }
        if (Ticks % 200 == 0) {
            LastSent.values().removeIf(Last -> Ticks - Last > 200);
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        LastSent.clear();
        Scp096Faces.clearAll();
    }
}
