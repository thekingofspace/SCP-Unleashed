package net.rai.scp_unleashed.rifttech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.Extractable;
import net.rai.scp_unleashed.extraction.client.ExtractionBeam;
import net.rai.scp_unleashed.extraction.client.ExtractionRenderTypes;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rifttech.RiftCollectorBlockEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class CollectorBeams {
    private static final Set<RiftCollectorBlockEntity> Active = Collections.newSetFromMap(new WeakHashMap<>());

    private CollectorBeams() {
    }

    public static void track(RiftCollectorBlockEntity Collector) {
        Active.add(Collector);
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || Active.isEmpty() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 Eye = Event.getCamera().getPosition();
        float Time = (World.getGameTime() % 72000L) + Partial;
        List<RiftCollectorBlockEntity> Drawn = new ArrayList<>(Active);
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        VertexConsumer Consumer = null;
        PoseStack.Pose Last = Event.getPoseStack().last();
        for (RiftCollectorBlockEntity Collector : Drawn) {
            if (Collector.isRemoved() || Collector.getLevel() != World || !Collector.isBeaming()) {
                Active.remove(Collector);
                continue;
            }
            Entity Target = World.getEntity(Collector.getClientTarget());
            if (!(Target instanceof Extractable Rift) || Target.isRemoved()) {
                continue;
            }
            Vec3 Start = Collector.getEmitter(Partial);
            Vec3 End = Target.getPosition(Partial).add(0.0, Target.getBbHeight() * 0.5, 0.0);
            if (Start.distanceToSqr(Eye) > 160.0 * 160.0) {
                continue;
            }
            float Progress = Target instanceof RiftEntity Entry ? Entry.getShownExtraction(Partial) : Rift.getExtraction();
            if (Consumer == null) {
                Consumer = Buffers.getBuffer(ExtractionRenderTypes.Beam);
            }
            ExtractionBeam.draw(Consumer, Last, Start.subtract(Eye), End.subtract(Eye), Rift.getRiftColour(), Progress, Time, Collector.getBlockPos().hashCode() & 1023);
        }
        if (Consumer != null) {
            Buffers.endBatch(ExtractionRenderTypes.Beam);
        }
    }
}
