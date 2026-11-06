package net.rai.scp_unleashed.notice;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record NoticePayload(Notice Shown) implements CustomPacketPayload {
    public static final Type<NoticePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "notice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NoticePayload> PayloadCodec = Notice.NoticeCodec.map(NoticePayload::new, NoticePayload::Shown);

    @Override
    public Type<NoticePayload> type() {
        return PayloadType;
    }
}
