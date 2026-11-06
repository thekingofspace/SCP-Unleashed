package net.rai.scp_unleashed.labware;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record LabSwabRecord(boolean Labelled, Component Subject, List<ResourceLocation> Findings, long Taken) {
    public static final int MaxFindings = 64;
    public static final Codec<LabSwabRecord> RecordCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.BOOL.optionalFieldOf("labelled", false).forGetter(LabSwabRecord::Labelled),
            ComponentSerialization.CODEC.optionalFieldOf("subject", Component.empty()).forGetter(LabSwabRecord::Subject),
            ResourceLocation.CODEC.sizeLimitedListOf(MaxFindings).optionalFieldOf("findings", List.of()).forGetter(LabSwabRecord::Findings),
            Codec.LONG.optionalFieldOf("taken", 0L).forGetter(LabSwabRecord::Taken)
    ).apply(Instance, LabSwabRecord::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, LabSwabRecord> RecordStream = StreamCodec.composite(
            ByteBufCodecs.BOOL, LabSwabRecord::Labelled,
            ComponentSerialization.STREAM_CODEC, LabSwabRecord::Subject,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MaxFindings)), LabSwabRecord::Findings,
            ByteBufCodecs.VAR_LONG, LabSwabRecord::Taken,
            LabSwabRecord::new);

    public LabSwabRecord {
        Findings = List.copyOf(Findings.size() > MaxFindings ? Findings.subList(0, MaxFindings) : Findings);
    }

    public boolean isBlank() {
        return Findings.isEmpty();
    }

    @Nullable
    public ResourceLocation cultureSample() {
        for (ResourceLocation Finding : Findings) {
            if (LabOrganisms.isKnown(Finding)) {
                return Finding;
            }
        }
        return Findings.isEmpty() ? null : Findings.get(0);
    }
}
