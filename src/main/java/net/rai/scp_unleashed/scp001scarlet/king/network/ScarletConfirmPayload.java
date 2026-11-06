package net.rai.scp_unleashed.scp001scarlet.king.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ScarletConfirmPayload(int Nonce, boolean Build) implements CustomPacketPayload {
    public static final Type<ScarletConfirmPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scarlet_king_confirm"));
    public static final StreamCodec<ByteBuf, ScarletConfirmPayload> PayloadCodec = StreamCodec.composite(ByteBufCodecs.VAR_INT, ScarletConfirmPayload::Nonce,
            ByteBufCodecs.BOOL, ScarletConfirmPayload::Build, ScarletConfirmPayload::new);

    @Override
    public Type<ScarletConfirmPayload> type() {
        return PayloadType;
    }
}
