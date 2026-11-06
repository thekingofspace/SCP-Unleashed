package net.rai.scp_unleashed.seasonal.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record SeasonsPayload(int Mask) implements CustomPacketPayload {
    public static final Type<SeasonsPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "seasons"));
    public static final StreamCodec<ByteBuf, SeasonsPayload> PayloadCodec = ByteBufCodecs.VAR_INT.map(SeasonsPayload::new, SeasonsPayload::Mask);

    @Override
    public Type<SeasonsPayload> type() {
        return PayloadType;
    }
}
