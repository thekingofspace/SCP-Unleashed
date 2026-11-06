package net.rai.scp_unleashed.scp323.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp323WatchPayload(int CreatureId) implements CustomPacketPayload {
    public static final int Nobody = -1;
    public static final Type<Scp323WatchPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp323_watch"));
    public static final StreamCodec<ByteBuf, Scp323WatchPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.INT, Scp323WatchPayload::CreatureId,
            Scp323WatchPayload::new);

    @Override
    public Type<Scp323WatchPayload> type() {
        return PayloadType;
    }
}
