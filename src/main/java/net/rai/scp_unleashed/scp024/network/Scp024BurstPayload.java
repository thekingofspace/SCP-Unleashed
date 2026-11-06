package net.rai.scp_unleashed.scp024.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public record Scp024BurstPayload(List<BlockPos> Cannons, BlockPos Focus, int Strength) implements CustomPacketPayload {
    public static final Type<Scp024BurstPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_burst"));
    public static final StreamCodec<ByteBuf, Scp024BurstPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(16)), Scp024BurstPayload::Cannons,
            BlockPos.STREAM_CODEC, Scp024BurstPayload::Focus,
            ByteBufCodecs.VAR_INT, Scp024BurstPayload::Strength,
            Scp024BurstPayload::new);

    @Override
    public Type<Scp024BurstPayload> type() {
        return PayloadType;
    }
}
