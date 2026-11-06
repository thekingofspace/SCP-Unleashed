package net.rai.scp_unleashed.scp539.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp539WarnPayload(int Ticks) implements CustomPacketPayload {
    public static final Type<Scp539WarnPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp539_warn"));
    public static final StreamCodec<ByteBuf, Scp539WarnPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp539WarnPayload::Ticks,
            Scp539WarnPayload::new);

    @Override
    public Type<Scp539WarnPayload> type() {
        return PayloadType;
    }
}
