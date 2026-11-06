package net.rai.scp_unleashed.orbital;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public record OrbitalLink(UUID Owner, String Name) {
    public static final Codec<OrbitalLink> LinkCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(OrbitalLink::Owner),
            Codec.STRING.fieldOf("name").forGetter(OrbitalLink::Name)
    ).apply(Instance, OrbitalLink::new));
    public static final StreamCodec<ByteBuf, OrbitalLink> LinkStreamCodec = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, OrbitalLink::Owner,
            ByteBufCodecs.STRING_UTF8, OrbitalLink::Name,
            OrbitalLink::new);

    public boolean ownedBy(UUID Wanted) {
        return Owner.equals(Wanted);
    }
}
