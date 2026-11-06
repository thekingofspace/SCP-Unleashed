package net.rai.scp_unleashed.labware;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record AssayRecord(boolean Labelled, Component Subject, List<ResourceLocation> Findings, long Sampled, long Printed) {
    public static final Codec<AssayRecord> AssayCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.BOOL.optionalFieldOf("labelled", false).forGetter(AssayRecord::Labelled),
            ComponentSerialization.CODEC.optionalFieldOf("subject", Component.empty()).forGetter(AssayRecord::Subject),
            ResourceLocation.CODEC.sizeLimitedListOf(LabSwabRecord.MaxFindings).optionalFieldOf("findings", List.of()).forGetter(AssayRecord::Findings),
            Codec.LONG.optionalFieldOf("sampled", 0L).forGetter(AssayRecord::Sampled),
            Codec.LONG.optionalFieldOf("printed", 0L).forGetter(AssayRecord::Printed)
    ).apply(Instance, AssayRecord::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssayRecord> AssayStream = StreamCodec.composite(
            ByteBufCodecs.BOOL, AssayRecord::Labelled,
            ComponentSerialization.STREAM_CODEC, AssayRecord::Subject,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(LabSwabRecord.MaxFindings)), AssayRecord::Findings,
            ByteBufCodecs.VAR_LONG, AssayRecord::Sampled,
            ByteBufCodecs.VAR_LONG, AssayRecord::Printed,
            AssayRecord::new);

    public AssayRecord {
        Findings = List.copyOf(Findings.size() > LabSwabRecord.MaxFindings ? Findings.subList(0, LabSwabRecord.MaxFindings) : Findings);
    }

    public static AssayRecord of(LabSwabRecord Swab, long Printed) {
        return new AssayRecord(Swab.Labelled(), Swab.Subject(), Swab.Findings(), Swab.Taken(), Printed);
    }

    public boolean isPositive() {
        return !Findings.isEmpty();
    }

    public Component subjectName() {
        return Subject.getString().isEmpty() ? Component.translatable("labware.scp_unleashed.subject.unknown") : Subject;
    }
}
