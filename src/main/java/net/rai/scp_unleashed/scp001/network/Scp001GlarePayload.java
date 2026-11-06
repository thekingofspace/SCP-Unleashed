package net.rai.scp_unleashed.scp001.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp001GlarePayload(float Strength, int Ticks) implements CustomPacketPayload {
    public static final Type<Scp001GlarePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp001_glare"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Scp001GlarePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Scp001GlarePayload::Strength,
            ByteBufCodecs.VAR_INT, Scp001GlarePayload::Ticks,
            Scp001GlarePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }
}
