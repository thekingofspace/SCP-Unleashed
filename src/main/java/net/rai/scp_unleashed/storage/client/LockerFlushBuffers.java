package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public final class LockerFlushBuffers implements MultiBufferSource {
    private final MultiBufferSource Inner;
    private final Set<RenderType> Used = new LinkedHashSet<>();

    public LockerFlushBuffers(MultiBufferSource Inner) {
        this.Inner = Inner;
    }

    @Override
    public VertexConsumer getBuffer(RenderType Type) {
        Used.add(Type);
        return Inner.getBuffer(Type);
    }

    public Set<RenderType> used() {
        return Used;
    }

    public void flush(MultiBufferSource.BufferSource Source) {
        for (RenderType Type : Used) {
            Source.endBatch(Type);
        }
    }

    @Nullable
    public static MultiBufferSource.BufferSource source(MultiBufferSource Buffers) {
        MultiBufferSource Found = Buffers;
        while (Found instanceof TintedBuffers Tinted) {
            Found = Tinted.Inner();
        }
        return Found instanceof MultiBufferSource.BufferSource Source ? Source : null;
    }
}
