package net.rai.scp_unleashed.archive.document;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.investigation.SiteCondition;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.Optional;

public record AnomalyDocument(String Lang, DocumentClass Classification, int Danger, SpawnRule Spawn, float Weight, Optional<ResourceLocation> Photo,
                              StartCondition Condition, ArchivePool Pool, LegacyText Legacy, Optional<String> Added) {
    public static final int MinDanger = 1;
    public static final int MaxDanger = 5;
    public static final String LangPrefix = "dossier";
    public static final String AddedField = "added";
    public static final String FallbackVersion = "0.0.0";
    public static final Codec<AnomalyDocument> DocumentCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.STRING.optionalFieldOf("lang", "").forGetter(AnomalyDocument::Lang),
            DocumentClass.ClassCodec.fieldOf("classification").forGetter(AnomalyDocument::Classification),
            Codec.intRange(MinDanger, MaxDanger).fieldOf("danger").forGetter(AnomalyDocument::Danger),
            SpawnRule.RuleCodec.optionalFieldOf("spawn", SpawnRule.Once).forGetter(AnomalyDocument::Spawn),
            Codec.floatRange(0.0f, 1000.0f).optionalFieldOf("weight", 1.0f).forGetter(AnomalyDocument::Weight),
            ResourceLocation.CODEC.optionalFieldOf("photo").forGetter(AnomalyDocument::Photo),
            StartConditions.ConditionCodec.optionalFieldOf("start_condition", new SiteCondition(300, 1500, 24, 40)).forGetter(AnomalyDocument::Condition),
            ArchivePool.PoolCodec.optionalFieldOf("archive_pool", ArchivePool.Normal).forGetter(AnomalyDocument::Pool),
            LegacyText.LegacyCodec.forGetter(AnomalyDocument::Legacy),
            Codec.STRING.optionalFieldOf(AddedField).forGetter(AnomalyDocument::Added)
    ).apply(Instance, AnomalyDocument::new));

    public static String defaultLang(ResourceLocation Id) {
        return LangPrefix + "." + Id.getNamespace() + "." + Id.getPath();
    }

    public AnomalyDocument withLang(String Base) {
        return new AnomalyDocument(Base, Classification, Danger, Spawn, Weight, Photo, Condition, Pool, Legacy, Added);
    }

    public String addedVersion() {
        return Added.filter(Version -> !Version.isBlank()).orElseGet(AnomalyDocument::currentVersion);
    }

    public boolean isNew() {
        return addedVersion().equals(currentVersion());
    }

    public static String currentVersion() {
        try {
            return ModList.get().getModContainerById(Scp_unleashed.ModId)
                    .map(Container -> Container.getModInfo().getVersion().toString()).orElse(FallbackVersion);
        } catch (RuntimeException Failure) {
            return FallbackVersion;
        }
    }

    public String key(String Field) {
        return Lang + "." + Field;
    }

    public boolean isListed() {
        return Pool == ArchivePool.Normal;
    }

    public Component designation() {
        return Legacy.Designation().<Component>map(Fallback -> Component.translatableWithFallback(key(DossierContent.DesignationField), Fallback))
                .orElseGet(() -> Component.translatable(key(DossierContent.DesignationField)));
    }

    public Component title() {
        return Legacy.Title().<Component>map(Fallback -> Component.translatableWithFallback(key(DossierContent.TitleField), Fallback))
                .orElseGet(() -> Component.translatable(key(DossierContent.TitleField)));
    }

    public Component objectClass() {
        String Key = key(DossierContent.ClassField);
        if (Language.getInstance().has(Key)) {
            return Component.translatable(Key);
        }
        return Legacy.Dossier().flatMap(DossierText::ObjectClass).<Component>map(Component::literal).orElseGet(Classification::getName);
    }

    public Component dangerNumeral() {
        return Component.translatable("archive.scp_unleashed.tier." + Danger);
    }

    public Optional<ResourceLocation> photoTexture() {
        return Photo.map(Sprite -> Sprite.withPath(Path -> "textures/" + Path + ".png"));
    }
}
