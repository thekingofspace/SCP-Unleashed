package net.rai.scp_unleashed.radio;

public final class RadioVoiceFilter {
    public static final int SampleRate = 48000;
    public static final double LowCut = 320.0;
    public static final double HighCut = 3200.0;
    public static final double Drive = 1.7;
    public static final double Ceiling = 26000.0;
    private final double HighPass;
    private final double LowPass;
    private double LastIn;
    private double LastHigh;
    private double FirstLow;
    private double SecondLow;

    public RadioVoiceFilter() {
        HighPass = 1.0 / (1.0 + 2.0 * Math.PI * LowCut / SampleRate);
        LowPass = 1.0 - Math.exp(-2.0 * Math.PI * HighCut / SampleRate);
    }

    public void process(short[] Pcm) {
        for (int Index = 0; Index < Pcm.length; Index++) {
            double In = Pcm[Index];
            double High = HighPass * (LastHigh + In - LastIn);
            LastIn = In;
            LastHigh = High;
            FirstLow += LowPass * (High - FirstLow);
            SecondLow += LowPass * (FirstLow - SecondLow);
            double Scaled = SecondLow * Drive / Ceiling;
            double Shaped = Scaled / (1.0 + Math.abs(Scaled));
            Pcm[Index] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(Shaped * Ceiling)));
        }
    }

    public void reset() {
        LastIn = 0.0;
        LastHigh = 0.0;
        FirstLow = 0.0;
        SecondLow = 0.0;
    }
}
