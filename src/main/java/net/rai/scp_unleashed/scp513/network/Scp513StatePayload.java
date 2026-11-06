package net.rai.scp_unleashed.scp513.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp513StatePayload(int Elapsed, long Seed) implements CustomPacketPayload {
    public static final Type<Scp513StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp513_state"));
    public static final StreamCodec<ByteBuf, Scp513StatePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp513StatePayload::Elapsed,
            ByteBufCodecs.VAR_LONG, Scp513StatePayload::Seed,
            Scp513StatePayload::new);

    public static Scp513StatePayload cleared() {
        return new Scp513StatePayload(-1, 0L);
    }

    @Override
    public Type<Scp513StatePayload> type() {
        return PayloadType;
    }
}
