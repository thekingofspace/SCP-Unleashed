package net.rai.scp_unleashed.scp131.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp131GriefPayload(long Until) implements CustomPacketPayload {
    public static final Type<Scp131GriefPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp131_grief"));
    public static final StreamCodec<ByteBuf, Scp131GriefPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, Scp131GriefPayload::Until,
            Scp131GriefPayload::new);

    @Override
    public Type<Scp131GriefPayload> type() {
        return PayloadType;
    }
}
