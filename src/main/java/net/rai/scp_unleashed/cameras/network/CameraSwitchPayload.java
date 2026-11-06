package net.rai.scp_unleashed.cameras.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record CameraSwitchPayload(int Step) implements CustomPacketPayload {
    public static final Type<CameraSwitchPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "camera_switch"));
    public static final StreamCodec<ByteBuf, CameraSwitchPayload> PayloadCodec = ByteBufCodecs.VAR_INT.map(CameraSwitchPayload::new, CameraSwitchPayload::Step);

    @Override
    public Type<CameraSwitchPayload> type() {
        return PayloadType;
    }
}
