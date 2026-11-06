package net.rai.scp_unleashed.stranger.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record StrangerDialoguePayload(int EntityId, String Variant, String Dialogue, int Pages) implements CustomPacketPayload {
    public static final Type<StrangerDialoguePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "stranger_dialogue"));
    public static final StreamCodec<ByteBuf, StrangerDialoguePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StrangerDialoguePayload::EntityId,
            ByteBufCodecs.STRING_UTF8, StrangerDialoguePayload::Variant,
            ByteBufCodecs.STRING_UTF8, StrangerDialoguePayload::Dialogue,
            ByteBufCodecs.VAR_INT, StrangerDialoguePayload::Pages,
            StrangerDialoguePayload::new);

    @Override
    public Type<StrangerDialoguePayload> type() {
        return PayloadType;
    }
}
