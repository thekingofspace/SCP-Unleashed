package net.rai.scp_unleashed.scp079;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Scp079Memory(int Pranks, String LastPrank, String LastVisitor, int Visits, int Broadcasts) {
    public static final Scp079Memory Blank = new Scp079Memory(0, "", "", 0, 0);
    public static final Codec<Scp079Memory> MemoryCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.optionalFieldOf("pranks", 0).forGetter(Scp079Memory::Pranks),
            Codec.STRING.optionalFieldOf("last_prank", "").forGetter(Scp079Memory::LastPrank),
            Codec.STRING.optionalFieldOf("last_visitor", "").forGetter(Scp079Memory::LastVisitor),
            Codec.INT.optionalFieldOf("visits", 0).forGetter(Scp079Memory::Visits),
            Codec.INT.optionalFieldOf("broadcasts", 0).forGetter(Scp079Memory::Broadcasts)
    ).apply(Instance, Scp079Memory::new));
    public static final StreamCodec<ByteBuf, Scp079Memory> MemoryStreamCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp079Memory::Pranks,
            ByteBufCodecs.STRING_UTF8, Scp079Memory::LastPrank,
            ByteBufCodecs.STRING_UTF8, Scp079Memory::LastVisitor,
            ByteBufCodecs.VAR_INT, Scp079Memory::Visits,
            ByteBufCodecs.VAR_INT, Scp079Memory::Broadcasts,
            Scp079Memory::new);

    public Scp079Memory {
        Pranks = Math.max(0, Pranks);
        LastPrank = LastPrank == null ? "" : LastPrank;
        LastVisitor = LastVisitor == null ? "" : LastVisitor;
        Visits = Math.max(0, Visits);
        Broadcasts = Math.max(0, Broadcasts);
    }

    public boolean isBlank() {
        return equals(Blank);
    }
}
