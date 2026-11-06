package net.rai.scp_unleashed.scp5001.mekhane.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.mekhane.entity.Mekhane;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class MekhaneSkyPass {
    private static int Frame;

    private MekhaneSkyPass() {
    }

    public static int frame() {
        return Frame;
    }

    @SubscribeEvent
    public static void frameStart(RenderFrameEvent.Pre Event) {
        Frame++;
    }

    public static List<Mekhane> giants(ClientLevel World) {
        List<Mekhane> Found = new ArrayList<>(1);
        for (Entity Each : World.entitiesForRendering()) {
            if (Each instanceof Mekhane Giant && !Giant.isRemoved()) {
                Found.add(Giant);
            }
        }
        return Found;
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || Client.isPaused()) {
            return;
        }
        for (Mekhane Giant : giants(World)) {
            if (!Giant.ticked() && !World.tickRateManager().isEntityFrozen(Giant)) {
                World.tickNonPassenger(Giant);
            }
            Giant.settle();
        }
    }

    @SubscribeEvent
    public static void stage(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ShaderCompat.renderingShadowPass()) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null) {
            return;
        }
        List<Mekhane> Missed = new ArrayList<>(1);
        for (Mekhane Giant : giants(World)) {
            if (Giant.drawn() != Frame) {
                Missed.add(Giant);
            }
        }
        if (Missed.isEmpty()) {
            return;
        }
        EntityRenderDispatcher Dispatcher = Client.getEntityRenderDispatcher();
        Frustum View = Event.getFrustum();
        Vec3 Eye = Event.getCamera().getPosition();
        MultiBufferSource.BufferSource Buffers = Client.renderBuffers().bufferSource();
        PoseStack Pose = new PoseStack();
        for (Mekhane Giant : Missed) {
            if (View != null && !Dispatcher.shouldRender(Giant, View, Eye.x, Eye.y, Eye.z)) {
                continue;
            }
            float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(!World.tickRateManager().isEntityFrozen(Giant));
            double X = Mth.lerp(Partial, Giant.xOld, Giant.getX());
            double Y = Mth.lerp(Partial, Giant.yOld, Giant.getY());
            double Z = Mth.lerp(Partial, Giant.zOld, Giant.getZ());
            float Yaw = Mth.lerp(Partial, Giant.yRotO, Giant.getYRot());
            Dispatcher.render(Giant, X - Eye.x, Y - Eye.y, Z - Eye.z, Yaw, Partial, Pose, Buffers, Dispatcher.getPackedLightCoords(Giant, Partial));
        }
        Buffers.endLastBatch();
    }
}
