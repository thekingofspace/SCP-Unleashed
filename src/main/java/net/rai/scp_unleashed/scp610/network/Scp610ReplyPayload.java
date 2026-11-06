package net.rai.scp_unleashed.scp610.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp610.Scp610Dialogue;

public record Scp610ReplyPayload(BlockPos Pos, String Option) implements CustomPacketPayload {
    public static final Type<Scp610ReplyPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_reply"));
    public static final StreamCodec<ByteBuf, Scp610ReplyPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Scp610ReplyPayload::Pos,
            ByteBufCodecs.stringUtf8(32), Scp610ReplyPayload::Option,
            Scp610ReplyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }

    public static void handle(Scp610ReplyPayload Payload, IPayloadContext Context) {
        Context.enqueueWork(() -> {
            if (Context.player() instanceof ServerPlayer User) {
                Scp610Dialogue.reply(User, Payload.Pos(), Payload.Option());
            }
        });
    }
}
