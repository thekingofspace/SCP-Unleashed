package net.rai.scp_unleashed.scp427;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class Scp427State {
    public static final Codec<Scp427State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.optionalFieldOf("ticks", 0).forGetter(Saved -> Saved.Ticks),
            Codec.INT.optionalFieldOf("warned", 0).forGetter(Saved -> Saved.Warned),
            Codec.INT.optionalFieldOf("transform", 0).forGetter(Saved -> Saved.Transform)
    ).apply(Instance, Scp427State::new));

    public int Ticks;
    public int Warned;
    public int Transform;
    public int OpenTicks;
    public boolean WasOpen;

    public Scp427State() {
    }

    public Scp427State(int Ticks, int Warned, int Transform) {
        this.Ticks = Math.max(0, Ticks);
        this.Warned = Math.max(0, Warned);
        this.Transform = Math.max(0, Transform);
    }

    public boolean worthSaving() {
        return Ticks > 0 || Transform > 0;
    }

    public boolean transforming() {
        return Transform > 0;
    }
}
