package net.rai.scp_unleashed.cameras.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record CameraEditPayload(int Hand, int Index, boolean Remove, String Name) implements CustomPacketPayload {
    public static final Type<CameraEditPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "camera_edit"));
    public static final StreamCodec<ByteBuf, CameraEditPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CameraEditPayload::Hand,
            ByteBufCodecs.VAR_INT, CameraEditPayload::Index,
            ByteBufCodecs.BOOL, CameraEditPayload::Remove,
            ByteBufCodecs.stringUtf8(64), CameraEditPayload::Name,
            CameraEditPayload::new);

    @Override
    public Type<CameraEditPayload> type() {
        return PayloadType;
    }
}
