package net.rai.scp_unleashed.scp330.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp330MarkPayload(int EntityId, byte Flags) implements CustomPacketPayload {
    public static final Type<Scp330MarkPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp330_mark"));
    public static final StreamCodec<ByteBuf, Scp330MarkPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp330MarkPayload::EntityId,
            ByteBufCodecs.BYTE, Scp330MarkPayload::Flags,
            Scp330MarkPayload::new);

    @Override
    public Type<Scp330MarkPayload> type() {
        return PayloadType;
    }
}
