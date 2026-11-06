package net.rai.scp_unleashed.radio.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record RadioTunePayload(int Step) implements CustomPacketPayload {
    public static final Type<RadioTunePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "radio_tune"));
    public static final StreamCodec<ByteBuf, RadioTunePayload> PayloadCodec = ByteBufCodecs.VAR_INT.map(RadioTunePayload::new, RadioTunePayload::Step);

    @Override
    public Type<RadioTunePayload> type() {
        return PayloadType;
    }
}
