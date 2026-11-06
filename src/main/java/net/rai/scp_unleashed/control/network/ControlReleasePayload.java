package net.rai.scp_unleashed.control.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ControlReleasePayload() implements CustomPacketPayload {
    public static final ControlReleasePayload Instance = new ControlReleasePayload();
    public static final Type<ControlReleasePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "control_release"));
    public static final StreamCodec<ByteBuf, ControlReleasePayload> PayloadCodec = StreamCodec.unit(Instance);

    @Override
    public Type<ControlReleasePayload> type() {
        return PayloadType;
    }
}
