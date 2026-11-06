package net.rai.scp_unleashed.archive.document;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

public record DossierText(Optional<String> ItemNumber, Optional<String> ObjectClass, String Containment, String Description, List<Addendum> Addenda) {
    public static final Codec<DossierText> TextCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.STRING.optionalFieldOf("item").forGetter(DossierText::ItemNumber),
            Codec.STRING.optionalFieldOf("object_class").forGetter(DossierText::ObjectClass),
            Codec.STRING.fieldOf("containment").forGetter(DossierText::Containment),
            Codec.STRING.fieldOf("description").forGetter(DossierText::Description),
            Addendum.AddendumCodec.listOf().optionalFieldOf("addenda", List.of()).forGetter(DossierText::Addenda)
    ).apply(Instance, DossierText::new));

    public record Addendum(String Title, String Text) {
        public static final Codec<Addendum> AddendumCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Codec.STRING.fieldOf("title").forGetter(Addendum::Title),
                Codec.STRING.fieldOf("text").forGetter(Addendum::Text)
        ).apply(Instance, Addendum::new));
    }
}
