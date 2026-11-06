package net.rai.scp_unleashed.scp323.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp323BondPayload(int EntityId, int Stage) implements CustomPacketPayload {
    public static final Type<Scp323BondPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp323_bond"));
    public static final StreamCodec<ByteBuf, Scp323BondPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp323BondPayload::EntityId,
            ByteBufCodecs.VAR_INT, Scp323BondPayload::Stage,
            Scp323BondPayload::new);

    @Override
    public Type<Scp323BondPayload> type() {
        return PayloadType;
    }
}
