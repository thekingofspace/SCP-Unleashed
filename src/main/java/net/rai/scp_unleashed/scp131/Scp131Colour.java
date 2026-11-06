package net.rai.scp_unleashed.scp131;

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

public enum Scp131Colour implements StringRepresentable {
    Red("red", 0xF0645A, 0.0f),
    Orange("orange", 0xF59A3C, 0.04f),
    Yellow("yellow", 0xF2D34A, 0.09f),
    Green("green", 0x72C85A, -0.03f),
    Blue("blue", 0x5EA2F0, -0.07f),
    Purple("purple", 0xB07EE6, 0.06f);

    public static final StringRepresentable.EnumCodec<Scp131Colour> ColourCodec = StringRepresentable.fromEnum(Scp131Colour::values);
    public static final IntFunction<Scp131Colour> ById = ByIdMap.continuous(Scp131Colour::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final StreamCodec<ByteBuf, Scp131Colour> ColourStreamCodec = ByteBufCodecs.idMapper(ById, Scp131Colour::ordinal);
    public static final String KeyPrefix = "entity.scp_unleashed.scp131_pod.colour.";

    private final String Name;
    private final int Label;
    private final float Voice;

    Scp131Colour(String Name, int Label, float Voice) {
        this.Name = Name;
        this.Label = Label;
        this.Voice = Voice;
    }

    public static Scp131Colour byIndex(int Index) {
        return ById.apply(Index);
    }

    public static Scp131Colour byName(String Wanted) {
        Scp131Colour Found = ColourCodec.byName(Wanted);
        return Found == null ? Red : Found;
    }

    public static Scp131Colour random(RandomSource Random) {
        Scp131Colour[] All = values();
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
