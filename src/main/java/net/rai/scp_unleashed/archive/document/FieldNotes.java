package net.rai.scp_unleashed.archive.document;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.locale.Language;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record FieldNotes(List<Component> Lines, boolean Resolved, Optional<UUID> CaseId) {
    public static final int MaxLines = 8;
    public static final Codec<FieldNotes> NotesCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ComponentSerialization.CODEC.listOf().optionalFieldOf("lines", List.of()).forGetter(FieldNotes::Lines),
            Codec.BOOL.optionalFieldOf("resolved", false).forGetter(FieldNotes::Resolved),
            UUIDUtil.CODEC.optionalFieldOf("case").forGetter(FieldNotes::CaseId)
    ).apply(Instance, FieldNotes::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FieldNotes> NotesStreamCodec = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(MaxLines)), FieldNotes::Lines,
            ByteBufCodecs.BOOL, FieldNotes::Resolved,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), FieldNotes::CaseId,
            FieldNotes::new);

    public FieldNotes {
        Lines = List.copyOf(Lines.size() > MaxLines ? Lines.subList(0, MaxLines) : Lines);
    }

    public List<Component> readableLines() {
        return Lines.stream().filter(FieldNotes::isReadable).toList();
    }

    private static boolean isReadable(Component Line) {
        return !(Line.getContents() instanceof TranslatableContents Translated) || Translated.getFallback() != null || Language.getInstance().has(Translated.getKey());
    }

    public FieldNotes resolve() {
        return new FieldNotes(Lines, true, CaseId);
    }

    public boolean belongsTo(UUID Case) {
        return CaseId.isEmpty() || CaseId.get().equals(Case);
    }
}
