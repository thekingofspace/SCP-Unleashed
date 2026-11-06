package net.rai.scp_unleashed.scp096.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp096SeenPayload(int EntityId) implements CustomPacketPayload {
    public static final Type<Scp096SeenPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp096_seen"));
    public static final StreamCodec<ByteBuf, Scp096SeenPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp096SeenPayload::EntityId,
            Scp096SeenPayload::new);

    @Override
    public Type<Scp096SeenPayload> type() {
        return PayloadType;
    }
}
