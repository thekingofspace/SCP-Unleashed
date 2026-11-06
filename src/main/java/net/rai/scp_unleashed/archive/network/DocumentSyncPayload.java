package net.rai.scp_unleashed.archive.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;

import java.util.HashMap;
import java.util.Map;

public record DocumentSyncPayload(Map<ResourceLocation, AnomalyDocument> Documents) implements CustomPacketPayload {
    public static final Type<DocumentSyncPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "archive_documents"));
    public static final StreamCodec<ByteBuf, DocumentSyncPayload> PayloadCodec = ByteBufCodecs
            .<ByteBuf, ResourceLocation, AnomalyDocument, Map<ResourceLocation, AnomalyDocument>>map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.fromCodec(AnomalyDocument.DocumentCodec))
            .map(DocumentSyncPayload::new, DocumentSyncPayload::Documents);

    @Override
    public Type<DocumentSyncPayload> type() {
        return PayloadType;
    }
}
