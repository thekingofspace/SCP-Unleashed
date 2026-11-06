package net.rai.scp_unleashed.dclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record PersonnelRecord(UUID Subject, int Number, String Trait, String Crime, int Appearance, int Iq, int Athletic) {
    public static final Codec<PersonnelRecord> RecordCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            UUIDUtil.CODEC.fieldOf("subject").forGetter(PersonnelRecord::Subject),
            Codec.INT.fieldOf("number").forGetter(PersonnelRecord::Number),
            Codec.STRING.fieldOf("trait").forGetter(PersonnelRecord::Trait),
            Codec.STRING.fieldOf("crime").forGetter(PersonnelRecord::Crime),
            Codec.INT.fieldOf("appearance").forGetter(PersonnelRecord::Appearance),
            Codec.INT.optionalFieldOf("iq", 0).forGetter(PersonnelRecord::Iq),
            Codec.INT.optionalFieldOf("athletic", 0).forGetter(PersonnelRecord::Athletic)
    ).apply(Instance, PersonnelRecord::new));
    public static final StreamCodec<ByteBuf, PersonnelRecord> RecordStreamCodec = ByteBufCodecs.fromCodec(RecordCodec);

    public static PersonnelRecord of(DClass Subject) {
        return new PersonnelRecord(Subject.getUUID(), Subject.number(), Subject.trait().getId(), Subject.crime().getId(), Subject.appearance(), Subject.iq(), Subject.athletic());
    }

    public DClassTrait trait() {
        return DClassTrait.byId(Trait);
    }

    public DClassCrime crime() {
        return DClassCrime.byId(Crime);
    }

    public String designation() {
        return DClassNumbers.format(Number);
    }
}
