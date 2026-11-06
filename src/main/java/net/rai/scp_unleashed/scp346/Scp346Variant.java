package net.rai.scp_unleashed.scp346;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

public enum Scp346Variant implements StringRepresentable {
    Ash("ash", 0xA4A7AE, 1.0f),
    Smoky("smoky", 0x8C7360, 0.96f),
    Sooty("sooty", 0x55555C, 0.92f),
    Sandy("sandy", 0xCDB07E, 1.04f),
    Rusty("rusty", 0xB0603A, 0.98f),
    Pale("pale", 0xE8E2D2, 1.07f);

    public static final StringRepresentable.EnumCodec<Scp346Variant> VariantCodec = StringRepresentable.fromEnum(Scp346Variant::values);
    public static final IntFunction<Scp346Variant> ById = ByIdMap.continuous(Scp346Variant::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final String KeyPrefix = "entity.scp_unleashed.scp346.variant.";

    private final String Name;
    private final int Label;
    private final float Voice;

    Scp346Variant(String Name, int Label, float Voice) {
        this.Name = Name;
        this.Label = Label;
        this.Voice = Voice;
    }

    public static Scp346Variant byIndex(int Index) {
        return ById.apply(Index);
    }

    public static Scp346Variant byName(String Wanted) {
        Scp346Variant Found = VariantCodec.byName(Wanted);
        return Found == null ? Ash : Found;
    }

    @Nullable
    public static Scp346Variant lookup(String Wanted) {
        return VariantCodec.byName(Wanted);
    }

    public static Scp346Variant random(RandomSource Random) {
        Scp346Variant[] All = values();
        return All[Random.nextInt(All.length)];
    }

    public int getLabel() {
        return Label;
    }

    public float getVoice() {
        return Voice;
    }

    public String getKey() {
        return KeyPrefix + Name;
    }

    public MutableComponent displayName() {
        return Component.translatable(getKey()).withStyle(Text -> Text.withColor(TextColor.fromRgb(Label)));
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
