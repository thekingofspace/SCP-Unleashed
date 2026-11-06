package net.rai.scp_unleashed.cameras.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record CameraExitPayload() implements CustomPacketPayload {
    public static final CameraExitPayload Instance = new CameraExitPayload();
    public static final Type<CameraExitPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "camera_exit"));
    public static final StreamCodec<ByteBuf, CameraExitPayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<CameraExitPayload> type() {
        return PayloadType;
    }
}
