package net.rai.scp_unleashed.computer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ComputerNoticePayload(BlockPos Pos, Component Text, int Tone) implements CustomPacketPayload {
    public static final int Info = 0;
    public static final int Error = 1;
    public static final int Chime = 2;
    public static final Type<ComputerNoticePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "computer_notice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerNoticePayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComputerNoticePayload::Pos,
            ComponentSerialization.STREAM_CODEC, ComputerNoticePayload::Text,
            ByteBufCodecs.VAR_INT, ComputerNoticePayload::Tone,
            ComputerNoticePayload::new);

    @Override
    public Type<ComputerNoticePayload> type() {
        return PayloadType;
    }
}
