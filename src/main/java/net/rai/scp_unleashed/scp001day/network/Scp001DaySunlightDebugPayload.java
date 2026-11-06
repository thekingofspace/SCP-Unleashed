package net.rai.scp_unleashed.scp001day.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp001DaySunlightDebugPayload() implements CustomPacketPayload {
    public static final Scp001DaySunlightDebugPayload Instance = new Scp001DaySunlightDebugPayload();
    public static final Type<Scp001DaySunlightDebugPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp001_day_sunlight_debug"));
    public static final StreamCodec<ByteBuf, Scp001DaySunlightDebugPayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<Scp001DaySunlightDebugPayload> type() {
        return PayloadType;
    }
}
