package net.rai.scp_unleashed.scp001day.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp001DayTurningPayload(int EntityId, float Progress) implements CustomPacketPayload {
    public static final Type<Scp001DayTurningPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp001_day_turning"));
    public static final StreamCodec<ByteBuf, Scp001DayTurningPayload> PayloadCodec = StreamCodec.composite(ByteBufCodecs.VAR_INT, Scp001DayTurningPayload::EntityId, ByteBufCodecs.FLOAT,
            Scp001DayTurningPayload::Progress, Scp001DayTurningPayload::new);

    @Override
    public Type<Scp001DayTurningPayload> type() {
        return PayloadType;
    }
}
