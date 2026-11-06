package net.rai.scp_unleashed.scp020.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp020StatePayload(int Intensity) implements CustomPacketPayload {
    public static final Type<Scp020StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp020_state"));
    public static final StreamCodec<ByteBuf, Scp020StatePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp020StatePayload::Intensity,
            Scp020StatePayload::new);

    @Override
    public Type<Scp020StatePayload> type() {
        return PayloadType;
    }
}
