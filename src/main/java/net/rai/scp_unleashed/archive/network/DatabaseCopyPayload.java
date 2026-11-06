package net.rai.scp_unleashed.archive.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record DatabaseCopyPayload(BlockPos Pos, ResourceLocation Document) implements CustomPacketPayload {
    public static final Type<DatabaseCopyPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "archive_database_copy"));
    public static final StreamCodec<ByteBuf, DatabaseCopyPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DatabaseCopyPayload::Pos,
            ResourceLocation.STREAM_CODEC, DatabaseCopyPayload::Document,
            DatabaseCopyPayload::new);

    @Override
    public Type<DatabaseCopyPayload> type() {
        return PayloadType;
    }
}
