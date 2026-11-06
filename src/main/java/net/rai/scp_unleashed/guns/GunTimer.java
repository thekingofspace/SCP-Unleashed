package net.rai.scp_unleashed.guns;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GunTimer(long Start, long End) {
    public static final StreamCodec<ByteBuf, GunTimer> TimerStreamCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, GunTimer::Start,
            ByteBufCodecs.VAR_LONG, GunTimer::End,
            GunTimer::new);

    public float progress(long Now, float Partial) {
        long Span = Math.max(1L, End - Start);
        return Math.max(0.0F, Math.min(1.0F, ((Now - Start) + Partial) / (float) Span));
    }

    public boolean isDone(long Now) {
        return Now >= End;
    }
}
