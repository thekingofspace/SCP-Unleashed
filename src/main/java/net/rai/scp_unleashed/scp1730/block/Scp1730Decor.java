package net.rai.scp_unleashed.scp1730.block;

public enum Scp1730Decor {
    EmergencyLight("scp1730_emergency_light", Kind.Light, Shape.None, 10, false),
    AlarmLight("scp1730_alarm_light", Kind.Light, Shape.None, 8, false),
    BrokenLight("scp1730_broken_light", Kind.Light, Shape.None, 7, true),
    DeadLight("scp1730_dead_light", Kind.Light, Shape.None, 0, false),
    OverturnedChair("scp1730_overturned_chair", Kind.Prop, Shape.Low, 0, false),
    OverturnedDesk("scp1730_overturned_desk", Kind.Prop, Shape.Full, 0, false),
    CrackedTerminal("scp1730_cracked_terminal", Kind.Prop, Shape.Half, 0, false),
    Gurney("scp1730_gurney", Kind.Prop, Shape.Half, 0, false),
    FallenPanel("scp1730_fallen_panel", Kind.Prop, Shape.Low, 0, false),
    CableTangle("scp1730_cable_tangle", Kind.Prop, Shape.Low, 0, false),
    BodyBag("scp1730_body_bag", Kind.Prop, Shape.Low, 0, false),
    CellDoorWreck("scp1730_cell_door_wreck", Kind.Prop, Shape.Low, 0, false),
    Rubble("scp1730_rubble", Kind.Prop, Shape.Low, 0, false),
    WeaponRack("scp1730_weapon_rack", Kind.Prop, Shape.Wall, 0, false),
    AmmoCrate("scp1730_ammo_crate", Kind.Prop, Shape.Half, 0, false),
    Conduit("scp1730_conduit", Kind.Prop, Shape.Wall, 0, false),
    BrokenVent("scp1730_broken_vent", Kind.Prop, Shape.Wall, 0, false),
    SignMemetic("scp1730_sign_memetic", Kind.Prop, Shape.Wall, 0, false),
    SignHighVoltage("scp1730_sign_high_voltage", Kind.Prop, Shape.Wall, 0, false),
    SignBiohazard("scp1730_sign_biohazard", Kind.Prop, Shape.Wall, 0, false),
    SignKeter("scp1730_sign_keter", Kind.Prop, Shape.Wall, 0, false),
    SignSectorA("scp1730_sign_sector_a", Kind.Prop, Shape.Wall, 0, false),
    SignSectorB("scp1730_sign_sector_b", Kind.Prop, Shape.Wall, 0, false),
    SignSectorC("scp1730_sign_sector_c", Kind.Prop, Shape.Wall, 0, false),
    SignSectorD("scp1730_sign_sector_d", Kind.Prop, Shape.Wall, 0, false),
    DragMarks("scp1730_drag_marks", Kind.Decal, Shape.None, 0, false),
    CableTray("scp1730_cable_tray", Kind.Decal, Shape.None, 0, false),
    HangingCables("scp1730_hanging_cables", Kind.Decal, Shape.None, 0, false),
    RedRim("scp1730_red_rim", Kind.Cube, Shape.Full, 0, false),
    RedGlow("scp1730_red_glow", Kind.Cube, Shape.Full, 11, false),
    ChamberStone("scp1730_chamber_stone", Kind.Cube, Shape.Full, 0, false),
    ChamberFloor("scp1730_chamber_floor", Kind.Cube, Shape.Full, 0, false),
    BronzeInlay("scp1730_bronze_inlay", Kind.Cube, Shape.Full, 0, false),
    Drain("scp1730_drain", Kind.Cube, Shape.Full, 0, false),
    ChamberGlyph("scp1730_chamber_glyph", Kind.FacingCube, Shape.Full, 0, false),
    Shutter("scp1730_shutter", Kind.FacingCube, Shape.Full, 0, false),
    HeavyDoor("scp1730_heavy_door", Kind.FacingCube, Shape.Full, 0, false),
    ElevatorDoorLeft("scp1730_elevator_door_left", Kind.FacingCube, Shape.Full, 0, false),
    ElevatorDoorRight("scp1730_elevator_door_right", Kind.FacingCube, Shape.Full, 0, false),
    Generator("scp1730_generator", Kind.FacingCube, Shape.Full, 0, false),
    BoardedWindow("scp1730_boarded_window", Kind.FacingCube, Shape.Full, 0, false),
    EzFloorCracked("scp1730_ez_floor_cracked", Kind.Cube, Shape.Full, 0, false),
    EzFloorBroken("scp1730_ez_floor_broken", Kind.Cube, Shape.Full, 0, false),
    EzWallHoled("scp1730_ez_wall_holed", Kind.Cube, Shape.Full, 0, false),
    EzWallStained("scp1730_ez_wall_stained", Kind.Cube, Shape.Full, 0, false),
    LczFloorCracked("scp1730_lcz_floor_cracked", Kind.Cube, Shape.Full, 0, false),
    LczFloorBroken("scp1730_lcz_floor_broken", Kind.Cube, Shape.Full, 0, false),
    LczWallHoled("scp1730_lcz_wall_holed", Kind.Cube, Shape.Full, 0, false),
    LczWallStained("scp1730_lcz_wall_stained", Kind.Cube, Shape.Full, 0, false),
    RubbleFloor("scp1730_rubble_floor", Kind.Cube, Shape.Full, 0, false),
    GlassShards("scp1730_glass_shards", Kind.Decal, Shape.None, 0, false),
    Mould("scp1730_mould", Kind.Decal, Shape.None, 0, false),
    BrokenGlass("scp1730_broken_glass", Kind.Prop, Shape.Full, 0, false),
    Sandbags("scp1730_sandbags", Kind.Prop, Shape.Full, 0, false),
    Barricade("scp1730_barricade", Kind.Prop, Shape.Full, 0, false),
    OverturnedBench("scp1730_overturned_bench", Kind.Prop, Shape.Half, 0, false),
    DanglingPanel("scp1730_dangling_panel", Kind.Decal, Shape.None, 0, false);

    public enum Kind {
        Cube, FacingCube, Prop, Decal, Light
    }

    public enum Shape {
        Full, Half, Low, Wall, None
    }

    private final String Name;
    private final Kind Type;
    private final Shape Form;
    private final int Glow;
    private final boolean Sparking;

    Scp1730Decor(String Name, Kind Type, Shape Form, int Glow, boolean Sparking) {
        this.Name = Name;
        this.Type = Type;
        this.Form = Form;
        this.Glow = Glow;
        this.Sparking = Sparking;
    }

    public String id() {
        return Name;
    }

    public Kind kind() {
        return Type;
    }

    public Shape shape() {
        return Form;
    }

    public int glow() {
        return Glow;
    }

    public boolean sparks() {
        return Sparking;
    }
}
