package net.rai.scp_unleashed.scp372.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp372.Scp372Registry;

public record Scp372UnlockPayload(boolean Unlocked) implements CustomPacketPayload {
    public static final Type<Scp372UnlockPayload> PayloadType = new Type<>(Scp372Registry.location("scp372_unlock"));
    public static final StreamCodec<ByteBuf, Scp372UnlockPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp372UnlockPayload::Unlocked,
            Scp372UnlockPayload::new);

    @Override
    public Type<Scp372UnlockPayload> type() {
        return PayloadType;
    }
}
