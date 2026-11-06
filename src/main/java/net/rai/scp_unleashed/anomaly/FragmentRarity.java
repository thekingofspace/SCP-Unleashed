package net.rai.scp_unleashed.anomaly;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum FragmentRarity implements StringRepresentable {
    Faint("faint", 1, 0xFFB8C4CE),
    Wavering("wavering", 2, 0xFF5CD07A),
    Volatile("volatile", 3, 0xFF4C9BF5),
    Severe("severe", 4, 0xFFB45CF5),
    Abyssal("abyssal", 5, 0xFFF0483C);

    public static final Codec<FragmentRarity> RarityCodec = StringRepresentable.fromEnum(FragmentRarity::values);

    private final String SerializedName;
    private final int Tier;
    private final int Colour;

    FragmentRarity(String SerializedName, int Tier, int Colour) {
        this.SerializedName = SerializedName;
        this.Tier = Tier;
        this.Colour = Colour;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }

    public int getTier() {
        return Tier;
    }

    public int getColour() {
        return Colour;
    }

    public static FragmentRarity byTier(int Tier) {
        FragmentRarity[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Tier - 1))];
    }
}
