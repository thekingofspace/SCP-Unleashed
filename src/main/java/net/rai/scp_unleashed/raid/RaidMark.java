package net.rai.scp_unleashed.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RaidMark(int Raid, String Role) {
    public static final String Caster = "caster";
    public static final String Raider = "raider";
    public static final RaidMark None = new RaidMark(-1, "");
    public static final Codec<RaidMark> MarkCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.fieldOf("raid").forGetter(RaidMark::Raid),
            Codec.STRING.optionalFieldOf("role", "").forGetter(RaidMark::Role)
    ).apply(Instance, RaidMark::new));

    public boolean marked() {
        return Raid >= 0;
    }

    public boolean is(String Name) {
        return Role.equals(Name);
    }
}
