package net.rai.scp_unleashed.scp001scarlet.king.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public record ScarletRiteFxPayload(BlockPos Throne, List<BlockPos> Seats, long Start) implements CustomPacketPayload {
    public static final Type<ScarletRiteFxPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scarlet_king_rite_fx"));
    public static final StreamCodec<ByteBuf, ScarletRiteFxPayload> PayloadCodec = StreamCodec.composite(BlockPos.STREAM_CODEC, ScarletRiteFxPayload::Throne,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(16)), ScarletRiteFxPayload::Seats, ByteBufCodecs.VAR_LONG, ScarletRiteFxPayload::Start, ScarletRiteFxPayload::new);

    @Override
    public Type<ScarletRiteFxPayload> type() {
        return PayloadType;
    }
}
