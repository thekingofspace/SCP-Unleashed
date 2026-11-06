package net.rai.scp_unleashed.scp024.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.UUID;

public record Scp024BarPayload(UUID Bar) implements CustomPacketPayload {
    public static final Type<Scp024BarPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_bar"));
    public static final StreamCodec<ByteBuf, Scp024BarPayload> PayloadCodec = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Scp024BarPayload::Bar,
            Scp024BarPayload::new);

    @Override
    public Type<Scp024BarPayload> type() {
        return PayloadType;
    }
}
