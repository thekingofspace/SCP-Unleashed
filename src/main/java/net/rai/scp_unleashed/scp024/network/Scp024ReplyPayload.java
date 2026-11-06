package net.rai.scp_unleashed.scp024.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp024ReplyPayload(int Token) implements CustomPacketPayload {
    public static final Type<Scp024ReplyPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_reply"));
    public static final StreamCodec<ByteBuf, Scp024ReplyPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp024ReplyPayload::Token,
            Scp024ReplyPayload::new);

    @Override
    public Type<Scp024ReplyPayload> type() {
        return PayloadType;
    }
}
