package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public record TintedBuffers(MultiBufferSource Inner, int Argb) implements MultiBufferSource {
    @Override
    public VertexConsumer getBuffer(RenderType Type) {
        return new TintedConsumer(Inner.getBuffer(Type), (Argb >> 16 & 255) / 255.0f, (Argb >> 8 & 255) / 255.0f, (Argb & 255) / 255.0f, (Argb >>> 24) / 255.0f);
    }
}
