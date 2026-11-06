package net.rai.scp_unleashed.scp151.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp151BreathPayload(boolean Gazing, boolean Recovering) implements CustomPacketPayload {
    public static final Type<Scp151BreathPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp151_breath"));
    public static final StreamCodec<ByteBuf, Scp151BreathPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp151BreathPayload::Gazing,
            ByteBufCodecs.BOOL, Scp151BreathPayload::Recovering,
            Scp151BreathPayload::new);

    @Override
    public Type<Scp151BreathPayload> type() {
        return PayloadType;
    }
}
