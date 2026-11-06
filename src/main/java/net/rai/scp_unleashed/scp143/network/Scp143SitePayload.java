package net.rai.scp_unleashed.scp143.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp143SitePayload(boolean Known, BlockPos Base) implements CustomPacketPayload {
    public static final Type<Scp143SitePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp143_site"));
    public static final StreamCodec<ByteBuf, Scp143SitePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp143SitePayload::Known,
            BlockPos.STREAM_CODEC, Scp143SitePayload::Base,
            Scp143SitePayload::new);

    @Override
    public Type<Scp143SitePayload> type() {
        return PayloadType;
    }
}
