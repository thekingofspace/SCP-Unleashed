package net.rai.scp_unleashed.scp087.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp087StatePayload(boolean Inside, int Flights, boolean Inert) implements CustomPacketPayload {
    public static final Type<Scp087StatePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp087_state"));
    public static final StreamCodec<ByteBuf, Scp087StatePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp087StatePayload::Inside,
            ByteBufCodecs.VAR_INT, Scp087StatePayload::Flights,
            ByteBufCodecs.BOOL, Scp087StatePayload::Inert,
            Scp087StatePayload::new);

    @Override
    public Type<Scp087StatePayload> type() {
        return PayloadType;
    }
}
