package net.rai.scp_unleashed.archive.database;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.FieldNotes;

import java.util.List;
import java.util.Optional;

public record DatabaseRecord(ResourceLocation Document, DocumentClass Classification, FieldNotes Notes, String Archivist, long Day) {
    public static final FieldNotes NoNotes = new FieldNotes(List.of(), false, Optional.empty());
    public static final Codec<DatabaseRecord> RecordCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ResourceLocation.CODEC.fieldOf("document").forGetter(DatabaseRecord::Document),
            DocumentClass.ClassCodec.fieldOf("classification").forGetter(DatabaseRecord::Classification),
            FieldNotes.NotesCodec.optionalFieldOf("notes", NoNotes).forGetter(DatabaseRecord::Notes),
            Codec.STRING.optionalFieldOf("archivist", "").forGetter(DatabaseRecord::Archivist),
            Codec.LONG.optionalFieldOf("day", 0L).forGetter(DatabaseRecord::Day)
    ).apply(Instance, DatabaseRecord::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DatabaseRecord> RecordStreamCodec = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, DatabaseRecord::Document,
            DocumentClass.ClassStreamCodec, DatabaseRecord::Classification,
            FieldNotes.NotesStreamCodec, DatabaseRecord::Notes,
            ByteBufCodecs.STRING_UTF8, DatabaseRecord::Archivist,
            ByteBufCodecs.VAR_LONG, DatabaseRecord::Day,
            DatabaseRecord::new);

    public static FieldNotes snapshot(FieldNotes Notes) {
        return Notes == null ? NoNotes : new FieldNotes(Notes.Lines(), Notes.Resolved(), Optional.empty());
    }
}
