package net.rai.scp_unleashed.scp055.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp055WakePayload(boolean Interrupted) implements CustomPacketPayload {
    public static final Type<Scp055WakePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp055_wake"));
    public static final StreamCodec<ByteBuf, Scp055WakePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp055WakePayload::Interrupted,
            Scp055WakePayload::new);

    @Override
    public Type<Scp055WakePayload> type() {
        return PayloadType;
    }
}
