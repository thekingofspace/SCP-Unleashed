package net.rai.scp_unleashed.wire;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.OptionalInt;

public final class RedstoneConversion {
    public static final List<String> SignalKeys = List.of("Signal", "Redstone", "Strength", "Level");
    public static final List<String> SwitchKeys = List.of("Active", "Detected", "Triggered", "Tripped", "Powered", "Open", "On");
    public static final List<String> CountKeys = List.of("Count", "Value", "Amount");
    public static final String DurationKey = "Duration";

    private RedstoneConversion() {
    }

    public static OptionalInt signalOf(CompoundTag Payload) {
        for (String Key : SignalKeys) {
            if (Payload.get(Key) instanceof NumericTag Numeric) {
                return OptionalInt.of(Mth.clamp((int) Math.round(Numeric.getAsDouble()), 0, 15));
            }
        }
        for (String Key : SwitchKeys) {
            if (Payload.get(Key) instanceof NumericTag Numeric) {
                return OptionalInt.of(Numeric.getAsDouble() != 0.0 ? 15 : 0);
            }
        }
        for (String Key : CountKeys) {
            if (Payload.get(Key) instanceof NumericTag Numeric) {
                return OptionalInt.of(Mth.clamp(Mth.ceil(Numeric.getAsDouble()), 0, 15));
            }
        }
        return OptionalInt.empty();
    }

    public static int durationOf(CompoundTag Payload) {
        return Payload.get(DurationKey) instanceof NumericTag Numeric ? Math.max(0, Numeric.getAsInt()) : 0;
    }
}
