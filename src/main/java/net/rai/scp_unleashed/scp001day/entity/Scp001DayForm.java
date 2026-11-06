package net.rai.scp_unleashed.scp001day.entity;

public enum Scp001DayForm {
    Crawler("crawler", 1, 8.0, 0.36, 1, 1.6, 0.9, 0.16, 0.7f, 0.55f),
    Clinger("clinger", 2, 12.0, 0.29, 2, 2.0, 0.8, 0.2, 1.2f, 0.6f),
    Lurker("lurker", 4, 24.0, 0.27, 3, 1.9, 0.75, 0.26, 0.7f, 2.1f),
    Mass("mass", 6, 40.0, 0.17, 5, 2.6, 0.55, 0.34, 1.8f, 1.4f);

    public static final int MassCap = 24;
    public static final int FuseFloor = 6;
    public static final int PileFloor = 12;
    public static final int PileLimbs = 8;
    public static final int PileVariants = 3;

    public static int pileLimbs(int Mass) {
        return Math.max(3, Math.min(PileLimbs, 3 + (Mass - PileFloor) / 2));
    }

    public final String Key;
    public final int Weight;
    public final double Health;
    public final double Speed;
    public final int Grip;
    public final double Reach;
    public final double DragSpeed;
    public final double Pull;
    public final float Width;
    public final float Height;

    Scp001DayForm(String Key, int Weight, double Health, double Speed, int Grip, double Reach, double DragSpeed, double Pull, float Width, float Height) {
        this.Key = Key;
        this.Weight = Weight;
        this.Health = Health;
        this.Speed = Speed;
        this.Grip = Grip;
        this.Reach = Reach;
        this.DragSpeed = DragSpeed;
        this.Pull = Pull;
        this.Width = Width;
        this.Height = Height;
    }

    public static Scp001DayForm forMass(int Mass) {
        if (Mass >= FuseFloor) {
            return Scp001DayForm.Mass;
        }
        if (Mass >= Lurker.Weight) {
            return Lurker;
        }
        if (Mass >= Clinger.Weight) {
            return Clinger;
        }
        return Crawler;
    }
}
