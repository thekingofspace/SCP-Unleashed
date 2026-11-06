package net.rai.scp_unleashed.scp1730.plan;

public final class Scp1730Blocks {
    public static final String Mod = "scp_unleashed:";
    public static final String Seal = Mod + "scp1730_seal";
    public static final String Turret = Mod + "scp1730_turret";
    public static final String Laser = Mod + "scp1730_laser_emitter";
    public static final String Control = Mod + "scp1730_control";
    public static final String Card = Mod + "scp1730_card";
    public static final String Thresher = Mod + "scp1730_thresher";
    public static final String Symbol = Mod + "scp1730_symbol";
    public static final String Crack = Mod + "scp1730_crack";
    public static final String PitVoid = Mod + "scp1730_pit_void";
    public static final String EmergencyLight = Mod + "scp1730_emergency_light";
    public static final String AlarmLight = Mod + "scp1730_alarm_light";
    public static final String BrokenLight = Mod + "scp1730_broken_light";
    public static final String DeadLight = Mod + "scp1730_dead_light";
    public static final String OverturnedChair = Mod + "scp1730_overturned_chair";
    public static final String OverturnedDesk = Mod + "scp1730_overturned_desk";
    public static final String CellDoorWreck = Mod + "scp1730_cell_door_wreck";
    public static final String CrackedTerminal = Mod + "scp1730_cracked_terminal";
    public static final String Gurney = Mod + "scp1730_gurney";
    public static final String FallenPanel = Mod + "scp1730_fallen_panel";
    public static final String CableTangle = Mod + "scp1730_cable_tangle";
    public static final String BodyBag = Mod + "scp1730_body_bag";
    public static final String Conduit = Mod + "scp1730_conduit";
    public static final String BrokenVent = Mod + "scp1730_broken_vent";
    public static final String HangingCables = Mod + "scp1730_hanging_cables";
    public static final String BoardedWindow = Mod + "scp1730_boarded_window";
    public static final String Drain = Mod + "scp1730_drain";
    public static final String SignMemetic = Mod + "scp1730_sign_memetic";
    public static final String SignVoltage = Mod + "scp1730_sign_high_voltage";
    public static final String SignBiohazard = Mod + "scp1730_sign_biohazard";
    public static final String SignKeter = Mod + "scp1730_sign_keter";
    public static final String SignSector = Mod + "scp1730_sign_sector_";
    public static final String PaperStack = Mod + "paper_stack";
    public static final String RubbleFloor = Mod + "scp1730_rubble_floor";
    public static final String GlassShards = Mod + "scp1730_glass_shards";
    public static final String BrokenGlass = Mod + "scp1730_broken_glass";
    public static final String Sandbags = Mod + "scp1730_sandbags";
    public static final String Barricade = Mod + "scp1730_barricade";
    public static final String OverturnedBench = Mod + "scp1730_overturned_bench";
    public static final String MouldPatch = Mod + "scp1730_mould";
    public static final String DanglingPanel = Mod + "scp1730_dangling_panel";
    public static final String Rubble = Mod + "scp1730_rubble";
    public static final String WeaponRack = Mod + "scp1730_weapon_rack";
    public static final String AmmoCrate = Mod + "scp1730_ammo_crate";
    public static final String Pipes = Mod + "scp1730_pipes";
    public static final String Turnstile = Mod + "scp1730_turnstile";
    public static final String CableTray = Mod + "scp1730_cable_tray";
    public static final String DragMarks = Mod + "scp1730_drag_marks";
    public static final String RedRim = Mod + "scp1730_red_rim";
    public static final String RedGlow = Mod + "scp1730_red_glow";
    public static final String Shutter = Mod + "scp1730_shutter";
    public static final String HeavyDoor = Mod + "scp1730_heavy_door";
    public static final String ElevatorLeft = Mod + "scp1730_elevator_door_left";
    public static final String ElevatorRight = Mod + "scp1730_elevator_door_right";
    public static final String Generator = Mod + "scp1730_generator";
    public static final String ChamberStone = Mod + "scp1730_chamber_stone";
    public static final String ChamberGlyph = Mod + "scp1730_chamber_glyph";
    public static final String ChamberFloor = Mod + "scp1730_chamber_floor";
    public static final String BronzeInlay = Mod + "scp1730_bronze_inlay";
    public static final String Fluorescent = Mod + "fluorescent_light[face=ceiling,facing=north,lit=true]";
    public static final String Chain = "minecraft:chain[axis=y]";
    public static final String Glass = "minecraft:glass";
    public static final String Water = "minecraft:water";
    public static final String Falling = "minecraft:water[level=8]";
    public static final String Ladder = Mod + "industrial_ladder";
    public static final String Catwalk = Mod + "catwalk";
    public static final String Railing = Mod + "safety_railing";
    public static final String RailingYellow = Mod + "safety_railing_yellow";
    public static final String Pipe = Mod + "decor_pipe";
    public static final String WallPipes = Mod + "lcz_wall_pipes";
    public static final String PipeElbow = Mod + "decor_pipe_elbow";
    public static final String PipeValve = Mod + "decor_pipe_valve";
    public static final String Grate = Mod + "metal_grate";
    public static final String ShowerTile = Mod + "facility_shower_tile";
    public static final String Air = null;

    public static class Palette {
        public final String Floor;
        public final String FloorMarked;
        public final String WallBottom;
        public final String WallMiddle;
        public final String WallTop;
        public final String Ceiling;
        public final String Lamp;
        public final String Stairs;
        public final String Pillar;
        public final String Window;

        public Palette(String Set) {
            Floor = Mod + Set + "_floor";
            FloorMarked = Mod + Set + "_floor_marked";
            WallBottom = Mod + Set + "_wall_bottom";
            WallMiddle = Mod + Set + "_wall_middle";
            WallTop = Mod + Set + "_wall_top";
            Ceiling = Mod + Set + "_ceiling";
            Lamp = Mod + Set + "_lamp[lit=false]";
            Stairs = Mod + Set + "_stairs";
            Pillar = Mod + Set + "_pillar";
            Window = Mod + Set + "_wall_window";
        }

        public String wall(int Level, int Height) {
            if (Level <= 1) {
                return WallBottom;
            }
            return Level >= Height ? WallTop : WallMiddle;
        }

        public String marked(String Facing) {
            return FloorMarked + "[facing=" + Facing + "]";
        }

        public String stairs(String Facing) {
            return Stairs + "[facing=" + Facing + ",half=bottom,shape=straight]";
        }

        public String pillar(String Part) {
            return Pillar + "[axis=y,part=" + Part + "]";
        }

        public String window(String Axis) {
            return Window + "[axis=" + Axis + "]";
        }
    }

    public static final Palette Ez = new Palette("ez");
    public static final Palette Lcz = new Palette("lcz");
    public static final Palette Hcz = new Palette("hcz");
    public static final Palette Office = new Palette("office");
    public static final Palette Medical = new Palette("medical");
    public static final Palette Research = new Palette("research");
    public static final Palette Power = new Palette("power");
    public static final Palette Containment = new Palette("containment_room");

    private Scp1730Blocks() {
    }

    public static String facing(String Base, String Facing) {
        return Base + "[facing=" + Facing + "]";
    }

    public static boolean water(String State) {
        return State != null && (State.startsWith(Water) || State.contains("waterlogged=true"));
    }

    public static String with(String Base, String Props) {
        return Base + "[" + Props + "]";
    }

    public static String opposite(String Facing) {
        return switch (Facing) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            case "west" -> "east";
            case "up" -> "down";
            default -> "up";
        };
    }

    public static boolean passable(String State) {
        if (State == null) {
            return true;
        }
        if (Scp1730PuzzlePlan.passable(State)) {
            return true;
        }
        if (State.startsWith(Water) || State.startsWith(Ladder)) {
            return true;
        }
        if (State.startsWith(Mod + "tesla_gate")) {
            return !State.contains("part=4") && !State.contains("part=5");
        }
        return State.startsWith(Symbol) || State.startsWith(EmergencyLight) || State.startsWith(AlarmLight) || State.startsWith(BrokenLight)
                || State.startsWith(DeadLight) || State.startsWith(HangingCables) || State.startsWith(DragMarks) || State.startsWith(Card)
                || State.startsWith(Conduit) || State.startsWith(BrokenVent) || State.startsWith(Mod + "scp1730_sign_") || State.startsWith(GlassShards)
                || State.startsWith(MouldPatch) || State.startsWith(DanglingPanel)
                || State.startsWith(CableTray) || State.startsWith(Chain) || State.startsWith(Mod + "fluorescent_light") || State.startsWith(Control)
                || State.startsWith(Mod + "facility_sign_") || State.startsWith(Mod + "paper_stack") || State.startsWith(Mod + "fire_extinguisher")
                || State.startsWith(Mod + "security_camera") || State.startsWith(Mod + "keycard_scanner") || State.startsWith(Mod + "containment_room_hazard_sign")
                || State.startsWith(Mod + "containment_room_cell_plaque") || State.startsWith(Turret) || State.startsWith(Mod + "wall_clock");
    }
}
