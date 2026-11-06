package net.rai.scp_unleashed.archive.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record OpenDatabasePayload(BlockPos Pos, boolean Main) implements CustomPacketPayload {
    public static final Type<OpenDatabasePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "archive_database_open"));
    public static final StreamCodec<ByteBuf, OpenDatabasePayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenDatabasePayload::Pos,
            ByteBufCodecs.BOOL, OpenDatabasePayload::Main,
            OpenDatabasePayload::new);

    @Override
    public Type<OpenDatabasePayload> type() {
        return PayloadType;
    }
}
