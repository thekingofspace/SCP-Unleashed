package net.rai.scp_unleashed.security;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DetectorConfigPayload(BlockPos Pos, int Setting, int Value) implements CustomPacketPayload {
    public static final Type<DetectorConfigPayload> PayloadType = new Type<>(SecurityRegistry.location("security_detector_config"));
    public static final StreamCodec<ByteBuf, DetectorConfigPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DetectorConfigPayload::Pos,
            ByteBufCodecs.VAR_INT, DetectorConfigPayload::Setting,
            ByteBufCodecs.VAR_INT, DetectorConfigPayload::Value,
            DetectorConfigPayload::new);

    @Override
    public Type<DetectorConfigPayload> type() {
        return PayloadType;
    }

    public static void handle(DetectorConfigPayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer User && User.level().isLoaded(Payload.Pos()) && User.canInteractWithBlock(Payload.Pos(), 1.0)
                && User.level().getBlockEntity(Payload.Pos()) instanceof MotionDetectorBlockEntity Detector) {
            Detector.applySetting(Payload.Setting(), Payload.Value());
        }
    }
}
