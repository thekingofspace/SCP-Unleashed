package net.rai.scp_unleashed.scp178.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp178ReportPayload(byte Kind, int Id) implements CustomPacketPayload {
    public static final byte Stared = 0;
    public static final byte Struck = 1;
    public static final Type<Scp178ReportPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp178_report"));
    public static final StreamCodec<ByteBuf, Scp178ReportPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BYTE, Scp178ReportPayload::Kind,
            ByteBufCodecs.VAR_INT, Scp178ReportPayload::Id,
            Scp178ReportPayload::new);

    @Override
    public Type<Scp178ReportPayload> type() {
        return PayloadType;
    }
}
