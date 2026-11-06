package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public record TintedConsumer(VertexConsumer Inner, float Red, float Green, float Blue, float Alpha) implements VertexConsumer {
    @Override
    public VertexConsumer addVertex(float X, float Y, float Z) {
        Inner.addVertex(X, Y, Z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int R, int G, int B, int A) {
        Inner.setColor(Math.round(R * Red), Math.round(G * Green), Math.round(B * Blue), Math.round(A * Alpha));
        return this;
    }

    @Override
    public VertexConsumer setUv(float U, float V) {
        Inner.setUv(U, V);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int U, int V) {
        Inner.setUv1(U, V);
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
