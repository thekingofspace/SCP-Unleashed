package net.rai.scp_unleashed.scp129.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp129EpisodePayload(int Kind, int Ticks, float Strength) implements CustomPacketPayload {
    public static final Type<Scp129EpisodePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp129_episode"));
    public static final StreamCodec<ByteBuf, Scp129EpisodePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp129EpisodePayload::Kind,
            ByteBufCodecs.VAR_INT, Scp129EpisodePayload::Ticks,
            ByteBufCodecs.FLOAT, Scp129EpisodePayload::Strength,
            Scp129EpisodePayload::new);

    @Override
    public Type<Scp129EpisodePayload> type() {
        return PayloadType;
    }
}
