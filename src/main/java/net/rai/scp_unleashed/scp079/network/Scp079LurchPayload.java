package net.rai.scp_unleashed.scp079.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp079LurchPayload(int TargetId) implements CustomPacketPayload {
    public static final Type<Scp079LurchPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp079_lurch"));
    public static final StreamCodec<ByteBuf, Scp079LurchPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp079LurchPayload::TargetId,
            Scp079LurchPayload::new);

    @Override
    public Type<Scp079LurchPayload> type() {
        return PayloadType;
    }
}
