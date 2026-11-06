package net.rai.scp_unleashed.archive.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.database.DatabaseRecord;

import java.util.List;

public record DatabaseSyncPayload(List<DatabaseRecord> Records) implements CustomPacketPayload {
    public static final Type<DatabaseSyncPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "archive_database_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseSyncPayload> PayloadCodec = StreamCodec.composite(
            DatabaseRecord.RecordStreamCodec.apply(ByteBufCodecs.list()), DatabaseSyncPayload::Records,
            DatabaseSyncPayload::new);

    @Override
    public Type<DatabaseSyncPayload> type() {
        return PayloadType;
    }
}
