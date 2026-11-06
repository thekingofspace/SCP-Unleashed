package net.rai.scp_unleashed.rifttech.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class RiftTechDeferred {
    public static final int BufferBytes = 262144;
    public static final int MaxPending = 512;

    private static final List<Consumer<MultiBufferSource>> Pending = new ArrayList<>();
    private static final List<Consumer<MultiBufferSource>> Late = new ArrayList<>();
    private static MultiBufferSource.BufferSource Buffers;

    private RiftTechDeferred() {
    }

    public static void queue(Consumer<MultiBufferSource> Draw) {
        if (Pending.size() < MaxPending) {
            Pending.add(Draw);
        }
    }

    public static void queueLate(Consumer<MultiBufferSource> Draw) {
        if (Late.size() < MaxPending) {
            Late.add(Draw);
        }
    }

    @SubscribeEvent
    public static void stage(RenderLevelStageEvent Event) {
        if (Event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            if (Pending.isEmpty()) {
                return;
            }
            Minecraft.getInstance().renderBuffers().bufferSource().endLastBatch();
            flush(Pending);
        } else if (Event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            flush(Late);
        } else if (Event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            Pending.clear();
            Late.clear();
        }
    }

    private static void flush(List<Consumer<MultiBufferSource>> Draws) {
        if (Draws.isEmpty()) {
            return;
        }
        MultiBufferSource.BufferSource Own = buffers();
        for (Consumer<MultiBufferSource> Draw : Draws) {
            Draw.accept(Own);
        }
        Draws.clear();
        Own.endBatch();
    }

    private static MultiBufferSource.BufferSource buffers() {
        if (Buffers == null) {
            Buffers = MultiBufferSource.immediate(new ByteBufferBuilder(BufferBytes));
        }
        return Buffers;
    }
}
