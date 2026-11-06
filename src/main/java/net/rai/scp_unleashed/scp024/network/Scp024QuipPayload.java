package net.rai.scp_unleashed.scp024.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp024QuipPayload(Component Text, int Ticks) implements CustomPacketPayload {
    public static final Type<Scp024QuipPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_quip"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Scp024QuipPayload> PayloadCodec = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, Scp024QuipPayload::Text,
            ByteBufCodecs.VAR_INT, Scp024QuipPayload::Ticks,
            Scp024QuipPayload::new);

    @Override
    public Type<Scp024QuipPayload> type() {
        return PayloadType;
    }
}
