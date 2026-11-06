package net.rai.scp_unleashed.scp427.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp427ConvulsePayload(int EntityId, int Elapsed) implements CustomPacketPayload {
    public static final int Stopped = -1;
    public static final int Overcome = -2;
    public static final Type<Scp427ConvulsePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp427_convulse"));
    public static final StreamCodec<ByteBuf, Scp427ConvulsePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp427ConvulsePayload::EntityId,
            ByteBufCodecs.INT, Scp427ConvulsePayload::Elapsed,
            Scp427ConvulsePayload::new);

    @Override
    public Type<Scp427ConvulsePayload> type() {
        return PayloadType;
    }
}
