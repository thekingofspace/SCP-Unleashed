package net.rai.scp_unleashed.scp049.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public record Scp049TintBuffers(MultiBufferSource Inner, int Argb) implements MultiBufferSource {
    @Override
    public VertexConsumer getBuffer(RenderType Type) {
        return new Tinted(Inner, Type, (Argb >> 16 & 255) / 255.0f, (Argb >> 8 & 255) / 255.0f, (Argb & 255) / 255.0f, (Argb >>> 24) / 255.0f);
    }

    public static final class Tinted implements VertexConsumer {
        private final MultiBufferSource Source;
        private final RenderType Type;
        private final boolean Refetch;
        private final float Red;
        private final float Green;
        private final float Blue;
        private final float Alpha;
        private VertexConsumer Current;

        public Tinted(MultiBufferSource Source, RenderType Type, float Red, float Green, float Blue, float Alpha) {
            this.Source = Source;
            this.Type = Type;
            this.Refetch = Type.canConsolidateConsecutiveGeometry();
            this.Red = Red;
            this.Green = Green;
            this.Blue = Blue;
            this.Alpha = Alpha;
            this.Current = Source.getBuffer(Type);
        }

        @Override
        public VertexConsumer addVertex(float X, float Y, float Z) {
            if (Refetch) {
                Current = Source.getBuffer(Type);
            }
            Current.addVertex(X, Y, Z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int R, int G, int B, int A) {
            Current.setColor(Math.round(R * Red), Math.round(G * Green), Math.round(B * Blue), Math.round(A * Alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float U, float V) {
            Current.setUv(U, V);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int U, int V) {
            Current.setUv1(U, V);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int U, int V) {
            Current.setUv2(U, V);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float X, float Y, float Z) {
            Current.setNormal(X, Y, Z);
            return this;
        }
    }
}
