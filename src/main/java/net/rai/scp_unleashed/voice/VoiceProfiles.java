package net.rai.scp_unleashed.voice;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleUnaryOperator;

public final class VoiceProfiles {
    public static final int SampleRate = 48000;
    public static final double FadeMillis = 10.0;
    public static final double Scp939Pitch = 0.96;
    public static final double Scp939Wobble = 0.045;
    public static final double Scp939WobbleHz = 0.8;
    public static final double Scp939DriftHz = 0.23;
    public static final double Scp939DriftShare = 0.4;
    public static final double Scp939LowAlpha = 0.28;
    public static final double DayPitch = 0.96;
    public static final double DayWobble = 0.012;
    public static final double DayWobbleHz = 0.19;
    public static final double DayWobbleSecondHz = 0.43;
    public static final double DayWobbleSecondShare = 0.35;
    public static final double DayFormantHz = 450.0;
    public static final double DayFormantQ = 0.9;
    public static final double DayFormantGainDb = 5.0;
    public static final double DayHighCutHz = 4200.0;
    public static final double DayChorusMix = 0.22;
    public static final double DayChorusDelayMillis = 7.0;
    public static final double DayChorusDepthMillis = 2.5;
    public static final double DayChorusHz = 0.7;
    public static final double DayFirstStutterMin = 0.6;
    public static final double DayFirstStutterMax = 3.5;
    public static final double DayStutterGapMin = 3.0;
    public static final double DayStutterGapMax = 5.5;
    public static final double DayStutterMinMillis = 40.0;
    public static final double DayStutterMaxMillis = 80.0;
    public static final double DayStutterFadeMillis = 3.0;
    public static final VoiceProfile Lure = (Pcm, Seed, Scale) -> scp939(Pcm, Seed, Scale);
    public static final VoiceProfile Day = (Pcm, Seed, Scale) -> scp001Day(Pcm, Seed, Scale).Pcm();
    private static final Map<String, VoiceProfile> Profiles = new ConcurrentHashMap<>();

    static {
        register(VoicePolicy.Scp939, Lure);
        register(VoicePolicy.Scp001Day, Day);
    }

    private VoiceProfiles() {
    }

    public record Shaped(short[] Pcm, int Stutters) {
    }

    public static void register(String Anomaly, VoiceProfile Profile) {
        Profiles.put(Anomaly, Profile);
    }

    public static VoiceProfile forAnomaly(String Anomaly) {
        return Profiles.getOrDefault(Anomaly, Lure);
    }

    private static double[] resample(short[] Pcm, DoubleUnaryOperator Rate) {
        int Guess = (int) Math.ceil(Pcm.length / 0.5) + 2;
        double[] Out = new double[Math.max(2, Guess)];
        int Count = 0;
        double Read = 0.0;
        while (Read < Pcm.length - 1 && Count < Out.length) {
            int Whole = (int) Read;
            double Fraction = Read - Whole;
            Out[Count] = Pcm[Whole] * (1.0 - Fraction) + Pcm[Whole + 1] * Fraction;
            Read += Math.max(0.25, Rate.applyAsDouble(Count / (double) SampleRate));
            Count++;
        }
        double[] Trimmed = new double[Count];
        System.arraycopy(Out, 0, Trimmed, 0, Count);
        return Trimmed;
    }

    private static short[] finish(double[] Signal) {
        int Fade = (int) (FadeMillis * SampleRate / 1000.0);
        short[] Out = new short[Signal.length];
        for (int Index = 0; Index < Signal.length; Index++) {
            double Gain = Math.min(1.0, Math.min(Index, Signal.length - 1 - Index) / (double) Math.max(1, Fade));
            Out[Index] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(Signal[Index] * Gain)));
        }
        return Out;
    }

    public static short[] scp939(short[] Pcm, long Seed, float Scale) {
        Random Roll = new Random(Seed);
        double Phase = Roll.nextDouble();
        double Drift = Roll.nextDouble();
        double Base = Scp939Pitch * Scale;
        double[] Signal = resample(Pcm, Time -> Base * (1.0 + Scp939Wobble * Math.sin(2.0 * Math.PI * (Phase + Scp939WobbleHz * Time))
                + Scp939Wobble * Scp939DriftShare * Math.sin(2.0 * Math.PI * (Drift + Scp939DriftHz * Time))));
        double Low = 0.0;
        for (int Index = 0; Index < Signal.length; Index++) {
            Low += Scp939LowAlpha * (Signal[Index] - Low);
            Signal[Index] = Low;
        }
        return finish(Signal);
    }

    public static Shaped scp001Day(short[] Pcm, long Seed, float Scale) {
        Random Roll = new Random(Seed);
        double First = Roll.nextDouble();
        double Second = Roll.nextDouble();
        double Sway = Roll.nextDouble();
        double Base = DayPitch * Scale;
        double[] Signal = resample(Pcm, Time -> Base * (1.0 + DayWobble * ((1.0 - DayWobbleSecondShare) * Math.sin(2.0 * Math.PI * (First + DayWobbleHz * Time))
                + DayWobbleSecondShare * Math.sin(2.0 * Math.PI * (Second + DayWobbleSecondHz * Time)))));
        formant(Signal);
        highCut(Signal);
        Signal = chorus(Signal, Sway);
        return stutter(Signal, Roll);
    }

    private static void formant(double[] Signal) {
        double Gain = Math.pow(10.0, DayFormantGainDb / 40.0);
        double Omega = 2.0 * Math.PI * DayFormantHz / SampleRate;
        double Alpha = Math.sin(Omega) / (2.0 * DayFormantQ);
        double B0 = 1.0 + Alpha * Gain;
        double B1 = -2.0 * Math.cos(Omega);
        double B2 = 1.0 - Alpha * Gain;
        double A0 = 1.0 + Alpha / Gain;
        double A1 = -2.0 * Math.cos(Omega);
        double A2 = 1.0 - Alpha / Gain;
        double X1 = 0.0;
        double X2 = 0.0;
        double Y1 = 0.0;
        double Y2 = 0.0;
        for (int Index = 0; Index < Signal.length; Index++) {
            double In = Signal[Index];
            double Out = (B0 * In + B1 * X1 + B2 * X2 - A1 * Y1 - A2 * Y2) / A0;
            X2 = X1;
            X1 = In;
            Y2 = Y1;
            Y1 = Out;
            Signal[Index] = Out;
        }
    }

    private static void highCut(double[] Signal) {
        double Alpha = 1.0 - Math.exp(-2.0 * Math.PI * DayHighCutHz / SampleRate);
        double Low = 0.0;
        for (int Index = 0; Index < Signal.length; Index++) {
            Low += Alpha * (Signal[Index] - Low);
            Signal[Index] = Low;
        }
    }

    private static double[] chorus(double[] Signal, double Sway) {
        double[] Out = new double[Signal.length];
        for (int Index = 0; Index < Signal.length; Index++) {
            double Time = Index / (double) SampleRate;
            double Delay = (DayChorusDelayMillis + DayChorusDepthMillis * Math.sin(2.0 * Math.PI * (Sway + DayChorusHz * Time))) * SampleRate / 1000.0;
            double Back = Index - Delay;
            double Echo = 0.0;
            if (Back >= 0.0) {
                int Whole = (int) Back;
                double Fraction = Back - Whole;
                Echo = Signal[Whole] * (1.0 - Fraction) + Signal[Math.min(Signal.length - 1, Whole + 1)] * Fraction;
            }
            Out[Index] = (1.0 - DayChorusMix) * Signal[Index] + DayChorusMix * Echo;
        }
        return Out;
    }

    private static Shaped stutter(double[] Signal, Random Roll) {
        int Fade = (int) (DayStutterFadeMillis * SampleRate / 1000.0);
        double[] Out = new double[Signal.length + (int) (Signal.length / (DayStutterGapMin * SampleRate) + 2) * (int) (DayStutterMaxMillis * SampleRate / 1000.0)];
        int Written = 0;
        int Read = 0;
        int Count = 0;
        double Next = (DayFirstStutterMin + Roll.nextDouble() * (DayFirstStutterMax - DayFirstStutterMin)) * SampleRate;
        while (Read < Signal.length) {
            int Until = (int) Math.min(Signal.length, Next);
            while (Read < Until) {
                Out[Written++] = Signal[Read++];
            }
            if (Read >= Signal.length) {
                break;
            }
            int Length = (int) ((DayStutterMinMillis + Roll.nextDouble() * (DayStutterMaxMillis - DayStutterMinMillis)) * SampleRate / 1000.0);
            if (Read >= Length && Written + Length < Out.length) {
                int Start = Read - Length;
                for (int Index = 0; Index < Length; Index++) {
                    double Sample = Signal[Start + Index];
                    if (Index < Fade) {
                        double Blend = Index / (double) Fade;
                        Sample = Sample * Blend + Signal[Math.min(Signal.length - 1, Read + Index)] * (1.0 - Blend);
                    }
                    Out[Written++] = Sample;
                }
                Count++;
            }
            Next = Read + (DayStutterGapMin + Roll.nextDouble() * (DayStutterGapMax - DayStutterGapMin)) * SampleRate;
        }
        double[] Trimmed = new double[Written];
        System.arraycopy(Out, 0, Trimmed, 0, Written);
        return new Shaped(finish(Trimmed), Count);
    }
}
