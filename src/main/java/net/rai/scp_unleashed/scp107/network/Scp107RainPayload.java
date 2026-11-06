package net.rai.scp_unleashed.scp107.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp107RainPayload(int Kind, int Elapsed, int Total) implements CustomPacketPayload {
    public static final Type<Scp107RainPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp107_rain"));
    public static final StreamCodec<ByteBuf, Scp107RainPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp107RainPayload::Kind,
            ByteBufCodecs.VAR_INT, Scp107RainPayload::Elapsed,
            ByteBufCodecs.VAR_INT, Scp107RainPayload::Total,
            Scp107RainPayload::new);

    @Override
    public Type<Scp107RainPayload> type() {
        return PayloadType;
    }
}
