package net.rai.scp_unleashed.scp327.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp327HeldPayload(boolean Held) implements CustomPacketPayload {
    public static final Type<Scp327HeldPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp327_held"));
    public static final StreamCodec<ByteBuf, Scp327HeldPayload> PayloadCodec = StreamCodec.composite(ByteBufCodecs.BOOL, Scp327HeldPayload::Held, Scp327HeldPayload::new);

    @Override
    public Type<Scp327HeldPayload> type() {
        return PayloadType;
    }
}
