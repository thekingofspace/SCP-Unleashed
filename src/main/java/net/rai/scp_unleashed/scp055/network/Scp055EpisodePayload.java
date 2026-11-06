package net.rai.scp_unleashed.scp055.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp055.Scp055Step;

import java.util.List;

public record Scp055EpisodePayload(long Seed, int DistortTicks, int StaticTicks, List<Scp055Step> Steps) implements CustomPacketPayload {
    public static final int MaxSteps = 32;
    public static final Type<Scp055EpisodePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp055_episode"));
    public static final StreamCodec<ByteBuf, Scp055EpisodePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, Scp055EpisodePayload::Seed,
            ByteBufCodecs.VAR_INT, Scp055EpisodePayload::DistortTicks,
            ByteBufCodecs.VAR_INT, Scp055EpisodePayload::StaticTicks,
            Scp055Step.StepCodec.apply(ByteBufCodecs.list(MaxSteps)), Scp055EpisodePayload::Steps,
            Scp055EpisodePayload::new);

    @Override
    public Type<Scp055EpisodePayload> type() {
        return PayloadType;
    }
}
