package net.rai.scp_unleashed.cameras.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record CameraOpenPayload(int Hand, int Index) implements CustomPacketPayload {
    public static final Type<CameraOpenPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "camera_open"));
    public static final StreamCodec<ByteBuf, CameraOpenPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CameraOpenPayload::Hand,
            ByteBufCodecs.VAR_INT, CameraOpenPayload::Index,
            CameraOpenPayload::new);

    @Override
    public Type<CameraOpenPayload> type() {
        return PayloadType;
    }
}
