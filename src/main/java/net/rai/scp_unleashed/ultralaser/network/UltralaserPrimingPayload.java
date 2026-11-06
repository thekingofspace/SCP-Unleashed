package net.rai.scp_unleashed.ultralaser.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record UltralaserPrimingPayload(int ShooterId, boolean Playing) implements CustomPacketPayload {
    public static final Type<UltralaserPrimingPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "ultralaser_priming"));
    public static final StreamCodec<ByteBuf, UltralaserPrimingPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UltralaserPrimingPayload::ShooterId,
            ByteBufCodecs.BOOL, UltralaserPrimingPayload::Playing,
            UltralaserPrimingPayload::new);

    @Override
    public Type<UltralaserPrimingPayload> type() {
        return PayloadType;
    }
}
