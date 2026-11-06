package net.rai.scp_unleashed.cameras.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record CameraWindowPayload(boolean Open, int X, int Z) implements CustomPacketPayload {
    public static final CameraWindowPayload Closed = new CameraWindowPayload(false, 0, 0);
    public static final Type<CameraWindowPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "camera_window"));
    public static final StreamCodec<ByteBuf, CameraWindowPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, CameraWindowPayload::Open,
            ByteBufCodecs.VAR_INT, CameraWindowPayload::X,
            ByteBufCodecs.VAR_INT, CameraWindowPayload::Z,
            CameraWindowPayload::new);

    @Override
    public Type<CameraWindowPayload> type() {
        return PayloadType;
    }
}
