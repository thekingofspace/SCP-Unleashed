package net.rai.scp_unleashed.z0002.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Z0002WarningPayload(int Ticks) implements CustomPacketPayload {
    public static final Type<Z0002WarningPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0002_warning"));
    public static final StreamCodec<ByteBuf, Z0002WarningPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Z0002WarningPayload::Ticks,
            Z0002WarningPayload::new);

    @Override
    public Type<Z0002WarningPayload> type() {
        return PayloadType;
    }
}
