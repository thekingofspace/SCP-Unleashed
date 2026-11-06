package net.rai.scp_unleashed.scp224.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp224AgePayload(float Progress, boolean Forced) implements CustomPacketPayload {
    public static final Type<Scp224AgePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp224_age"));
    public static final StreamCodec<ByteBuf, Scp224AgePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Scp224AgePayload::Progress,
            ByteBufCodecs.BOOL, Scp224AgePayload::Forced,
            Scp224AgePayload::new);

    @Override
    public Type<Scp224AgePayload> type() {
        return PayloadType;
    }
}
