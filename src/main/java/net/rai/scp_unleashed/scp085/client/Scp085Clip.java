package net.rai.scp_unleashed.scp085.client;

public enum Scp085Clip {
    Stand("stand", 0, 2, 31),
    Walk("walk", 2, 8, 31),
    Front("front", 10, 2, 31),
    Wave("wave", 12, 4, 31),
    Hold("hold", 16, 2, 31),
    SitSide("sit_side", 18, 2, 25),
    SitFront("sit_front", 20, 2, 25),
    SitGround("sit_ground", 22, 2, 31),
    Lie("lie", 24, 2, 30),
    Crouch("crouch", 26, 4, 31),
    LookOut("lookout", 30, 2, 31),
    Write("write", 32, 4, 31),
    Twirl("twirl", 36, 4, 31),
    Leap("leap", 40, 7, 31),
    Scribble("scribble", 47, 4, 31),
    CatWalk("cat_walk", 51, 6, 31),
    CatSit("cat_sit", 57, 2, 31),
    BirdHop("bird_hop", 59, 2, 31),
    BirdPeck("bird_peck", 61, 2, 31),
    BirdFly("bird_fly", 63, 4, 31),
    RabbitHop("rabbit_hop", 67, 4, 31),
    RabbitSit("rabbit_sit", 71, 2, 31),
    Sign("sign", 73, 8, 31),
    Note("note", 81, 4, 31),
    Sleep("sleep", 85, 2, 31),
    Heart("heart", 87, 1, 31);

    public static final int Cell = 32;
    public static final int Columns = 16;
    public static final float AtlasWidth = 512.0f;
    public static final float AtlasHeight = 256.0f;

    private final String Name;
    private final int Start;
    private final int Count;
    private final int Anchor;

    Scp085Clip(String Name, int Start, int Count, int Anchor) {
        this.Name = Name;
        this.Start = Start;
        this.Count = Count;
        this.Anchor = Anchor;
    }

    public String clipName() {
        return Name;
    }

    public int cell(int Frame) {
        return Start + Math.floorMod(Frame, Count);
    }

    public int count() {
        return Count;
    }

    public int anchor() {
        return Anchor;
    }
}
