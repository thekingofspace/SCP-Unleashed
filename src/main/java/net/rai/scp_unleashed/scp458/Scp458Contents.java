package net.rai.scp_unleashed.scp458;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public record Scp458Contents(boolean Open, Optional<Scp458Pizza> Pizza, int Taken, long Since) {
    public static final int Slices = 6;
    public static final int Whole = (1 << Slices) - 1;
    public static final float SliceSpan = 360.0f / Slices;
    public static final Scp458Contents Sealed = new Scp458Contents(false, Optional.empty(), 0, Long.MIN_VALUE / 2);
    public static final Codec<Scp458Contents> ContentsCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.BOOL.optionalFieldOf("open", false).forGetter(Scp458Contents::Open),
            Scp458Pizza.PizzaCodec.optionalFieldOf("pizza").forGetter(Scp458Contents::Pizza),
            Codec.INT.optionalFieldOf("taken", 0).forGetter(Scp458Contents::Taken),
            Codec.LONG.optionalFieldOf("since", Sealed.Since()).forGetter(Scp458Contents::Since)
    ).apply(Instance, Scp458Contents::new));
    public static final StreamCodec<ByteBuf, Scp458Contents> ContentsStreamCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp458Contents::Open,
            ByteBufCodecs.optional(Scp458Pizza.PizzaStreamCodec), Scp458Contents::Pizza,
            ByteBufCodecs.VAR_INT, Scp458Contents::Taken,
            ByteBufCodecs.VAR_LONG, Scp458Contents::Since,
            Scp458Contents::new);

    public Scp458Contents {
        Taken &= Whole;
    }

    public Scp458Contents opened(Scp458Pizza Served, long Now) {
        return new Scp458Contents(true, Optional.of(Served), 0, Now);
    }

    public Scp458Contents closed(long Now) {
        return new Scp458Contents(false, Pizza, Taken, Now);
    }

    public Scp458Contents without(int Slice) {
        return new Scp458Contents(Open, Pizza, Taken | (1 << Slice), Since);
    }

    public boolean hasSlice(int Slice) {
        return Pizza.isPresent() && (Taken & (1 << Slice)) == 0;
    }

    public int left() {
        return Pizza.isPresent() ? Slices - Integer.bitCount(Taken) : 0;
    }

    public boolean empty() {
        return left() <= 0;
    }

    public boolean resting(long Now) {
        return !Open && Now - Since < Scp458Box.LidRest && Now >= Since;
    }

    public long age(long Now) {
        return Now - Since;
    }

    public static float centre(int Slice) {
        return (Slice + 0.5f) * SliceSpan;
    }

    public int nearest(float Angle) {
        int Best = -1;
        float BestGap = Float.MAX_VALUE;
        for (int Slice = 0; Slice < Slices; Slice++) {
            if (!hasSlice(Slice)) {
                continue;
            }
            float Gap = Math.abs(((centre(Slice) - Angle) % 360.0f + 540.0f) % 360.0f - 180.0f);
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = Slice;
            }
        }
        return Best;
    }
}
