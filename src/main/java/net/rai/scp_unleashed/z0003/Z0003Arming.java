package net.rai.scp_unleashed.z0003;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Z0003Arming(float Radius, long Due) {
    public static final Codec<Z0003Arming> ArmingCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.FLOAT.fieldOf("radius").forGetter(Z0003Arming::Radius),
            Codec.LONG.fieldOf("due").forGetter(Z0003Arming::Due)
    ).apply(Instance, Z0003Arming::new));
    public static final StreamCodec<ByteBuf, Z0003Arming> ArmingStreamCodec = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Z0003Arming::Radius,
            ByteBufCodecs.VAR_LONG, Z0003Arming::Due,
            Z0003Arming::new);

    public static Z0003Arming of(Z0003Phrase.Setting Setting, long Now) {
        return new Z0003Arming(Setting.Radius(), Now + Setting.DelayTicks());
    }

    public boolean due(long Now) {
        return Now >= Due;
    }

    public long left(long Now) {
        return Math.max(0L, Due - Now);
    }
}
