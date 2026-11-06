package net.rai.scp_unleashed.scp610.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp610StatePayload(int EntityId, int Stage) implements CustomPacketPayload {
    public static final Type<Scp610StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_state"));
    public static final StreamCodec<ByteBuf, Scp610StatePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp610StatePayload::EntityId,
            ByteBufCodecs.VAR_INT, Scp610StatePayload::Stage,
            Scp610StatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }
}
