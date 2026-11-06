package net.rai.scp_unleashed.scp465.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp465LetterPayload(int Line, String Name) implements CustomPacketPayload {
    public static final Type<Scp465LetterPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp465_letter"));
    public static final StreamCodec<ByteBuf, Scp465LetterPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp465LetterPayload::Line,
            ByteBufCodecs.stringUtf8(64), Scp465LetterPayload::Name,
            Scp465LetterPayload::new);

    @Override
    public Type<Scp465LetterPayload> type() {
        return PayloadType;
    }
}
