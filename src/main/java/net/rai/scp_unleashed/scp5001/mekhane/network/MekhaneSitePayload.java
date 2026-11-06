package net.rai.scp_unleashed.scp5001.mekhane.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record MekhaneSitePayload(int Stage, int Elapsed, BlockPos Emitter, double Hover) implements CustomPacketPayload {
    public static final Type<MekhaneSitePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "mekhane_site"));
    public static final StreamCodec<ByteBuf, MekhaneSitePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MekhaneSitePayload::Stage,
            ByteBufCodecs.VAR_INT, MekhaneSitePayload::Elapsed,
            BlockPos.STREAM_CODEC, MekhaneSitePayload::Emitter,
            ByteBufCodecs.DOUBLE, MekhaneSitePayload::Hover,
            MekhaneSitePayload::new);

    @Override
    public Type<MekhaneSitePayload> type() {
        return PayloadType;
    }
}
