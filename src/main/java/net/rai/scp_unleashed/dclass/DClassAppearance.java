package net.rai.scp_unleashed.dclass;

import net.minecraft.util.RandomSource;

public final class DClassAppearance {
    public static final int Skins = 6;
    public static final int Styles = 7;
    public static final int HairColours = 6;
    public static final int EyeColours = 6;
    public static final int Brows = 3;
    public static final int Facials = 4;
    public static final int Marks = 4;
    public static final int Suits = 2;
    public static final int Bald = 0;
    public static final int Buzz = 1;
    public static final int Short = 2;
    public static final int Parted = 3;
    public static final int Long = 4;
    public static final int Curly = 5;
    public static final int Mohawk = 6;
    public static final int Glasses = 3;
    public static final int Jumpsuit = 0;
    public static final int Fatigues = 1;
    public static final int Theodore = pack(1, Parted, 2, 2, 2, 2, 0, Fatigues);

    private DClassAppearance() {
    }

    public static int pack(int Skin, int Style, int HairColour, int EyeColour, int Brow, int Facial, int Mark, int Suit) {
        return Skin & 7 | (Style & 7) << 3 | (HairColour & 7) << 6 | (EyeColour & 7) << 9 | (Brow & 3) << 12 | (Facial & 3) << 14 | (Mark & 3) << 16 | (Suit & 3) << 18;
    }

    public static int skin(int Code) {
        return Math.min(Skins - 1, Code & 7);
    }

    public static int style(int Code) {
        return Math.min(Styles - 1, Code >> 3 & 7);
    }

    public static int hairColour(int Code) {
        return Math.min(HairColours - 1, Code >> 6 & 7);
    }

    public static int eyeColour(int Code) {
        return Math.min(EyeColours - 1, Code >> 9 & 7);
    }

    public static int brow(int Code) {
        return Math.min(Brows - 1, Code >> 12 & 3);
    }

    public static int facial(int Code) {
        return Math.min(Facials - 1, Code >> 14 & 3);
    }

    public static int mark(int Code) {
        return Math.min(Marks - 1, Code >> 16 & 3);
    }

    public static int suit(int Code) {
        return Math.min(Suits - 1, Code >> 18 & 3);
    }

    public static int random(RandomSource Random) {
        int Skin = Random.nextInt(Skins);
        int Style = Random.nextInt(Styles);
        int HairColour = Random.nextInt(10) < 2 ? 5 : Random.nextInt(HairColours - 1);
        int EyeColour = Random.nextInt(EyeColours);
        int Brow = Random.nextInt(Brows);
        int Facial = Random.nextInt(10) < 5 ? 0 : 1 + Random.nextInt(Facials - 1);
        int Mark = Random.nextInt(10) < 4 ? 0 : Random.nextInt(Marks);
        return pack(Skin, Style, HairColour, EyeColour, Brow, Facial, Mark, Jumpsuit);
    }
}
