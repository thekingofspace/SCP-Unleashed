package net.rai.scp_unleashed.z0003.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

public record Z0003StoneBuffers(MultiBufferSource Base, float Crack, boolean Fallback) implements MultiBufferSource {
    public static final int FallbackShade = 236;

    @Override
    public VertexConsumer getBuffer(RenderType Requested) {
        if (Requested.format() != DefaultVertexFormat.NEW_ENTITY || Requested.mode() != VertexFormat.Mode.QUADS) {
            return Requested.toString().contains("glint") ? Discard.Instance : Base.getBuffer(Requested);
        }
        if (Fallback) {
            return new Stone(Base, Z0003RenderTypes.fallbackStone(Crack), FallbackShade, FallbackShade, FallbackShade);
        }
        int Packed = Math.round(Math.max(0.0f, Math.min(1.0f, Crack)) * 255.0f);
        return new Stone(Base, Z0003RenderTypes.stone(Requested), Packed, 255, 255);
    }

    public static final class Stone implements VertexConsumer {
        private final MultiBufferSource Source;
        private final RenderType Type;
        private final boolean Refetch;
        private final int Red;
        private final int Green;
        private final int Blue;
        private VertexConsumer Inner;

        public Stone(MultiBufferSource Source, RenderType Type, int Red, int Green, int Blue) {
            this.Source = Source;
            this.Type = Type;
            this.Refetch = Type.canConsolidateConsecutiveGeometry();
            this.Red = Red;
            this.Green = Green;
            this.Blue = Blue;
            this.Inner = Source.getBuffer(Type);
        }

        @Override
        public VertexConsumer addVertex(float X, float Y, float Z) {
            if (Refetch) {
                Inner = Source.getBuffer(Type);
            }
            Inner.addVertex(X, Y, Z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int R, int G, int B, int A) {
            Inner.setColor(Red, Green, Blue, A);
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
            Inner.setUv2(U, V);
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
