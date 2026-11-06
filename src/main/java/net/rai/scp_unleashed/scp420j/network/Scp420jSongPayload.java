package net.rai.scp_unleashed.scp420j.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp420jSongPayload(int EntityId, boolean Playing) implements CustomPacketPayload {
    public static final Type<Scp420jSongPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp420j_song"));
    public static final StreamCodec<ByteBuf, Scp420jSongPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp420jSongPayload::EntityId,
            ByteBufCodecs.BOOL, Scp420jSongPayload::Playing,
            Scp420jSongPayload::new);

    @Override
    public Type<Scp420jSongPayload> type() {
        return PayloadType;
    }
}
