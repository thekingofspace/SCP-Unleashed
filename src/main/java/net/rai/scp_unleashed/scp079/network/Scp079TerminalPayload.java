package net.rai.scp_unleashed.scp079.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public record Scp079TerminalPayload(BlockPos Pos, boolean Upgraded, int Mood, boolean Opening, List<Component> Lines) implements CustomPacketPayload {
    public static final int MaxLines = 8;
    public static final Type<Scp079TerminalPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp079_terminal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Scp079TerminalPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Scp079TerminalPayload::Pos,
            ByteBufCodecs.BOOL, Scp079TerminalPayload::Upgraded,
            ByteBufCodecs.VAR_INT, Scp079TerminalPayload::Mood,
            ByteBufCodecs.BOOL, Scp079TerminalPayload::Opening,
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(MaxLines)), Scp079TerminalPayload::Lines,
            Scp079TerminalPayload::new);

    @Override
    public Type<Scp079TerminalPayload> type() {
        return PayloadType;
    }
}
