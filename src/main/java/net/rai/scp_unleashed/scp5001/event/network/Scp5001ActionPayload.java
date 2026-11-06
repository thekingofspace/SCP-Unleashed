package net.rai.scp_unleashed.scp5001.event.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;

public record Scp5001ActionPayload(BlockPos Pos, int Action) implements CustomPacketPayload {
    public static final int CoverAction = 0;
    public static final int ToggleAction = 1;
    public static final int OverdriveAction = 2;
    public static final int ResetAction = 3;
    public static final Type<Scp5001ActionPayload> PayloadType = new Type<>(Scp5001EventRegistry.location("scp5001_event_action"));
    public static final StreamCodec<ByteBuf, Scp5001ActionPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Scp5001ActionPayload::Pos,
            ByteBufCodecs.VAR_INT, Scp5001ActionPayload::Action,
            Scp5001ActionPayload::new);

    @Override
    public Type<Scp5001ActionPayload> type() {
        return PayloadType;
    }
}
