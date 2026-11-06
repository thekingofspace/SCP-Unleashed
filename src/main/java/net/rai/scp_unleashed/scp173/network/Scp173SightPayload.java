package net.rai.scp_unleashed.scp173.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp173.Scp173Sight;

import java.util.List;

public record Scp173SightPayload(List<Integer> Ids) implements CustomPacketPayload {
    public static final Type<Scp173SightPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp173_sight"));
    public static final StreamCodec<ByteBuf, Scp173SightPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(Scp173Sight.MaxReported)), Scp173SightPayload::Ids,
            Scp173SightPayload::new);

    @Override
    public Type<Scp173SightPayload> type() {
        return PayloadType;
    }
}
