package net.rai.scp_unleashed.scp902;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Scp902Latch(boolean Open, long Since) {
    public static final Scp902Latch Shut = new Scp902Latch(false, Long.MIN_VALUE / 2);
    public static final Codec<Scp902Latch> LatchCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.BOOL.optionalFieldOf("open", false).forGetter(Scp902Latch::Open),
            Codec.LONG.optionalFieldOf("since", Shut.Since()).forGetter(Scp902Latch::Since)
    ).apply(Instance, Scp902Latch::new));
    public static final StreamCodec<ByteBuf, Scp902Latch> LatchStreamCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Scp902Latch::Open,
            ByteBufCodecs.VAR_LONG, Scp902Latch::Since,
            Scp902Latch::new);

    public Scp902Latch toggled(long Now) {
        return new Scp902Latch(!Open, Now);
    }

    public boolean settling(long Now) {
        return Now >= Since && Now - Since < Scp902Lid.RestTicks;
    }
}
