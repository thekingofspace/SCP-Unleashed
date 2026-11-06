package net.rai.scp_unleashed.radio.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record RadioGlitchPayload(int Ticks, int Seed) implements CustomPacketPayload {
    public static final Type<RadioGlitchPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "radio_glitch"));
    public static final StreamCodec<ByteBuf, RadioGlitchPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RadioGlitchPayload::Ticks,
            ByteBufCodecs.VAR_INT, RadioGlitchPayload::Seed,
            RadioGlitchPayload::new);

    @Override
    public Type<RadioGlitchPayload> type() {
        return PayloadType;
    }
}
