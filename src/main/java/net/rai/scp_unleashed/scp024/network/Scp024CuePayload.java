package net.rai.scp_unleashed.scp024.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp024CuePayload(int Cue) implements CustomPacketPayload {
    public static final Type<Scp024CuePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_cue"));
    public static final StreamCodec<ByteBuf, Scp024CuePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp024CuePayload::Cue,
            Scp024CuePayload::new);

    @Override
    public Type<Scp024CuePayload> type() {
        return PayloadType;
    }
}
