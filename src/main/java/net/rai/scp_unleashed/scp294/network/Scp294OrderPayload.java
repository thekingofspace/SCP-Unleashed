package net.rai.scp_unleashed.scp294.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp294.Scp294Names;

public record Scp294OrderPayload(String Text) implements CustomPacketPayload {
    public static final Type<Scp294OrderPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp294_order"));
    public static final StreamCodec<ByteBuf, Scp294OrderPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(Scp294Names.MaxRaw), Scp294OrderPayload::Text,
            Scp294OrderPayload::new);

    @Override
    public Type<Scp294OrderPayload> type() {
        return PayloadType;
    }
}
