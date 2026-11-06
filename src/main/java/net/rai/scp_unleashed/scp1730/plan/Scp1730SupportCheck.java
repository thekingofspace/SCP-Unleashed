package net.rai.scp_unleashed.scp1730.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Scp1730SupportCheck {
    public static final String Curtain = Scp1730Blocks.Mod + "medical_curtain_";
    public static final Set<String> Paired = Set.of("iv_stand", "privacy_screen", "coat_rack", "locker", "water_cooler", "vending_machine", "whiteboard");
    public static final Set<String> Hanging = Set.of(Scp1730Blocks.HangingCables, Scp1730Blocks.DanglingPanel, Scp1730Blocks.CableTray);
    public static final Set<String> WallOnly = Set.of("containment_room_sedation_vent", "containment_room_status_panel", "containment_room_cell_plaque", "containment_room_alarm_light",
            "containment_room_hazard_sign", "hcz_valve_panel", "lcz_notice_board", "lcz_intercom", "lcz_wall_pipes", "ez_directory_sign", "medical_cabinet", "medical_defib_cabinet",
            "scp1730_lockdown_panel", "scp1730_junction_box", "scp1730_terminal", "scp1730_clue_board", "scp1730_switch", "scp1730_valve", "scp1730_cable_junction",
            "scp1730_disposal_hatch", "scp1730_sluice_wheel");
    public static final Set<String> Screens = Set.of("hcz_monitor_console", "security_monitor", "crt_monitor", "computer_terminal", "wall_monitor", "scp1730_cracked_terminal",
            "vitals_monitor", "scp1730_terminal", "scp1730_studio_monitor");
    public static final Set<String> Seats = Set.of("office_chair", "lab_stool", "cafeteria_chair", "ez_waiting_chairs", "scp1730_overturned_chair");
    public static final Set<String> LowProps = Set.of("scp1730_body_bag", "scp1730_rubble", "scp1730_fallen_panel", "scp1730_cable_tangle", "scp1730_cell_door_wreck",
            "scp1730_overturned_chair", "scp1730_drag_marks", "scp1730_glass_shards", "scp1730_mould", "scp1730_gurney", "scp1730_overturned_bench", "scp1730_overturned_desk");
    public static final Set<String> Resting = Set.of("paper_stack", "crt_monitor", "security_monitor", "computer_terminal", "desk_lamp", "coffee_machine", "microwave",
            "research_microscope_station", "research_bench_centrifuge", "lab_tube_holder", "lab_analyzer", "lab_petri_dish", "archive_boxes", "supply_crate", "chemical_barrel",
            "biohazard_bin", "scp1730_cracked_terminal", "scp1730_ammo_crate", "office_chair", "lab_stool", "wheelchair", "medical_cart", "trash_can", "potted_plant",
            "mop_bucket", "wet_floor_sign", "vitals_monitor", "research_cryo_unit", "containment_room_restraint_anchor", "hcz_monitor_console", "scp1730_sandbags",
            "scp1730_barricade", "hcz_hazard_crate", "steel_shelf", "binder_shelf", "filing_cabinet", "chemical_shelf", "specimen_shelf", "lab_shelf");
    public static final Set<String> Carriers = Set.of("office_desk", "metal_desk", "lab_bench_desk", "lab_table", "reception_desk", "conference_table", "cafeteria_table",
            "steel_shelf", "binder_shelf", "filing_cabinet", "supply_crate", "hcz_hazard_crate", "archive_boxes", "scp1730_generator", "scp1730_ammo_crate", "chemical_barrel");
    public static final List<String> FullEnds = List.of("_floor", "_floor_marked", "_floor_cracked", "_floor_broken", "_floor_marked_corner", "_floor_marked_outer", "_floor_arrow",
            "_wall_bottom", "_wall_middle", "_wall_top", "_ceiling", "_pillar", "_lamp");
    public static final Set<String> FullBlocks = Set.of("scp1730_rubble_floor", "scp1730_drain", "facility_shower_tile", "scp1730_chamber_floor", "scp1730_chamber_stone",
            "scp1730_bronze_inlay", "scp1730_red_rim", "scp1730_red_glow", "scp1730_generator", "metal_grate", "facility_rubber_matting", "magma_block", "scp1730_heavy_door",
            "scp1730_shutter", "facility_carpet_maroon", "facility_concrete_cracked", "facility_lab_tile_cracked");
    public static final Set<String> Halves = Set.of("hospital_bed", "lcz_cell_bunk", "lcz_checkpoint_barrier");
    public static final List<String> FreeFacing = List.of("bed", "bunk", "chair", "stool", "stairs", "door", "seal", "sign", "bench", "couch", "_wall_", "scp1730_generator",
            "scp1730_shutter", "scp1730_boarded_window", "scp1730_chamber_glyph", "elevator", "turnstile", "railing", "lamp", "light", "plaque", "wheelchair", "gurney", "body_bag",
            "drag_marks", "rubble", "fallen_panel", "cable", "glass_shards", "mould", "decal", "vent", "pipes", "panel", "cell_door_wreck", "sandbags", "barricade", "paper_stack", "overturned_desk",
            "ammo_crate", "archive_boxes", "trash_can", "turret", "curtain", "iv_stand", "symbol", "crack", "laser", "card", "control", "pickup", "pylon");
    public static final List<String> OpenFront = List.of("rubble", "cable_tangle", "glass", "drag_marks", "paper_stack", "fallen_panel", "body_bag", "cell_door_wreck", "mould",
            "window");
    public static final Set<String> PipeParts = Set.of("decor_pipe", "decor_pipe_valve", "decor_pipe_elbow", "hcz_pipe_manifold");
    public static final Set<String> Rails = Set.of("safety_railing", "safety_railing_yellow");
    public static final String[] Posts = {"post_ne", "post_se", "post_sw", "post_nw"};
    public static final int PostEvery = 4;
    public static final Set<String> Joined = Set.of("office_desk", "metal_desk", "lab_bench_desk", "lab_table", "reception_desk", "couch_grey", "couch_blue", "couch_leather",
            "conference_table", "cafeteria_table");

    private Scp1730SupportCheck() {
    }

    public static boolean solid(String State) {
        return State != null && !Scp1730Blocks.passable(State);
    }

    static String id(String State) {
        int Cut = State.indexOf('[');
        return Cut < 0 ? State : State.substring(0, Cut);
    }

    public static boolean facesOpen(String Before) {
        return Scp1730Blocks.passable(Before) || Seats.contains(id(Before).replace(Scp1730Blocks.Mod, "")) || open(Before);
    }

    static boolean open(String State) {
        for (String Each : OpenFront) {
            if (id(State).contains(Each)) {
                return true;
            }
        }
        return false;
    }

    static String shortOf(String State) {
        return id(State).replace(Scp1730Blocks.Mod, "").replace("minecraft:", "");
    }

    public static boolean full(String State) {
        if (State == null) {
            return false;
        }
        String Short = shortOf(State);
        if (FullBlocks.contains(Short)) {
            return true;
        }
        for (String End : FullEnds) {
            if (Short.endsWith(End)) {
                return true;
            }
        }
        return false;
    }

    static boolean free(String Short) {
        for (String Each : FreeFacing) {
            if (Short.contains(Each)) {
                return true;
            }
        }
        return Rails.contains(Short) || PipeParts.contains(Short);
    }

    static boolean walled(String State) {
        if (State == null) {
            return false;
        }
        String Short = id(State).replace(Scp1730Blocks.Mod, "");
        return Short.endsWith("_wall_bottom") || Short.endsWith("_wall_middle") || Short.endsWith("_wall_top") || Short.equals("scp1730_chamber_stone") || Short.endsWith("_wall_window")
                || Short.startsWith("scp1730_ez_wall") || Short.startsWith("scp1730_lcz_wall");
    }

    static String axisOf(String Orientation) {
        if (Orientation == null) {
            return null;
        }
        int Cut = Orientation.lastIndexOf('_');
        return Cut < 0 ? null : Orientation.substring(Cut + 1);
    }

    static boolean edge(String State, int Dir) {
        return State != null && Rails.contains(id(State).replace(Scp1730Blocks.Mod, "")) && "true".equals(Scp1730RoomDressing.prop(State, Scp1730Layout.Facings[Dir]));
    }

    public static String posted(Scp1730RoomDressing.Cells G, int X, int Y, int Z, String State) {
        String Made = State;
        for (int First = 0; First < 4; First++) {
            int Second = (First + 1) % 4;
            boolean HasFirst = edge(State, First);
            boolean HasSecond = edge(State, Second);
            boolean Post = HasFirst && HasSecond;
            if (HasFirst != HasSecond) {
                int Run = HasFirst ? First : Second;
                int Toward = HasFirst ? Second : First;
                String Next = G.at(X + Scp1730Layout.Dx[Toward], Y, Z + Scp1730Layout.Dz[Toward]);
                boolean Low = Run % 2 == 0 ? First == 3 || Second == 3 : First == 0 || Second == 0;
                int Along = Run % 2 == 0 ? X : Z;
                Post = !edge(Next, Run) || Low && Math.floorMod(Along, PostEvery) == 0;
            }
            Made = Scp1730RoomDressing.with(Made, Posts[First], String.valueOf(Post));
        }
        return Made;
    }

    public static List<String> check(Scp1730Plan Plan) {
        List<int[]> Spots = new ArrayList<>();
        List<String> Texts = new ArrayList<>();
        Plan.paint(0, 0, Scp1730Plan.Width - 1, Scp1730Plan.Depth - 1, (X, Y, Z, Text) -> {
            String Id = id(Text);
            String Short = Id.startsWith(Scp1730Blocks.Mod) ? Id.substring(Scp1730Blocks.Mod.length()) : Id;
            if (Id.startsWith(Curtain) || Paired.contains(Short) || Hanging.contains(Id) || Id.equals("minecraft:chain") || Joined.contains(Short) || WallOnly.contains(Short)
                    || Short.equals("decor_pipe") || Short.equals("decor_pipe_valve") || Rails.contains(Short) || Halves.contains(Short) || Screens.contains(Short)
                    || Text.contains("facing=") && Id.startsWith(Scp1730Blocks.Mod) && !free(Short) || LowProps.contains(Short) || Resting.contains(Short)) {
                Spots.add(new int[]{X, Y, Z});
                Texts.add(Text);
            }
        });
        List<String> Problems = new ArrayList<>();
        for (int Index = 0; Index < Spots.size(); Index++) {
            int[] At = Spots.get(Index);
            String Text = Texts.get(Index);
            String Id = id(Text);
            String Short = Id.startsWith(Scp1730Blocks.Mod) ? Id.substring(Scp1730Blocks.Mod.length()) : Id;
            String Below = Plan.stateAt(At[0], At[1] - 1, At[2]);
            String Above = Plan.stateAt(At[0], At[1] + 1, At[2]);
            String Where = " at " + At[0] + "," + At[1] + "," + At[2];
            if (LowProps.contains(Short) && !full(Below)) {
                Problems.add(Text + Where + " lies on " + Below + " instead of a full floor block");
                continue;
            }
            if (Resting.contains(Short) && !full(Below) && !(Below != null && (Carriers.contains(shortOf(Below)) || id(Below).equals(Id) && Short.endsWith("crate")))) {
                Problems.add(Text + Where + " rests on " + Below + ", which cannot carry it");
            }
            String Front = Scp1730RoomDressing.prop(Text, "facing");
            int Ahead = Front == null || Front.equals("up") || Front.equals("down") ? -1 : Scp1730Builder.dirOf(Front);
            if (Halves.contains(Short) && Ahead >= 0) {
                String Part = Short.equals("hospital_bed") ? Scp1730RoomDressing.prop(Text, "part") : Scp1730RoomDressing.prop(Text, "pair");
                boolean First = "foot".equals(Part) || "main".equals(Part);
                int Toward = Short.equals("lcz_checkpoint_barrier") ? (Ahead + 3) % 4 : Ahead;
                int Way = First ? Toward : (Toward + 2) % 4;
                String Partner = Plan.stateAt(At[0] + Scp1730Layout.Dx[Way], At[1], At[2] + Scp1730Layout.Dz[Way]);
                String Wanted = Short.equals("hospital_bed") ? (First ? "head" : "foot") : (First ? "extension" : "main");
                String Key = Short.equals("hospital_bed") ? "part" : "pair";
                if (Partner == null || !id(Partner).equals(Id) || !Front.equals(Scp1730RoomDressing.prop(Partner, "facing")) || !Wanted.equals(Scp1730RoomDressing.prop(Partner, Key))) {
                    Problems.add(Text + Where + " is missing its " + Wanted + " half toward " + Scp1730Layout.Facings[Way] + " (" + Partner + ")");
                }
                continue;
            }
            if (Screens.contains(Short) && Ahead >= 0) {
                String Before = Plan.stateAt(At[0] + Scp1730Layout.Dx[Ahead], At[1], At[2] + Scp1730Layout.Dz[Ahead]);
                if (!facesOpen(Before)) {
                    Problems.add(Text + Where + " has its screen against " + Before);
                }
                if (!WallOnly.contains(Short)) {
                    continue;
                }
            }
            if (!WallOnly.contains(Short) && !free(Short) && Ahead >= 0 && Front != null && !Screens.contains(Short)) {
                String Before = Plan.stateAt(At[0] + Scp1730Layout.Dx[Ahead], At[1], At[2] + Scp1730Layout.Dz[Ahead]);
                String Behind = Plan.stateAt(At[0] - Scp1730Layout.Dx[Ahead], At[1], At[2] - Scp1730Layout.Dz[Ahead]);
                if (walled(Before) && !walled(Behind)) {
                    Problems.add(Text + Where + " faces into a wall with its back to the room");
                }
                if (!Paired.contains(Short) && !Joined.contains(Short)) {
                    continue;
                }
            }
            if (WallOnly.contains(Short)) {
                String Facing = Scp1730RoomDressing.prop(Text, "facing");
                int Dir = Facing == null ? -1 : Scp1730Builder.dirOf(Facing);
                if (Facing == null || Facing.equals("up") || Facing.equals("down")) {
                    Problems.add(Text + Where + " is a wall piece without a wall facing");
                } else if (!solid(Plan.stateAt(At[0] - Scp1730Layout.Dx[Dir], At[1], At[2] - Scp1730Layout.Dz[Dir]))) {
                    Problems.add(Text + Where + " has no wall behind it");
                }
                continue;
            }
            if (Short.equals("decor_pipe") || Short.equals("decor_pipe_valve")) {
                String Axis = Short.equals("decor_pipe") ? Scp1730RoomDressing.prop(Text, "axis") : axisOf(Scp1730RoomDressing.prop(Text, "orientation"));
                for (int Sign = -1; Sign <= 1; Sign += 2) {
                    int Ox = "x".equals(Axis) ? Sign : 0;
                    int Oy = "y".equals(Axis) ? Sign : 0;
                    int Oz = "z".equals(Axis) ? Sign : 0;
                    String End = Plan.stateAt(At[0] + Ox, At[1] + Oy, At[2] + Oz);
                    String EndShort = End == null ? "" : id(End).replace(Scp1730Blocks.Mod, "");
                    if (End == null || !PipeParts.contains(EndShort) && (!solid(End) || EndShort.equals("hcz_monitor_console") || Rails.contains(EndShort))) {
                        Problems.add(Text + Where + " has a loose end toward " + (Ox + Oy + Oz > 0 ? "+" : "-") + Axis + " (" + End + ")");
                    }
                }
                continue;
            }
            if (Rails.contains(Short)) {
                String Expected = posted(Plan::stateAt, At[0], At[1], At[2], Text);
                if (!Expected.equals(Text)) {
                    Problems.add(Text + Where + " has posts out of place (should be " + Expected + ")");
                }
                continue;
            }
            if (Id.startsWith(Curtain)) {
                boolean Bottom = Id.endsWith("_bottom");
                boolean Top = Id.endsWith("_top");
                if (Bottom && !solid(Below)) {
                    Problems.add(Text + Where + " has no floor");
                } else if (!Bottom && (Below == null || !id(Below).startsWith(Curtain))) {
                    Problems.add(Text + Where + " hangs without the curtain below");
                }
                if (Top && !solid(Above)) {
                    Problems.add(Text + Where + " has no ceiling for its track");
                } else if (!Top && (Above == null || !id(Above).startsWith(Curtain))) {
                    Problems.add(Text + Where + " stops short of the ceiling track");
                }
            } else if (Paired.contains(Short)) {
                boolean Lower = Text.contains("half=lower");
                if (Lower && !solid(Below)) {
                    Problems.add(Text + Where + " stands on nothing");
                } else if (Lower && (Above == null || !id(Above).equals(Id) || !Above.contains("half=upper"))) {
                    Problems.add(Text + Where + " lost its upper half");
                } else if (!Lower && (Below == null || !id(Below).equals(Id) || !Below.contains("half=lower"))) {
                    Problems.add(Text + Where + " floats without its lower half");
                }
            } else if (Id.equals("minecraft:chain")) {
                int Y = At[1] + 1;
                String Up = Plan.stateAt(At[0], Y, At[2]);
                while (Up != null && id(Up).equals("minecraft:chain")) {
                    Y++;
                    Up = Plan.stateAt(At[0], Y, At[2]);
                }
                if (!solid(Up)) {
                    Problems.add(Text + Where + " hangs from nothing");
                }
            } else if (Hanging.contains(Id) && !Text.contains("facing=up") && !solid(Above)) {
                Problems.add(Text + Where + " hangs from nothing");
            } else if (Joined.contains(Short)) {
                if (!solid(Below) && !(Below != null && id(Below).equals(Id))) {
                    Problems.add(Text + Where + " stands on nothing");
                }
                String Expected = Scp1730RoomDressing.joined(Plan::stateAt, At[0], At[1], At[2], Text);
                if (!Expected.equals(Text)) {
                    Problems.add(Text + Where + " joins a missing neighbour (should be " + Expected + ")");
                }
            }
        }
        return Problems;
    }
}
