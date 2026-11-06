package net.rai.scp_unleashed.scp085.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp085ResidentPayload(boolean Resident) implements CustomPacketPayload {
    public static final Type<Scp085ResidentPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp085_resident"));
    public static final StreamCodec<ByteBuf, Scp085ResidentPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp085ResidentPayload::Resident,
            Scp085ResidentPayload::new);

    @Override
    public Type<Scp085ResidentPayload> type() {
        return PayloadType;
    }
}
