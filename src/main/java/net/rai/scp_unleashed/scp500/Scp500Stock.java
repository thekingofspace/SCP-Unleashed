package net.rai.scp_unleashed.scp500;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record Scp500Stock(int Pills, long Restock) {
    public static final int Full = 8;
    public static final long DayLength = 24000L;
    public static final int RestockDays = 7;
    public static final long Period = DayLength * RestockDays;
    public static final long Unset = 0L;

    public static final Codec<Scp500Stock> StockCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.intRange(0, Full).optionalFieldOf("pills", Full).forGetter(Scp500Stock::Pills),
            Codec.LONG.optionalFieldOf("restock", Unset).forGetter(Scp500Stock::Restock)
    ).apply(Instance, Scp500Stock::new));
    public static final StreamCodec<ByteBuf, Scp500Stock> StockStreamCodec = StreamCodec.composite(ByteBufCodecs.VAR_INT, Scp500Stock::Pills,
            ByteBufCodecs.VAR_LONG, Scp500Stock::Restock, Scp500Stock::new);

    public Scp500Stock {
        Pills = Mth.clamp(Pills, 0, Full);
    }

    public static Scp500Stock fresh(long Now) {
        return new Scp500Stock(Full, Now + Period);
    }

    public Scp500Stock at(long Now) {
        long Next = Restock;
        if (Next <= Unset || Next - Now > Period) {
            Next = Now + Period;
        }
        if (Now < Next) {
            return Next == Restock ? this : new Scp500Stock(Pills, Next);
        }
        long Missed = (Now - Next) / Period + 1L;
        return new Scp500Stock(Full, Next + Missed * Period);
    }

    public long ticksLeft(long Now) {
        return Math.max(0L, at(Now).Restock() - Now);
    }

    public Scp500Stock withPills(int Count) {
        return new Scp500Stock(Count, Restock);
    }

    public boolean isFull() {
        return Pills >= Full;
    }
}
