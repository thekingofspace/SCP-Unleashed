package net.rai.scp_unleashed.scp327.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp327StrugglePayload() implements CustomPacketPayload {
    public static final Scp327StrugglePayload Press = new Scp327StrugglePayload();
    public static final Type<Scp327StrugglePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp327_struggle"));
    public static final StreamCodec<ByteBuf, Scp327StrugglePayload> PayloadCodec = StreamCodec.unit(Press);

    @Override
    public Type<Scp327StrugglePayload> type() {
        return PayloadType;
    }
}
