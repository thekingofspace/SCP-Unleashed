package net.rai.scp_unleashed.scp444.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp444OpenPayload() implements CustomPacketPayload {
    public static final Scp444OpenPayload Instance = new Scp444OpenPayload();
    public static final Type<Scp444OpenPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp444_open"));
    public static final StreamCodec<ByteBuf, Scp444OpenPayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<Scp444OpenPayload> type() {
        return PayloadType;
    }
}
