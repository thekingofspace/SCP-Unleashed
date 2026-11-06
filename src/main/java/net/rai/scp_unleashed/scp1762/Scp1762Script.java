package net.rai.scp_unleashed.scp1762;

import net.minecraft.world.phys.Vec3;

public final class Scp1762Script {
    public static final byte None = 0;
    public static final byte Emerge = 1;
    public static final byte Return = 2;
    public static final int Stride = 6;
    public static final int Smooth = 0;
    public static final int Linear = 1;
    public static final int Arc = 2;
    public static final double Floor = 0.072917;
    public static final double Rim = 0.375000;
    public static final double RimEdge = 0.260417;
    public static final double Outside = 0.6200;
    public static final double DiveHeight = 1.1500;
    public static final int ReturnLead = 3;
    public static final double[] DrakeEmerge = {0.0000, 0.0000, 0.0729, 0.5500, 0.0000, 0.0000, 0.1800, 0.0200, 0.0729, 0.8000, 0.0000, 0.0000, 0.3500, 0.1000, 0.2000, 0.9000, 0.0000, 0.0000, 0.5800, 0.2000, 0.3100, 1.0000, 0.0000, 0.0000, 0.7200, 0.2604, 0.3750, 1.0000, 2.0000, 0.1400, 0.9700, 0.6200, 0.0000, 1.0000, 1.0000, 0.0000, 1.2700, 0.6200, 0.0000, 1.0000, 1.0000, 0.0000};
    public static final double[] DrakeReturn = {0.0000, 0.0000, 0.0000, 1.0000, 1.0000, 0.0000, 0.1200, 0.0000, 0.0000, 1.0000, 2.0000, 0.1400, 0.4000, 0.3596, 0.3750, 1.0000, 0.0000, 0.0000, 0.6000, 0.3596, 0.3750, 1.0000, 2.0000, 0.0600, 1.0000, 0.6200, 0.0729, 0.5500, 1.0000, 0.0000};
    public static final double[] WyvernEmerge = {0.0000, 0.0000, 0.0729, 0.4500, 0.0000, 0.0000, 0.1500, 0.0000, 0.0729, 0.5000, 0.0000, 0.0000, 0.4000, 0.0500, 0.5000, 0.8000, 0.0000, 0.0000, 0.6500, 0.1800, 1.0000, 1.0000, 1.0000, 0.0000, 0.9000, 0.3400, 1.3500, 1.0000, 1.0000, 0.0000};
    public static final double[] WyvernReturn = {0.0000, 0.0000, 1.1500, 1.0000, 0.0000, 0.0000, 0.2500, 0.0000, 0.9500, 0.9500, 0.0000, 0.0000, 0.5500, 0.0000, 0.4500, 0.6000, 0.0000, 0.0000, 0.8000, 0.0000, 0.0829, 0.2500, 1.0000, 0.0000};
    public static final double[] DragonEmerge = {0.0000, 0.0000, 0.0729, 0.4500, 0.0000, 0.0000, 0.1500, 0.0000, 0.0729, 0.5000, 0.0000, 0.0000, 0.4000, 0.0500, 0.5000, 0.8000, 0.0000, 0.0000, 0.6500, 0.1800, 1.0000, 1.0000, 1.0000, 0.0000, 0.9000, 0.3400, 1.3500, 1.0000, 1.0000, 0.0000};
    public static final double[] DragonReturn = {0.0000, 0.0000, 1.1500, 1.0000, 0.0000, 0.0000, 0.2500, 0.0000, 0.9500, 0.9500, 0.0000, 0.0000, 0.5500, 0.0000, 0.4500, 0.6000, 0.0000, 0.0000, 0.8000, 0.0000, 0.0829, 0.2500, 1.0000, 0.0000};
    public static final double[] WyrmEmerge = {0.0000, 0.0000, 0.0729, 0.4000, 0.0000, 0.0000, 0.5000, 0.0400, 0.5500, 0.7500, 0.0000, 0.0000, 1.1000, 0.2200, 1.2000, 1.0000, 1.0000, 0.0000};
    public static final double[] WyrmReturn = {0.0000, 0.0000, 1.1500, 1.0000, 0.0000, 0.0000, 0.2500, 0.0000, 0.9500, 0.9500, 0.0000, 0.0000, 0.5500, 0.0000, 0.4500, 0.6000, 0.0000, 0.0000, 0.8000, 0.0000, 0.0829, 0.2500, 1.0000, 0.0000};

    private Scp1762Script() {
    }

    public static double[] table(Scp1762Kind Kind, byte Script) {
        boolean Emerging = Script == Emerge;
        return switch (Kind) {
            case Drake -> Emerging ? DrakeEmerge : DrakeReturn;
            case Wyvern -> Emerging ? WyvernEmerge : WyvernReturn;
            case Dragon -> Emerging ? DragonEmerge : DragonReturn;
            case Wyrm -> Emerging ? WyrmEmerge : WyrmReturn;
        };
    }

    public static double length(Scp1762Kind Kind, byte Script) {
        double[] Keys = table(Kind, Script);
        return Keys[Keys.length - Stride];
    }

    public static int ticks(Scp1762Kind Kind, byte Script) {
        return (int) Math.ceil(length(Kind, Script) * 20.0) + (Script == Return ? ReturnLead : 0);
    }

    public static double ease(double Share) {
        double Clamped = Math.max(0.0, Math.min(1.0, Share));
        return Clamped * Clamped * (3.0 - 2.0 * Clamped);
    }

    public static Vec3 sample(Scp1762Kind Kind, byte Script, double Seconds) {
        double[] Keys = table(Kind, Script);
        if (Seconds <= Keys[0]) {
            return new Vec3(Keys[1], Keys[2], Keys[3]);
        }
        for (int Index = 0; Index + Stride < Keys.length; Index += Stride) {
            double Start = Keys[Index];
            double End = Keys[Index + Stride];
            if (Seconds >= Start && Seconds <= End) {
                double Share = (Seconds - Start) / (End - Start);
                int Mode = (int) Keys[Index + 4];
                double Blend = Mode == Smooth ? ease(Share) : Share;
                double Forward = Keys[Index + 1] + (Keys[Index + Stride + 1] - Keys[Index + 1]) * Blend;
                double Up = Keys[Index + 2] + (Keys[Index + Stride + 2] - Keys[Index + 2]) * Blend;
                if (Mode == Arc) {
                    Up += Keys[Index + 5] * 4.0 * Share * (1.0 - Share);
                }
                double Scale = Keys[Index + 3] + (Keys[Index + Stride + 3] - Keys[Index + 3]) * Blend;
                return new Vec3(Forward, Up, Scale);
            }
        }
        int Last = Keys.length - Stride;
        return new Vec3(Keys[Last + 1], Keys[Last + 2], Keys[Last + 3]);
    }
}
