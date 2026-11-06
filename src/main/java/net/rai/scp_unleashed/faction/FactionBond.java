package net.rai.scp_unleashed.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record FactionBond(String Faction, int Era, int Site, String Role) {
    public static final FactionBond None = new FactionBond("", 0, -1, "");
    public static final Codec<FactionBond> BondCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.STRING.fieldOf("faction").forGetter(FactionBond::Faction),
            Codec.INT.optionalFieldOf("era", 0).forGetter(FactionBond::Era),
            Codec.INT.optionalFieldOf("site", -1).forGetter(FactionBond::Site),
            Codec.STRING.optionalFieldOf("role", "").forGetter(FactionBond::Role)
    ).apply(Instance, FactionBond::new));

    public boolean bound() {
        return !Faction.isEmpty();
    }

    public boolean belongsTo(ResourceLocation Id) {
        return Faction.equals(Id.toString());
    }

    public boolean enlisted() {
        return bound() && Site >= 0;
    }
}
