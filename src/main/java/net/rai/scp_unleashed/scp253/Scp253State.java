package net.rai.scp_unleashed.scp253;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Scp253State(int Elapsed, long Seed) {
    public static final Scp253State Fresh = new Scp253State(0, 0L);
    public static final Codec<Scp253State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.optionalFieldOf("elapsed", 0).forGetter(Scp253State::Elapsed),
            Codec.LONG.optionalFieldOf("seed", 0L).forGetter(Scp253State::Seed)
    ).apply(Instance, Scp253State::new));

    public Scp253State at(int Ticks) {
        return new Scp253State(Ticks, Seed);
    }
}
