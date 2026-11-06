package net.rai.scp_unleashed.warhead.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record WarheadAnnouncePayload(int Kind, int Seconds) implements CustomPacketPayload {
    public static final int Start = 0;
    public static final int Remaining = 1;
    public static final int Final = 2;
    public static final int Cancelled = 3;
    public static final int Detonation = 4;
    public static final int Failed = 5;
    public static final Type<WarheadAnnouncePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "warhead_announce"));
    public static final StreamCodec<ByteBuf, WarheadAnnouncePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WarheadAnnouncePayload::Kind,
            ByteBufCodecs.VAR_INT, WarheadAnnouncePayload::Seconds,
            WarheadAnnouncePayload::new);

    @Override
    public Type<WarheadAnnouncePayload> type() {
        return PayloadType;
    }
}
