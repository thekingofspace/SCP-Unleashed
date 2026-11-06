package net.rai.scp_unleashed.scp409.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp409StatePayload(int EntityId, int Stage, int Growth) implements CustomPacketPayload {
    public static final int Shattered = -1;
    public static final Type<Scp409StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp409_state"));
    public static final StreamCodec<ByteBuf, Scp409StatePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp409StatePayload::EntityId,
            ByteBufCodecs.INT, Scp409StatePayload::Stage,
            ByteBufCodecs.VAR_INT, Scp409StatePayload::Growth,
            Scp409StatePayload::new);

    @Override
    public Type<Scp409StatePayload> type() {
        return PayloadType;
    }
}
