package net.rai.scp_unleashed.scp009.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

public record CrystalBuffers(MultiBufferSource Base, RenderType Crystal, int GlowLight) implements MultiBufferSource {
    @Override
    public VertexConsumer getBuffer(RenderType Requested) {
        if (Requested.format() != DefaultVertexFormat.NEW_ENTITY || Requested.mode() != VertexFormat.Mode.QUADS) {
            return Discard.Instance;
        }
        return new Crystalline(Base.getBuffer(Crystal), GlowLight << 4);
    }

    public record Crystalline(VertexConsumer Inner, int MinBlock) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float X, float Y, float Z) {
            Inner.addVertex(X, Y, Z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int R, int G, int B, int A) {
            Inner.setColor(255, 255, 255, 255);
            return this;
        }

        @Override
        public VertexConsumer setUv(float U, float V) {
            Inner.setUv(U, V);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int U, int V) {
            Inner.setUv1(OverlayTexture.NO_OVERLAY & 0xFFFF, OverlayTexture.NO_OVERLAY >> 16 & 0xFFFF);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int U, int V) {
            Inner.setUv2(Math.max(U, MinBlock), V);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float X, float Y, float Z) {
            Inner.setNormal(X, Y, Z);
            return this;
        }
    }

    public enum Discard implements VertexConsumer {
        Instance;

        @Override
        public VertexConsumer addVertex(float X, float Y, float Z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int R, int G, int B, int A) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float U, float V) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int U, int V) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int U, int V) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float X, float Y, float Z) {
            return this;
        }
    }
}
