package net.rai.scp_unleashed.daevite.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record DaeviteSkyPayload(BlockPos Centre, float Strength, int Radius) implements CustomPacketPayload {
    public static final Type<DaeviteSkyPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_sky"));
    public static final StreamCodec<ByteBuf, DaeviteSkyPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DaeviteSkyPayload::Centre,
            ByteBufCodecs.FLOAT, DaeviteSkyPayload::Strength,
            ByteBufCodecs.VAR_INT, DaeviteSkyPayload::Radius,
            DaeviteSkyPayload::new);

    @Override
    public Type<DaeviteSkyPayload> type() {
        return PayloadType;
    }
}
