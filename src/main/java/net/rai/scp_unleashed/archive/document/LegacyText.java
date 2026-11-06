package net.rai.scp_unleashed.archive.document;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record LegacyText(Optional<String> Designation, Optional<String> Title, Optional<String> Caption, Optional<DossierText> Dossier) {
    public static final LegacyText None = new LegacyText(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    public static final MapCodec<LegacyText> LegacyCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.STRING.optionalFieldOf("designation").forGetter(LegacyText::Designation),
            Codec.STRING.optionalFieldOf("title").forGetter(LegacyText::Title),
            Codec.STRING.optionalFieldOf("photo_caption").forGetter(LegacyText::Caption),
            DossierText.TextCodec.optionalFieldOf("dossier").forGetter(LegacyText::Dossier)
    ).apply(Instance, LegacyText::new));

    public boolean isEmpty() {
        return Designation.isEmpty() && Title.isEmpty() && Caption.isEmpty() && Dossier.isEmpty();
    }
}
