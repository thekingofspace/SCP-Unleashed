package net.rai.scp_unleashed.dclass;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class DClassScores {
    public static final int IqLow = 70;
    public static final int IqHigh = 140;
    public static final int IqMean = 100;
    public static final int IqSpread = 40;
    public static final int IqDraws = 4;
    public static final int AthleticLow = 1;
    public static final int AthleticHigh = 100;
    public static final int AthleticDraws = 3;
    public static final int TheodoreIq = 100;
    public static final int TheodoreAthletic = 100;
    private static final String[] Bands = {"poor", "below_average", "average", "fit", "exceptional"};

    private DClassScores() {
    }

    public static int rollIq(RandomSource Random) {
        float Sum = 0.0f;
        for (int Draw = 0; Draw < IqDraws; Draw++) {
            Sum += Random.nextFloat() * 2.0f - 1.0f;
        }
        return Mth.clamp(Math.round(IqMean + Sum / IqDraws * IqSpread), IqLow, IqHigh);
    }

    public static int rollAthletic(RandomSource Random) {
        int Sum = 0;
        for (int Draw = 0; Draw < AthleticDraws; Draw++) {
            Sum += AthleticLow + Random.nextInt(AthleticHigh - AthleticLow + 1);
        }
        return Mth.clamp(Math.round(Sum / (float) AthleticDraws), AthleticLow, AthleticHigh);
    }

    public static Component band(int Athletic) {
        int Index = Mth.clamp((Athletic - 1) / 20, 0, Bands.length - 1);
        return Component.translatable("dclass.scp_unleashed.athletic." + Bands[Index]);
    }
}
