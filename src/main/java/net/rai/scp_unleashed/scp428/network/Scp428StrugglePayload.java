package net.rai.scp_unleashed.scp428.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp428StrugglePayload() implements CustomPacketPayload {
    public static final Scp428StrugglePayload Instance = new Scp428StrugglePayload();
    public static final Type<Scp428StrugglePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp428_struggle"));
    public static final StreamCodec<ByteBuf, Scp428StrugglePayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<Scp428StrugglePayload> type() {
        return PayloadType;
    }
}
