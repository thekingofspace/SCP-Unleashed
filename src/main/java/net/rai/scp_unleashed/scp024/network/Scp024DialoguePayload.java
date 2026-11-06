package net.rai.scp_unleashed.scp024.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public record Scp024DialoguePayload(int Token, List<Component> Pages, List<Integer> Cues) implements CustomPacketPayload {
    public static final Type<Scp024DialoguePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_dialogue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Scp024DialoguePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp024DialoguePayload::Token,
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(16)), Scp024DialoguePayload::Pages,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(16)), Scp024DialoguePayload::Cues,
            Scp024DialoguePayload::new);

    @Override
    public Type<Scp024DialoguePayload> type() {
        return PayloadType;
    }
}
