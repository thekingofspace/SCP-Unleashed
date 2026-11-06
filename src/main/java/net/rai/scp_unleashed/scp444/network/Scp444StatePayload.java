package net.rai.scp_unleashed.scp444.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp444StatePayload(boolean Silenced) implements CustomPacketPayload {
    public static final Type<Scp444StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp444_state"));
    public static final StreamCodec<ByteBuf, Scp444StatePayload> PayloadCodec = StreamCodec.composite(ByteBufCodecs.BOOL, Scp444StatePayload::Silenced, Scp444StatePayload::new);

    @Override
    public Type<Scp444StatePayload> type() {
        return PayloadType;
    }
}
