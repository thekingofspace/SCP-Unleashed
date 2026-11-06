package net.rai.scp_unleashed.scp253;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record Scp253Display(int Elapsed, long Stamp, long Seed) {
    public static final Scp253Display None = new Scp253Display(0, 0L, 0L);
    public static final int Drift = Scp253Infection.SyncTicks * 2;
    public static final StreamCodec<ByteBuf, Scp253Display> DisplayStream = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp253Display::Elapsed,
            ByteBufCodecs.VAR_LONG, Scp253Display::Stamp,
            ByteBufCodecs.VAR_LONG, Scp253Display::Seed,
            Scp253Display::new);

    public float progressAt(long Now, float Partial) {
        float Ahead = Mth.clamp(Now - Stamp + Partial, 0.0f, Drift);
        return Scp253Infection.progressOf(Elapsed + Ahead);
    }
}
