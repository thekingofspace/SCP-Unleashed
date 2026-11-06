package net.rai.scp_unleashed.scp001scarlet.king.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ScarletAnswerPayload(int Nonce, boolean Yes) implements CustomPacketPayload {
    public static final Type<ScarletAnswerPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scarlet_king_answer"));
    public static final StreamCodec<ByteBuf, ScarletAnswerPayload> PayloadCodec = StreamCodec.composite(ByteBufCodecs.VAR_INT, ScarletAnswerPayload::Nonce,
            ByteBufCodecs.BOOL, ScarletAnswerPayload::Yes, ScarletAnswerPayload::new);

    @Override
    public Type<ScarletAnswerPayload> type() {
        return PayloadType;
    }
}
