package net.rai.scp_unleashed.scp024.plan;

public enum Scp024Mat {
    Air(false, 0),
    Barrier(true, 0),
    Light(false, 15),
    DimLight(false, 5),
    Siding(true, 0),
    Concrete(true, 0),
    Roof(true, 0),
    Trim(true, 0),
    DockDoor(true, 0),
    Bumper(false, 0),
    Sign(false, 0),
    Lamp(false, 11),
    StageDoor(false, 0),
    ExitDoor(false, 13),
    StageFloor(true, 0),
    StageWall(true, 0),
    Marquee(true, 15),
    Curtain(true, 0),
    BlackConcrete(true, 0),
    WhiteConcrete(true, 0),
    RedConcrete(true, 0),
    YellowConcrete(true, 0),
    LimeConcrete(true, 0),
    BlueConcrete(true, 0),
    OrangeConcrete(true, 0),
    PurpleConcrete(true, 0),
    GrayConcrete(true, 0),
    LightBlueConcrete(true, 0),
    MagentaConcrete(true, 0),
    GreenConcrete(true, 0),
    CyanConcrete(true, 0),
    PinkConcrete(true, 0),
    OrangeTerracotta(true, 0),
    PackedIce(true, 0),
    BlueIce(true, 0),
    Magma(true, 3),
    GoldBlock(true, 0),
    SeaLantern(true, 15),
    IronBars(false, 0),
    StonePlate(false, 0),
    StoneButton(false, 0),
    IronTrapdoor(false, 0),
    Target(true, 0),
    Ladder(false, 0),
    Seat(false, 0),
    Dispenser(true, 0),
    SmoothSlab(false, 0),
    Lava(false, 15);

    public final boolean Solid;
    public final int Glow;

    Scp024Mat(boolean Solid, int Glow) {
        this.Solid = Solid;
        this.Glow = Glow;
    }

    public boolean isAir() {
        return this == Air;
    }

    public boolean isLight() {
        return this == Light || this == DimLight;
    }
}
