package net.rai.scp_unleashed.stranger.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record StrangerVanishPayload(int Colour) implements CustomPacketPayload {
    public static final Type<StrangerVanishPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "stranger_vanish"));
    public static final StreamCodec<ByteBuf, StrangerVanishPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.INT, StrangerVanishPayload::Colour,
            StrangerVanishPayload::new);

    @Override
    public Type<StrangerVanishPayload> type() {
        return PayloadType;
    }
}
