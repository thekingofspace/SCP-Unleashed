package net.rai.scp_unleashed.scp5001.mekhane;

import net.minecraft.util.Mth;

public final class MekhaneTimeline {
    public static final int AlarmEnd = 100;
    public static final int AlarmEvery = 40;
    public static final int BeamCharge = 60;
    public static final int BeamFire = 100;
    public static final int BeamFull = 140;
    public static final int DescentStart = 140;
    public static final int SummonTicks = 540;
    public static final double DescentHeight = 180.0;
    public static final int Clash = 300;
    public static final int Chase = 700;
    public static final int Dive = 1100;
    public static final int Slam = 1500;
    public static final int SlamHit = 1520;
    public static final int SlamGround = 1590;
    public static final int BindBeat = 1770;
    public static final int Hurl = 2330;
    public static final int Climax = 2400;
    public static final int LaserStart = 2600;
    public static final int LaserFull = 2640;
    public static final int PortalOpen = 2760;
    public static final int PortalFull = 2860;
    public static final int BindStart = 2860;
    public static final int Banish = 3060;
    public static final int Resolve = 3080;
    public static final int LookDown = 3240;
    public static final int Ascend = 3300;
    public static final int DissolveStart = 3400;
    public static final int BeamFade = 3500;
    public static final int DuelTicks = 3560;
    public static final double AscendHeight = 170.0;
    public static final int[] Beats = {0, Clash, Chase, Dive, Slam, BindBeat, Climax, Resolve};
    public static final int[][] Acts = {
            {0, Act.Raise.ordinal()},
            {240, Act.Fly.ordinal()},
            {300, Act.Dash.ordinal()},
            {342, Act.Cleave.ordinal()},
            {410, Act.Fly.ordinal()},
            {432, Act.Deflect.ordinal()},
            {510, Act.Fly.ordinal()},
            {576, Act.Recoil.ordinal()},
            {650, Act.Fly.ordinal()},
            {700, Act.Chase.ordinal()},
            {892, Act.Sweep.ordinal()},
            {960, Act.Chase.ordinal()},
            {1100, Act.Dash.ordinal()},
            {1132, Act.Cleave.ordinal()},
            {1220, Act.Fly.ordinal()},
            {1244, Act.Deflect.ordinal()},
            {1320, Act.Fly.ordinal()},
            {1382, Act.Sweep.ordinal()},
            {1460, Act.Fly.ordinal()},
            {1514, Act.Slammed.ordinal()},
            {1600, Act.Ascend.ordinal()},
            {1740, Act.Fly.ordinal()},
            {1810, Act.Bound.ordinal()},
            {2230, Act.Pry.ordinal()},
            {2400, Act.Fly.ordinal()},
            {2480, Act.Plant.ordinal()},
            {LaserStart, Act.Channel.ordinal()},
            {Banish, Act.Seal.ordinal()},
            {3100, Act.Idle.ordinal()},
            {LookDown, Act.LookDown.ordinal()},
            {Ascend, Act.Ascend.ordinal()},
    };
    public static final int[][] Damage = {{590, 1}, {SlamHit, 2}, {1900, 3}, {2050, 4}};
    public static final int[][] Dashes = {{300, 360, 410}, {872, 920, 960}, {1104, 1160, 1210}, {1360, 1410, 1452}};

    public enum Act {
        Idle("idle"),
        Descend("descend"),
        Raise("raise"),
        Cleave("cleave"),
        Sweep("sweep"),
        Deflect("deflect"),
        Recoil("recoil"),
        Bound("bound"),
        Pry("pry"),
        Plant("plant"),
        Channel("channel"),
        Seal("seal"),
        LookDown("look_down"),
        Ascend("ascend"),
        Fly("fly"),
        Dash("dash"),
        Chase("chase"),
        Slammed("slammed");

        public final String Clip;

        Act(String Clip) {
            this.Clip = Clip;
        }

        public static Act of(int Index) {
            Act[] All = values();
            return All[Mth.clamp(Index, 0, All.length - 1)];
        }

        public boolean loops() {
            return this == Idle || this == Bound || this == Channel || this == Fly || this == Chase;
        }
    }

    private MekhaneTimeline() {
    }

    public static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    public static float window(int Tick, int Start, int End) {
        return smooth((Tick - Start) / (float) Math.max(1, End - Start));
    }

    public static Act act(int Tick) {
        int Found = Act.Idle.ordinal();
        for (int[] Entry : Acts) {
            if (Tick >= Entry[0]) {
                Found = Entry[1];
            }
        }
        return Act.of(Found);
    }

    public static int damage(int Tick) {
        int Found = 0;
        for (int[] Entry : Damage) {
            if (Tick >= Entry[0]) {
                Found = Entry[1];
            }
        }
        return Found;
    }

    public static int beatStart(int Tick) {
        int Found = 0;
        for (int Beat : Beats) {
            if (Tick >= Beat) {
                Found = Beat;
            }
        }
        return Found;
    }

    public static float dash(int Tick) {
        for (int[] Window : Dashes) {
            if (Tick >= Window[0] && Tick <= Window[2]) {
                if (Tick <= Window[1]) {
                    return smooth((Tick - Window[0]) / (float) (Window[1] - Window[0]));
                }
                return 1.0f - smooth((Tick - Window[1]) / (float) (Window[2] - Window[1]));
            }
        }
        return 0.0f;
    }

    public static float descent(int SummonTick) {
        return 1.0f - window(SummonTick, DescentStart, SummonTicks);
    }

    public static float rise(int DuelTick) {
        return window(DuelTick, Ascend, DuelTicks - 60);
    }

    public static float charge(int DuelTick) {
        if (DuelTick < Climax) {
            return window(DuelTick, 0, 200);
        }
        if (DuelTick < Banish) {
            return 1.0f + window(DuelTick, 2480, LaserStart);
        }
        return 1.0f - window(DuelTick, Banish, Resolve + 60);
    }

    public static float laser(int DuelTick) {
        if (DuelTick < LaserStart || DuelTick >= Banish) {
            return 0.0f;
        }
        return window(DuelTick, LaserStart, LaserFull);
    }

    public static float portal(int DuelTick) {
        if (DuelTick < PortalOpen || DuelTick >= Banish + 15) {
            return 0.0f;
        }
        if (DuelTick >= Banish) {
            return 1.0f - (DuelTick - Banish) / 15.0f;
        }
        return window(DuelTick, PortalOpen, PortalFull);
    }

    public static float bind(int DuelTick) {
        return Mth.clamp((DuelTick - BindStart) / (float) (Banish - BindStart), 0.0f, 1.0f);
    }

    public static float dissolve(int DuelTick) {
        return window(DuelTick, DissolveStart, BeamFade);
    }

    public static float beam(MekhaneProtocol.Stage Stage, int Elapsed) {
        if (Stage == MekhaneProtocol.Stage.Summon) {
            return window(Elapsed, BeamCharge, BeamFull);
        }
        if (Stage == MekhaneProtocol.Stage.Duel) {
            return 1.0f - window(Elapsed, BeamFade, DuelTicks);
        }
        return 0.0f;
    }
}
