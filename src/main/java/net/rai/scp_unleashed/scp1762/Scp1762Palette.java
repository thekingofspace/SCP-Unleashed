package net.rai.scp_unleashed.scp1762;

import net.minecraft.util.RandomSource;

import java.util.List;

public final class Scp1762Palette {
    public record Paper(String Name, int Colour, int Weight) {
    }

    public static final List<Paper> Papers = List.of(
            new Paper("white", 0xFFFFFF, 30), new Paper("cream", 0xF4E9CF, 9), new Paper("newsprint", 0xD9D6CC, 8), new Paper("faded_red", 0xE2A49A, 6),
            new Paper("sky_blue", 0xAFCFEA, 7), new Paper("mint", 0xB8E2C4, 6), new Paper("ochre", 0xE6C887, 6), new Paper("pink", 0xF2BCCD, 6),
            new Paper("lavender", 0xCDBDE8, 6), new Paper("orange", 0xF2B98A, 6), new Paper("deep_red", 0xC0353A, 2), new Paper("royal_blue", 0x3F5FC8, 2),
            new Paper("emerald", 0x2FA76A, 1), new Paper("gold", 0xE8B932, 1));
    public static final int White = 0xFFFFFF;

    private Scp1762Palette() {
    }

    public static int total() {
        int Sum = 0;
        for (Paper Each : Papers) {
            Sum += Each.Weight();
        }
        return Sum;
    }

    public static int pick(RandomSource Random) {
        int Roll = Random.nextInt(total());
        for (Paper Each : Papers) {
            Roll -= Each.Weight();
            if (Roll < 0) {
                return Each.Colour();
            }
        }
        return White;
    }
}
