package net.rai.scp_unleashed.scp372.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp372.Scp372Registry;

public record Scp372HushPayload(boolean Held) implements CustomPacketPayload {
    public static final Type<Scp372HushPayload> PayloadType = new Type<>(Scp372Registry.location("scp372_hush"));
    public static final StreamCodec<ByteBuf, Scp372HushPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp372HushPayload::Held,
            Scp372HushPayload::new);

    @Override
    public Type<Scp372HushPayload> type() {
        return PayloadType;
    }
}
