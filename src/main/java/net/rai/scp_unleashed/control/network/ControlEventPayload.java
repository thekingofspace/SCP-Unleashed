package net.rai.scp_unleashed.control.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ControlEventPayload(byte Kind) implements CustomPacketPayload {
    public static final byte Linked = 0;
    public static final byte Released = 1;
    public static final byte Severed = 2;
    public static final Type<ControlEventPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "control_event"));
    public static final StreamCodec<ByteBuf, ControlEventPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BYTE, ControlEventPayload::Kind,
            ControlEventPayload::new);

    @Override
    public Type<ControlEventPayload> type() {
        return PayloadType;
    }
}
