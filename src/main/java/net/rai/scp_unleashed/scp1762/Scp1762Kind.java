package net.rai.scp_unleashed.scp1762;

public enum Scp1762Kind {
    Wyvern("wyvern", 0.5f, 0.35f, true, true, 0.0126, 0.0126, 0.5f),
    Wyrm("wyrm", 0.45f, 0.3f, true, false, 0.03, 0.03, 0.5f),
    Drake("drake", 0.4f, 0.3f, false, true, 0.01042, 0.02951, 0.5f),
    Dragon("dragon", 0.6f, 0.45f, true, true, 0.01315, 0.01315, 0.5f);

    public final String Name;
    public final float Width;
    public final float Height;
    public final boolean Flies;
    public final boolean Legs;
    public final double WalkClip;
    public final double RunClip;
    public final float Scale;

    Scp1762Kind(String Name, float Width, float Height, boolean Flies, boolean Legs, double WalkClip, double RunClip, float Scale) {
        this.Name = Name;
        this.Width = Width;
        this.Height = Height;
        this.Flies = Flies;
        this.Legs = Legs;
        this.WalkClip = WalkClip;
        this.RunClip = RunClip;
        this.Scale = Scale;
    }

    public String id() {
        return "scp1762_" + Name;
    }

    public String walkClip() {
        return this == Wyrm ? "slither" : "walk";
    }

    public String restClip() {
        return "idle";
    }
}
