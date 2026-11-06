package net.rai.scp_unleashed.speaker.cassette;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record CassetteLibraryPayload(CassetteLibrary Library) implements CustomPacketPayload {
    public static final Type<CassetteLibraryPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "speaker_library"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CassetteLibraryPayload> PayloadCodec = CassetteLibrary.LibraryStreamCodec
            .map(CassetteLibraryPayload::new, CassetteLibraryPayload::Library);

    @Override
    public Type<CassetteLibraryPayload> type() {
        return PayloadType;
    }
}
