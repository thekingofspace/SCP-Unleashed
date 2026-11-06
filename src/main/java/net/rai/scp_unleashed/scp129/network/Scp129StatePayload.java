package net.rai.scp_unleashed.scp129.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp129StatePayload(int Stage) implements CustomPacketPayload {
    public static final Type<Scp129StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp129_state"));
    public static final StreamCodec<ByteBuf, Scp129StatePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp129StatePayload::Stage,
            Scp129StatePayload::new);

    @Override
    public Type<Scp129StatePayload> type() {
        return PayloadType;
    }
}
