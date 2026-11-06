package net.rai.scp_unleashed.scp610.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public record Scp610DialoguePayload(BlockPos Pos, String Mood, List<String> Lines, List<String> Options) implements CustomPacketPayload {
    public static final Type<Scp610DialoguePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_dialogue"));
    public static final StreamCodec<ByteBuf, Scp610DialoguePayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Scp610DialoguePayload::Pos,
            ByteBufCodecs.stringUtf8(32), Scp610DialoguePayload::Mood,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), Scp610DialoguePayload::Lines,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)), Scp610DialoguePayload::Options,
            Scp610DialoguePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }
}
