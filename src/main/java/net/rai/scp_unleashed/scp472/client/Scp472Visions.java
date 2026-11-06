package net.rai.scp_unleashed.scp472.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.scp472.Scp472Mind;

import java.util.ArrayList;
import java.util.List;

public final class Scp472Visions {
    public static final int EyeDesigns = 3;
    public static final int SymbolDesigns = 8;
    public static final int[] MaxShown = {0, 0, 0, 0, 2, 3, 5};
    public static final int[] SpawnOdds = {0, 0, 0, 0, 40, 28, 16};
    public static final float[] EyeShare = {0.0f, 0.0f, 0.0f, 0.0f, 0.45f, 0.5f, 0.62f};
    public static final int EyeLifeMin = 40;
    public static final int EyeLifeMax = 90;
    public static final int SymbolLifeMin = 30;
    public static final int SymbolLifeMax = 70;
    public static final int FadeTicks = 6;
    public static final int FadeSteps = 3;
    public static final int BlinkTicks = 4;
    public static final int LidTicks = 3;
    public static final float EdgeBand = 0.55f;
    public static final float Margin = 0.06f;
    public static final float Spacing = 0.18f;
    public static final float FlickerOdds = 0.15f;
    private static final List<Vision> Shown = new ArrayList<>();

    private Scp472Visions() {
    }

    public enum Gaze {
        Left,
        Centre,
        Right
    }

    public static final class Vision {
        public final boolean Eye;
        public final int Design;
        public final float X;
        public final float Y;
        public final int Life;
        public final int BlinkAt;
        public final Gaze Look;
        public final long Seed;
        public int Age;

        private Vision(boolean Eye, int Design, float X, float Y, int Life, int BlinkAt, Gaze Look, long Seed) {
            this.Eye = Eye;
            this.Design = Design;
            this.X = X;
            this.Y = Y;
            this.Life = Life;
            this.BlinkAt = BlinkAt;
            this.Look = Look;
            this.Seed = Seed;
        }

        public float alpha() {
            int Edge = Math.min(Age + 1, Life - Age);
            if (Edge >= FadeTicks) {
                return 1.0f;
            }
            int Step = Mth.clamp((Edge * FadeSteps + FadeTicks - 1) / FadeTicks, 1, FadeSteps);
            return Step / (float) FadeSteps;
        }

        public boolean flickeredOut() {
            if (Eye) {
                return false;
            }
            long Mixed = mix(Seed, Age);
            return (Mixed & 1023L) < (long) (FlickerOdds * 1024.0f);
        }

        public int eyeFrame() {
            int Edge = Math.min(Age, Life - 1 - Age);
            if (Edge < LidTicks) {
                return 4;
            }
            if (Edge < LidTicks * 2) {
                return 3;
            }
            int Since = Age - BlinkAt;
            if (BlinkAt >= 0 && Since >= 0 && Since < BlinkTicks) {
                return Since == 0 || Since == BlinkTicks - 1 ? 3 : 4;
            }
            return Look.ordinal();
        }
    }

    public static List<Vision> shown() {
        return Shown;
    }

    public static void tick(Minecraft Client) {
        if (Client.player == null) {
            Shown.clear();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        int Stage = Scp472ClientState.heard() == null ? 0 : Scp472ClientState.stage();
        Shown.removeIf(Each -> ++Each.Age >= Each.Life);
        if (Stage < Scp472Mind.VisionStage) {
            Shown.clear();
            return;
        }
        RandomSource Random = Client.player.getRandom();
        if (Shown.size() < MaxShown[Stage] && Random.nextInt(SpawnOdds[Stage]) == 0) {
            spawn(Random, Stage);
        }
    }

    private static void spawn(RandomSource Random, int Stage) {
        for (int Attempt = 0; Attempt < 12; Attempt++) {
            float X = Margin + Random.nextFloat() * (1.0f - Margin * 2.0f);
            float Y = Margin + Random.nextFloat() * (1.0f - Margin * 2.0f);
            float Edge = Math.max(Math.abs(X - 0.5f), Math.abs(Y - 0.5f)) * 2.0f;
            if (Edge < EdgeBand) {
                continue;
            }
            boolean Crowded = false;
            for (Vision Other : Shown) {
                if (Math.abs(Other.X - X) < Spacing && Math.abs(Other.Y - Y) < Spacing) {
                    Crowded = true;
                    break;
                }
            }
            if (Crowded) {
                continue;
            }
            boolean Eye = Random.nextFloat() < EyeShare[Stage];
            int Life = Eye ? Mth.nextInt(Random, EyeLifeMin, EyeLifeMax) : Mth.nextInt(Random, SymbolLifeMin, SymbolLifeMax);
            int BlinkAt = Eye && Random.nextBoolean() ? Mth.nextInt(Random, FadeTicks, Math.max(FadeTicks, Life - FadeTicks - BlinkTicks)) : -1;
            Gaze Look = Stage >= Scp472Mind.HarmStage ? stare(X) : Gaze.values()[Random.nextInt(Gaze.values().length)];
            Shown.add(new Vision(Eye, Random.nextInt(Eye ? EyeDesigns : SymbolDesigns), X, Y, Life, BlinkAt, Look, Random.nextLong()));
            return;
        }
    }

    public static Gaze stare(float X) {
        if (X < 0.4f) {
            return Gaze.Right;
        }
        if (X > 0.6f) {
            return Gaze.Left;
        }
        return Gaze.Centre;
    }

    public static long mix(long Seed, long Value) {
        long Mixed = Seed + Value * 0x9E3779B97F4A7C15L;
        Mixed = (Mixed ^ (Mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        Mixed = (Mixed ^ (Mixed >>> 27)) * 0x94D049BB133111EBL;
        return Mixed ^ (Mixed >>> 31);
    }

    public static void reset() {
        Shown.clear();
    }
}
