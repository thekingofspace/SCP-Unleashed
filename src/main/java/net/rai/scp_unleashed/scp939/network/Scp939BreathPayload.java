package net.rai.scp_unleashed.scp939.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp939BreathPayload(float Exposure, float Breath, float Haze, boolean Masked) implements CustomPacketPayload {
    public static final Type<Scp939BreathPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp939_breath"));
    public static final StreamCodec<ByteBuf, Scp939BreathPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Scp939BreathPayload::Exposure,
            ByteBufCodecs.FLOAT, Scp939BreathPayload::Breath,
            ByteBufCodecs.FLOAT, Scp939BreathPayload::Haze,
            ByteBufCodecs.BOOL, Scp939BreathPayload::Masked,
            Scp939BreathPayload::new);

    @Override
    public Type<Scp939BreathPayload> type() {
        return PayloadType;
    }
}
