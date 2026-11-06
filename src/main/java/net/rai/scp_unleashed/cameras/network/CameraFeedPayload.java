package net.rai.scp_unleashed.cameras.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record CameraFeedPayload(boolean Active, BlockPos Pos, boolean Ceiling, Direction Facing, float Pan, float Tilt, String Name, int Index, int Count) implements CustomPacketPayload {
    public static final CameraFeedPayload Closed = new CameraFeedPayload(false, BlockPos.ZERO, false, Direction.NORTH, 0.0f, 0.0f, "", 0, 0);
    public static final Type<CameraFeedPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "camera_feed"));
    public static final StreamCodec<ByteBuf, CameraFeedPayload> PayloadCodec = StreamCodec.of(CameraFeedPayload::write, CameraFeedPayload::read);

    private static void write(ByteBuf Buffer, CameraFeedPayload Payload) {
        ByteBufCodecs.BOOL.encode(Buffer, Payload.Active());
        BlockPos.STREAM_CODEC.encode(Buffer, Payload.Pos());
        ByteBufCodecs.BOOL.encode(Buffer, Payload.Ceiling());
        Direction.STREAM_CODEC.encode(Buffer, Payload.Facing());
        ByteBufCodecs.FLOAT.encode(Buffer, Payload.Pan());
        ByteBufCodecs.FLOAT.encode(Buffer, Payload.Tilt());
        ByteBufCodecs.stringUtf8(64).encode(Buffer, Payload.Name());
        ByteBufCodecs.VAR_INT.encode(Buffer, Payload.Index());
        ByteBufCodecs.VAR_INT.encode(Buffer, Payload.Count());
    }

    private static CameraFeedPayload read(ByteBuf Buffer) {
        return new CameraFeedPayload(ByteBufCodecs.BOOL.decode(Buffer), BlockPos.STREAM_CODEC.decode(Buffer), ByteBufCodecs.BOOL.decode(Buffer),
                Direction.STREAM_CODEC.decode(Buffer), ByteBufCodecs.FLOAT.decode(Buffer), ByteBufCodecs.FLOAT.decode(Buffer),
                ByteBufCodecs.stringUtf8(64).decode(Buffer), ByteBufCodecs.VAR_INT.decode(Buffer), ByteBufCodecs.VAR_INT.decode(Buffer));
    }

    @Override
    public Type<CameraFeedPayload> type() {
        return PayloadType;
    }
}
