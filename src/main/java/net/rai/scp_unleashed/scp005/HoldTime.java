package net.rai.scp_unleashed.scp005;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum HoldTime implements StringRepresentable {
    Three("3s", 60),
    Five("5s", 100),
    Ten("10s", 200),
    Thirty("30s", 600),
    Sixty("60s", 1200),
    Forever("forever", -1);

    public static final HoldTime Default = Ten;
    public static final Codec<HoldTime> TimeCodec = StringRepresentable.fromEnum(HoldTime::values);
    private static final IntFunction<HoldTime> ById = ByIdMap.continuous(HoldTime::ordinal, values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
    public static final StreamCodec<ByteBuf, HoldTime> TimeStreamCodec = ByteBufCodecs.idMapper(ById, HoldTime::ordinal);

    private final String Name;
    private final int Ticks;

    HoldTime(String Name, int Ticks) {
        this.Name = Name;
        this.Ticks = Ticks;
    }

    public int getTicks() {
        return Ticks;
    }

    public boolean isForever() {
        return Ticks < 0;
    }

    public HoldTime next() {
        HoldTime[] All = values();
        return All[(ordinal() + 1) % All.length];
    }

    public Component describe() {
        if (isForever()) {
            return Component.translatable("item.scp_unleashed.scp005_key.forever");
        }
        return Component.translatable("item.scp_unleashed.scp005_key.seconds", Ticks / 20);
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
