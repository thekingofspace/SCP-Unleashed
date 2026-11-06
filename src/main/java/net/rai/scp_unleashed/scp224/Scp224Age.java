package net.rai.scp_unleashed.scp224;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

public record Scp224Age(int Elapsed, GlobalPos Clock, long Serial, boolean Released) {
    public static final Scp224Age Fresh = new Scp224Age(0, GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO), 0L, true);
    public static final Codec<Scp224Age> AgeCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.fieldOf("elapsed").forGetter(Scp224Age::Elapsed),
            GlobalPos.CODEC.fieldOf("clock").forGetter(Scp224Age::Clock),
            Codec.LONG.fieldOf("serial").forGetter(Scp224Age::Serial),
            Codec.BOOL.optionalFieldOf("released", false).forGetter(Scp224Age::Released)
    ).apply(Instance, Scp224Age::new));

    public Scp224Age withElapsed(int Ticks) {
        return new Scp224Age(Ticks, Clock, Serial, Released);
    }

    public Scp224Age released() {
        return new Scp224Age(Elapsed, Clock, Serial, true);
    }

    public float progress() {
        return Math.min(1.0f, Elapsed / (float) Scp224Aging.Span);
    }
}
