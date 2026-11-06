package net.rai.scp_unleashed.scp303.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp303.Scp303Registry;

public record Scp303UnlockPayload(boolean Unlocked) implements CustomPacketPayload {
    public static final Type<Scp303UnlockPayload> PayloadType = new Type<>(Scp303Registry.location("scp303_unlock"));
    public static final StreamCodec<ByteBuf, Scp303UnlockPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp303UnlockPayload::Unlocked,
            Scp303UnlockPayload::new);

    @Override
    public Type<Scp303UnlockPayload> type() {
        return PayloadType;
    }
}
