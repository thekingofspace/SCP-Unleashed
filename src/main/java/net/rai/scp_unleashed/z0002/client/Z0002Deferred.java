package net.rai.scp_unleashed.z0002.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
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
public final class Z0002Deferred {
    public static final int BufferBytes = 65536;
    public static final int MaxPending = 256;
    private static final List<Consumer<MultiBufferSource>> Pending = new ArrayList<>();
    private static MultiBufferSource.BufferSource Buffers;

    private Z0002Deferred() {
    }

    public static void queue(Consumer<MultiBufferSource> Draw) {
        if (Pending.size() < MaxPending) {
            Pending.add(Draw);
        }
    }

    @SubscribeEvent
    public static void stage(RenderLevelStageEvent Event) {
        if (Event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            if (Pending.isEmpty()) {
                return;
            }
            if (Buffers == null) {
                Buffers = MultiBufferSource.immediate(new ByteBufferBuilder(BufferBytes));
            }
            for (Consumer<MultiBufferSource> Draw : Pending) {
                Draw.accept(Buffers);
            }
            Pending.clear();
            Buffers.endBatch();
        } else if (Event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            Pending.clear();
        }
    }
}
