package net.rai.scp_unleashed.scp093;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Scp093Doses(float Amount, long Stamp, long VisionUntil, long ClaimAt) {
    public static final Scp093Doses None = new Scp093Doses(0.0f, 0L, 0L, 0L);
    public static final Codec<Scp093Doses> DoseCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.FLOAT.fieldOf("amount").forGetter(Scp093Doses::Amount),
            Codec.LONG.fieldOf("stamp").forGetter(Scp093Doses::Stamp),
            Codec.LONG.optionalFieldOf("vision_until", 0L).forGetter(Scp093Doses::VisionUntil),
            Codec.LONG.optionalFieldOf("claim_at", 0L).forGetter(Scp093Doses::ClaimAt)
    ).apply(Instance, Scp093Doses::new));

    public Scp093Doses decayed(long Now, float PerTick) {
        if (ClaimAt > 0L || Now <= Stamp) {
            return this;
        }
        float Left = Math.max(0.0f, Amount - (Now - Stamp) * PerTick);
        return new Scp093Doses(Left, Now, VisionUntil, ClaimAt);
    }

    public boolean seeing(long Now) {
        return VisionUntil > Now;
    }

    public boolean claimed() {
        return ClaimAt > 0L;
    }
}
