package net.rai.scp_unleashed.scp016.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp016StatePayload(int Stage, float Progress) implements CustomPacketPayload {
    public static final Type<Scp016StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp016_state"));
    public static final StreamCodec<ByteBuf, Scp016StatePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp016StatePayload::Stage,
            ByteBufCodecs.FLOAT, Scp016StatePayload::Progress,
            Scp016StatePayload::new);

    @Override
    public Type<Scp016StatePayload> type() {
        return PayloadType;
    }
}
