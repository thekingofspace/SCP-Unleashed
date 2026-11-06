package net.rai.scp_unleashed.scp513;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

public record Scp513State(int Elapsed, long Seed, double SourceX, double SourceY, double SourceZ) {
    public static final Scp513State Fresh = new Scp513State(-1, 0L, 0.0, 0.0, 0.0);
    public static final Codec<Scp513State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.fieldOf("elapsed").forGetter(Scp513State::Elapsed),
            Codec.LONG.fieldOf("seed").forGetter(Scp513State::Seed),
            Codec.DOUBLE.fieldOf("source_x").forGetter(Scp513State::SourceX),
            Codec.DOUBLE.fieldOf("source_y").forGetter(Scp513State::SourceY),
            Codec.DOUBLE.fieldOf("source_z").forGetter(Scp513State::SourceZ)
    ).apply(Instance, Scp513State::new));

    public static Scp513State begin(long Seed, Vec3 Source) {
        return new Scp513State(0, Seed, Source.x, Source.y, Source.z);
    }

    public boolean active() {
        return Elapsed >= 0;
    }

    public Vec3 source() {
        return new Vec3(SourceX, SourceY, SourceZ);
    }

    public Scp513State at(int Ticks) {
        return new Scp513State(Ticks, Seed, SourceX, SourceY, SourceZ);
    }

    public Scp513Stage stage() {
        return Scp513Stage.of(Elapsed);
    }
}
