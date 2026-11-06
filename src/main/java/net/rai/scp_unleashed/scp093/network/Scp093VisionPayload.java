package net.rai.scp_unleashed.scp093.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp093VisionPayload(float Doses, int Vision, int Claim) implements CustomPacketPayload {
    public static final Type<Scp093VisionPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp093_vision"));
    public static final StreamCodec<ByteBuf, Scp093VisionPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Scp093VisionPayload::Doses,
            ByteBufCodecs.VAR_INT, Scp093VisionPayload::Vision,
            ByteBufCodecs.INT, Scp093VisionPayload::Claim,
            Scp093VisionPayload::new);

    @Override
    public Type<Scp093VisionPayload> type() {
        return PayloadType;
    }
}
