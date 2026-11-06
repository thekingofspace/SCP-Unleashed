package net.rai.scp_unleashed.scp330;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Scp330GreedState(int Elapsed) {
    public static final Scp330GreedState Fresh = new Scp330GreedState(0);
    public static final Codec<Scp330GreedState> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.optionalFieldOf("elapsed", 0).forGetter(Scp330GreedState::Elapsed)
    ).apply(Instance, Scp330GreedState::new));

    public Scp330GreedState next() {
        return new Scp330GreedState(Elapsed + 1);
    }
}
