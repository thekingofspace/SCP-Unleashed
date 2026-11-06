package net.rai.scp_unleashed.scp079.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp079AskPayload(BlockPos Pos, int Prompt) implements CustomPacketPayload {
    public static final Type<Scp079AskPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp079_ask"));
    public static final StreamCodec<ByteBuf, Scp079AskPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Scp079AskPayload::Pos,
            ByteBufCodecs.VAR_INT, Scp079AskPayload::Prompt,
            Scp079AskPayload::new);

    @Override
    public Type<Scp079AskPayload> type() {
        return PayloadType;
    }
}
