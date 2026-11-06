package net.rai.scp_unleashed.stranger.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record StrangerReplyPayload(int EntityId, boolean Finished) implements CustomPacketPayload {
    public static final Type<StrangerReplyPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "stranger_reply"));
    public static final StreamCodec<ByteBuf, StrangerReplyPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StrangerReplyPayload::EntityId,
            ByteBufCodecs.BOOL, StrangerReplyPayload::Finished,
            StrangerReplyPayload::new);

    @Override
    public Type<StrangerReplyPayload> type() {
        return PayloadType;
    }
}
