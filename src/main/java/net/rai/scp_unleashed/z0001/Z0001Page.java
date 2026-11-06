package net.rai.scp_unleashed.z0001;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Z0001Page(long Day, long Until, String Name) {
    public static final int MaxName = 16;
    public static final Codec<Z0001Page> PageCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.LONG.fieldOf("day").forGetter(Z0001Page::Day),
            Codec.LONG.fieldOf("until").forGetter(Z0001Page::Until),
            Codec.string(0, MaxName).optionalFieldOf("name", "").forGetter(Z0001Page::Name)
    ).apply(Instance, Z0001Page::new));
    public static final StreamCodec<ByteBuf, Z0001Page> PageStreamCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, Z0001Page::Day,
            ByteBufCodecs.VAR_LONG, Z0001Page::Until,
            ByteBufCodecs.stringUtf8(MaxName), Z0001Page::Name,
            Z0001Page::new);

    public boolean isSpent(long Today) {
        return Today >= Day && Today < Until;
    }
}
