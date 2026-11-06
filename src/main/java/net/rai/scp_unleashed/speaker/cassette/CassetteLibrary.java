package net.rai.scp_unleashed.speaker.cassette;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record CassetteLibrary(List<CassetteRecording> Recordings) {
    public static final int MaxRecordings = 256;
    public static final CassetteLibrary Empty = new CassetteLibrary(List.of());
    public static final Codec<CassetteLibrary> LibraryCodec = CassetteRecording.RecordingCodec.listOf()
            .xmap(Saved -> new CassetteLibrary(Saved.size() > MaxRecordings ? Saved.subList(0, MaxRecordings) : Saved), CassetteLibrary::Recordings);
    public static final StreamCodec<RegistryFriendlyByteBuf, CassetteLibrary> LibraryStreamCodec = CassetteRecording.RecordingStreamCodec
            .apply(ByteBufCodecs.list(MaxRecordings)).map(CassetteLibrary::new, CassetteLibrary::Recordings);

    public CassetteLibrary {
        Recordings = List.copyOf(Recordings);
    }

    public Optional<CassetteRecording> find(ResourceLocation Sound) {
        for (CassetteRecording Recording : Recordings) {
            if (Recording.Sound().equals(Sound)) {
                return Optional.of(Recording);
            }
        }
        return Optional.empty();
    }

    public boolean contains(ResourceLocation Sound) {
        return find(Sound).isPresent();
    }

    public boolean isFull() {
        return Recordings.size() >= MaxRecordings;
    }

    public CassetteLibrary with(CassetteRecording Recording) {
        List<CassetteRecording> Grown = new ArrayList<>(Recordings);
        Grown.add(Recording);
        return new CassetteLibrary(Grown);
    }
}
