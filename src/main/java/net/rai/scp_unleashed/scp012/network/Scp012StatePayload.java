package net.rai.scp_unleashed.scp012.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp012StatePayload(boolean Composer, boolean Exposed, int Meter) implements CustomPacketPayload {
    public static final Type<Scp012StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp012_state"));
    public static final StreamCodec<ByteBuf, Scp012StatePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp012StatePayload::Composer,
            ByteBufCodecs.BOOL, Scp012StatePayload::Exposed,
            ByteBufCodecs.VAR_INT, Scp012StatePayload::Meter,
            Scp012StatePayload::new);

    @Override
    public Type<Scp012StatePayload> type() {
        return PayloadType;
    }
}
