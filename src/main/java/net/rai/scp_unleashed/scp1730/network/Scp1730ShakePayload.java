package net.rai.scp_unleashed.scp1730.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp1730ShakePayload(float Intensity, int Ticks) implements CustomPacketPayload {
    public static final Type<Scp1730ShakePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp1730_shake"));
    public static final StreamCodec<ByteBuf, Scp1730ShakePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Scp1730ShakePayload::Intensity,
            ByteBufCodecs.VAR_INT, Scp1730ShakePayload::Ticks,
            Scp1730ShakePayload::new);

    @Override
    public Type<Scp1730ShakePayload> type() {
        return PayloadType;
    }
}
