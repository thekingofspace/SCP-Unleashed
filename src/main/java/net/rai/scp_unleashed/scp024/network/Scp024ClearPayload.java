package net.rai.scp_unleashed.scp024.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;
import java.util.UUID;

public record Scp024ClearPayload(List<UUID> Bars) implements CustomPacketPayload {
    public static final Type<Scp024ClearPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_clear"));
    public static final StreamCodec<ByteBuf, Scp024ClearPayload> PayloadCodec = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list(64)), Scp024ClearPayload::Bars,
            Scp024ClearPayload::new);

    @Override
    public Type<Scp024ClearPayload> type() {
        return PayloadType;
    }
}
