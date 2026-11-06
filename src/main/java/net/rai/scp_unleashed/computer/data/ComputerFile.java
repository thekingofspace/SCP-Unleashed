package net.rai.scp_unleashed.computer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.database.DatabaseCopyItem;
import net.rai.scp_unleashed.archive.database.DatabaseRecord;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public record ComputerFile(FileKind Kind, String Name, ResourceLocation Document, DocumentClass Classification, FieldNotes Notes, String Text) {
    public static final int MaxName = 24;
    public static final int MaxText = 600;
    public static final int MaxTextLines = 24;
    public static final ResourceLocation NoDocument = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "none");
    public static final Codec<ComputerFile> FileCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            FileKind.KindCodec.fieldOf("kind").forGetter(ComputerFile::Kind),
            Codec.STRING.optionalFieldOf("name", "").forGetter(ComputerFile::Name),
            ResourceLocation.CODEC.optionalFieldOf("document", NoDocument).forGetter(ComputerFile::Document),
            DocumentClass.ClassCodec.optionalFieldOf("classification", DocumentClass.Unknown).forGetter(ComputerFile::Classification),
            FieldNotes.NotesCodec.optionalFieldOf("notes", DatabaseRecord.NoNotes).forGetter(ComputerFile::Notes),
            Codec.STRING.optionalFieldOf("text", "").forGetter(ComputerFile::Text)
    ).apply(Instance, ComputerFile::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerFile> FileStreamCodec = StreamCodec.composite(
            FileKind.KindStreamCodec, ComputerFile::Kind,
            ByteBufCodecs.stringUtf8(MaxName * 4), ComputerFile::Name,
            ResourceLocation.STREAM_CODEC, ComputerFile::Document,
            DocumentClass.ClassStreamCodec, ComputerFile::Classification,
            FieldNotes.NotesStreamCodec, ComputerFile::Notes,
            ByteBufCodecs.stringUtf8(MaxText * 4), ComputerFile::Text,
            ComputerFile::new);

    public ComputerFile {
        Name = ComputerText.clean(Name, MaxName);
        Text = Kind == FileKind.Note ? ComputerText.cleanLines(Text, MaxText, MaxTextLines) : "";
        if (Name.isEmpty()) {
            Name = Kind == FileKind.Note ? "NOTE" : "FILE";
        }
    }

    public static ComputerFile dossier(ResourceLocation Document, DocumentClass Classification, FieldNotes Notes) {
        return new ComputerFile(FileKind.Dossier, designation(Document), Document, Classification, DatabaseRecord.snapshot(Notes), "");
    }

    public static ComputerFile note(String Name, String Text) {
        return new ComputerFile(FileKind.Note, Name, NoDocument, DocumentClass.Unknown, DatabaseRecord.NoNotes, Text);
    }

    public static ComputerFile image(String Name, ResourceLocation Picture) {
        return new ComputerFile(FileKind.Image, Name, Picture, DocumentClass.Unknown, DatabaseRecord.NoNotes, "");
    }

    public static ComputerFile of(DatabaseRecord Entry) {
        return dossier(Entry.Document(), Entry.Classification(), Entry.Notes());
    }

    @Nullable
    public static ComputerFile fromStack(ItemStack Stack) {
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        if (Stamp == null) {
            return null;
        }
        FieldNotes Notes = Stack.get(ArchiveRegistry.NotesComponent.get());
        return dossier(Stamp.Document(), Stamp.Classification(), Notes == null ? DatabaseRecord.NoNotes : Notes);
    }

    public static String designation(ResourceLocation Document) {
        AnomalyDocument Found = AnomalyDocuments.lookup(Document);
        return Found == null ? Document.getPath().toUpperCase(Locale.ROOT) : Found.designation().getString();
    }

    public boolean isDossier() {
        return Kind == FileKind.Dossier;
    }

    public boolean isImage() {
        return Kind == FileKind.Image;
    }

    public ComputerFile renamed(String NewName) {
        return new ComputerFile(Kind, NewName, Document, Classification, Notes, Text);
    }

    public ComputerFile edited(String NewName, String NewText) {
        return new ComputerFile(Kind, NewName, Document, Classification, Notes, NewText);
    }

    public boolean sameContent(ComputerFile Other) {
        if (Kind != Other.Kind) {
            return false;
        }
        return Kind == FileKind.Dossier ? Document.equals(Other.Document) && Notes.equals(Other.Notes) : Name.equals(Other.Name) && Text.equals(Other.Text);
    }

    public ItemStack readable() {
        return DatabaseCopyItem.create(new DatabaseRecord(Document, Classification, Notes, "", 0L));
    }
}
