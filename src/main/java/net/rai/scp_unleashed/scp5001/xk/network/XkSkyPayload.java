package net.rai.scp_unleashed.scp5001.xk.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkRegistry;

public record XkSkyPayload(float Sky, boolean Snap) implements CustomPacketPayload {
    public static final Type<XkSkyPayload> PayloadType = new Type<>(Scp5001XkRegistry.location("scp5001_xk_sky"));
    public static final StreamCodec<ByteBuf, XkSkyPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, XkSkyPayload::Sky,
            ByteBufCodecs.BOOL, XkSkyPayload::Snap,
            XkSkyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }
}
