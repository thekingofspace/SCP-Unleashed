package net.rai.scp_unleashed.warhead.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record WarheadAlertPayload(BlockPos Source, int TicksLeft, boolean Locked, boolean Active, boolean Danger) implements CustomPacketPayload {
    public static final Type<WarheadAlertPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "warhead_alert"));
    public static final StreamCodec<ByteBuf, WarheadAlertPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WarheadAlertPayload::Source,
            ByteBufCodecs.VAR_INT, WarheadAlertPayload::TicksLeft,
            ByteBufCodecs.BOOL, WarheadAlertPayload::Locked,
            ByteBufCodecs.BOOL, WarheadAlertPayload::Active,
            ByteBufCodecs.BOOL, WarheadAlertPayload::Danger,
            WarheadAlertPayload::new);

    @Override
    public Type<WarheadAlertPayload> type() {
        return PayloadType;
    }
}
