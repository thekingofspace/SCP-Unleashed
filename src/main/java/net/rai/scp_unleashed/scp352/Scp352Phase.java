package net.rai.scp_unleashed.scp352;

import net.minecraft.util.Mth;

public enum Scp352Phase {
    Wander("wandering", null, 0, 0, Scp352Phase.Low, Scp352Phase.Low),
    Transform("rising", "transform", 40, 18, Scp352Phase.Low, Scp352Phase.Tall),
    Hunt("hunting", null, 0, 0, Scp352Phase.Tall, Scp352Phase.Tall),
    Fold("folding", "fold", 30, 20, Scp352Phase.Tall, Scp352Phase.Low),
    Seek("prowling", null, 0, 0, Scp352Phase.Tall, Scp352Phase.Tall),
    Settle("settling", "settle", 40, 26, Scp352Phase.Tall, Scp352Phase.Roost),
    Rest("resting", "rest", 0, 0, Scp352Phase.Roost, Scp352Phase.Roost),
    Wake("waking", "wake", 40, 24, Scp352Phase.Roost, Scp352Phase.Low),
    Rouse("roused", "rouse", 20, 8, Scp352Phase.Roost, Scp352Phase.Tall);

    public static final int Low = 0;
    public static final int Tall = 1;
    public static final int Roost = 2;

    private final String Word;
    private final String Clip;
    private final int Ticks;
    private final int Swap;
    private final int Before;
    private final int After;

    Scp352Phase(String Word, String Clip, int Ticks, int Swap, int Before, int After) {
        this.Word = Word;
        this.Clip = Clip;
        this.Ticks = Ticks;
        this.Swap = Swap;
        this.Before = Before;
        this.After = After;
    }

    public String word() {
        return Word;
    }

    public String clip() {
        return Clip;
    }

    public int ticks() {
        return Ticks;
    }

    public boolean timed() {
        return Ticks > 0;
    }

    public int size(long Age) {
        return Age < Swap ? Before : After;
    }

    public boolean tall() {
        return this == Hunt || this == Seek;
    }

    public boolean shielded() {
        return this == Transform || this == Rouse;
    }

    public static Scp352Phase byId(int Id) {
        Scp352Phase[] All = values();
        return All[Mth.clamp(Id, 0, All.length - 1)];
    }
}
