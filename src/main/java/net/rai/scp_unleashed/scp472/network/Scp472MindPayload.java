package net.rai.scp_unleashed.scp472.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp472MindPayload(float Sanity) implements CustomPacketPayload {
    public static final Type<Scp472MindPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp472_mind"));
    public static final StreamCodec<ByteBuf, Scp472MindPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Scp472MindPayload::Sanity,
            Scp472MindPayload::new);

    @Override
    public Type<Scp472MindPayload> type() {
        return PayloadType;
    }
}
