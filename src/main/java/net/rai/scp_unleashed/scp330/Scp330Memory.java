package net.rai.scp_unleashed.scp330;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record Scp330Memory(Map<UUID, Integer> Takes) {
    public static final Scp330Memory Empty = new Scp330Memory(Map.of());
    public static final Codec<Scp330Memory> MemoryCodec = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT)
            .xmap(Scp330Memory::new, Scp330Memory::Takes);
    public static final StreamCodec<ByteBuf, Scp330Memory> MemoryStreamCodec = ByteBufCodecs.<ByteBuf, UUID, Integer, Map<UUID, Integer>>map(HashMap::new,
            UUIDUtil.STREAM_CODEC, ByteBufCodecs.VAR_INT).map(Scp330Memory::new, Scp330Memory::Takes);

    public Scp330Memory {
        Takes = Map.copyOf(Takes);
    }

    public boolean isEmpty() {
        return Takes.isEmpty();
    }
}
