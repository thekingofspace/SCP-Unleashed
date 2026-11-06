package net.rai.scp_unleashed.scp111;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

public enum Scp111Variant implements StringRepresentable {
    Ember("ember", Breath.Flame, 0xFFF4B0, 0xFF7A1A, 0xF2E4C6, 0xE0843A, 0xFFA040, false),
    Saffron("saffron", Breath.Flame, 0xFFFDE0, 0xFFC21E, 0xF4E39A, 0xA8642C, 0xFFD84A, false),
    Verdant("verdant", Breath.Flame, 0xF2FFB8, 0x58C830, 0xBEE6A6, 0x3C7A2C, 0x7AD850, false),
    Crimson("crimson", Breath.Ember, 0xFFD48E, 0xE02A1A, 0xD8685C, 0x3A1A18, 0xFF5A4A, false),
    Slate("slate", Breath.Smoke, 0xA4A4AC, 0x34343C, 0x9C9CA6, 0x484852, 0xA8A8B4, true),
    Azure("azure", Breath.Flame, 0xE4FFFF, 0x2A9CFF, 0x3C5CAA, 0x7ADEFF, 0x5AC8FF, true),
    Citrine("citrine", Breath.Spark, 0xFFFFFF, 0xF2EE6A, 0xD6E66E, 0x78B43A, 0xE0EE5A, false),
    Amethyst("amethyst", Breath.Flame, 0xFFE6FF, 0xAE48EE, 0xC8A8E8, 0x7A48B8, 0xC88AF4, false);

    public static final StringRepresentable.EnumCodec<Scp111Variant> VariantCodec = StringRepresentable.fromEnum(Scp111Variant::values);
    public static final IntFunction<Scp111Variant> ById = ByIdMap.continuous(Scp111Variant::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, Scp111Variant> VariantStreamCodec = ByteBufCodecs.idMapper(ById, Scp111Variant::ordinal);
    public static final String KeyPrefix = "entity.scp_unleashed.scp111_snail.variant.";

    private final String Name;
    private final Breath Style;
    private final int Core;
    private final int Edge;
    private final int Shell;
    private final int Spot;
    private final int Label;
    private final boolean Glows;

    Scp111Variant(String Name, Breath Style, int Core, int Edge, int Shell, int Spot, int Label, boolean Glows) {
        this.Name = Name;
        this.Style = Style;
        this.Core = Core;
        this.Edge = Edge;
        this.Shell = Shell;
        this.Spot = Spot;
        this.Label = Label;
        this.Glows = Glows;
    }

    public static Scp111Variant byIndex(int Index) {
        return ById.apply(Index);
    }

    public static Scp111Variant byName(String Wanted) {
        Scp111Variant Found = VariantCodec.byName(Wanted);
        return Found == null ? Ember : Found;
    }

    public static Scp111Variant random(RandomSource Random) {
        Scp111Variant[] All = values();
        return All[Random.nextInt(All.length)];
    }

    public Breath getStyle() {
        return Style;
    }

    public int getCore() {
        return Core;
    }

    public int getEdge() {
        return Edge;
    }

    public int getShell() {
        return Shell;
    }

    public int getSpot() {
        return Spot;
    }

    public int getLabel() {
        return Label;
    }

    public boolean glows() {
        return Glows;
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

    public enum Breath {
        Flame,
        Ember,
        Smoke,
        Spark
    }
}
