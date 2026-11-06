package net.rai.scp_unleashed.cameras.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record CameraLookPayload(float Pan, float Tilt) implements CustomPacketPayload {
    public static final Type<CameraLookPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "camera_look"));
    public static final StreamCodec<ByteBuf, CameraLookPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CameraLookPayload::Pan,
            ByteBufCodecs.FLOAT, CameraLookPayload::Tilt,
            CameraLookPayload::new);

    @Override
    public Type<CameraLookPayload> type() {
        return PayloadType;
    }
}
