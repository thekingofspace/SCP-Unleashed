package net.rai.scp_unleashed.orbital;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record OrbitalRecharge(long ReadyAt, int Span) {
    public static final Codec<OrbitalRecharge> RechargeCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.LONG.fieldOf("ready_at").forGetter(OrbitalRecharge::ReadyAt),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("span").forGetter(OrbitalRecharge::Span)
    ).apply(Instance, OrbitalRecharge::new));
    public static final StreamCodec<ByteBuf, OrbitalRecharge> RechargeStreamCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, OrbitalRecharge::ReadyAt,
            ByteBufCodecs.VAR_INT, OrbitalRecharge::Span,
            OrbitalRecharge::new);

    public boolean charging(long Now) {
        return Now < ReadyAt;
    }

    public float progress(long Now) {
        return Mth.clamp(1.0F - (ReadyAt - Now) / (float) Span, 0.0F, 1.0F);
    }

    public int secondsLeft(long Now) {
        return (int) Math.max(0L, (ReadyAt - Now + 19L) / 20L);
    }
}
