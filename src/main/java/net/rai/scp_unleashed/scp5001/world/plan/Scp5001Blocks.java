package net.rai.scp_unleashed.scp5001.world.plan;

public final class Scp5001Blocks {
    public static final String Mod = "scp_unleashed:";
    public static final String Air = "minecraft:air";
    public static final String Shell = Mod + "scp5001_shell[seam=false]";
    public static final String ShellSeam = Mod + "scp5001_shell[seam=true]";
    public static final String Panel = Mod + "scp5001_arch_panel";
    public static final String PanelSeam = Mod + "scp5001_arch_panel_seam";
    public static final String PanelGlyph = Mod + "scp5001_arch_panel_glyph";
    public static final String Graphite = Mod + "scp5001_arch_graphite";
    public static final String GraphiteSeam = Mod + "scp5001_arch_graphite_seam";
    public static final String GraphitePanel = Mod + "scp5001_arch_graphite_panel";
    public static final String Rib = Mod + "scp5001_arch_rib";
    public static final String Hatch = Mod + "scp5001_arch_hatch";
    public static final String Vent = Mod + "scp5001_arch_vent";
    public static final String Floor = Mod + "scp5001_arch_floor";
    public static final String FloorHex = Mod + "scp5001_arch_floor_hex";
    public static final String FloorGrate = Mod + "scp5001_arch_floor_grate";
    public static final String FloorSeam = Mod + "scp5001_arch_floor_seam";
    public static final String FloorGlyph = Mod + "scp5001_arch_floor_glyph";
    public static final String ChamberFloor = Mod + "scp5001_arch_chamber_floor";
    public static final String Ceiling = Mod + "scp5001_arch_ceiling";
    public static final String CeilingLight = Mod + "scp5001_arch_ceiling_light";
    public static final String Glass = Mod + "scp5001_arch_glass";
    public static final String Pillar = Mod + "scp5001_arch_pillar";
    public static final String Trim = Mod + "scp5001_arch_trim";
    public static final String TrimAmber = Mod + "scp5001_arch_trim_amber";
    public static final String ChamberWall = Mod + "scp5001_arch_chamber_wall";
    public static final String Stairs = Mod + "scp5001_arch_stairs";
    public static final String Slab = Mod + "scp5001_arch_slab";
    public static final String Railing = Mod + "scp5001_arch_railing";
    public static final String Door = Mod + "scp5001_arch_door";
    public static final String EntryDoor = Mod + "scp5001_entry_door";
    public static final String Engine = Mod + "scp5001_engine";
    public static final String Terminal = Mod + "scp5001_data_terminal";
    public static final String ChamberCore = Mod + "scp5001_chamber_core";
    public static final String RodEmitter = Mod + "scp5001_rod_emitter";
    public static final String LaserEmitter = Mod + "scp5001_laser_emitter";
    public static final String ConsoleMain = Mod + "scp5001_console_main";
    public static final String Generator = Mod + "scp5001_generator";
    public static final String GeneratorConsole = Mod + "scp5001_generator_console";
    public static final String EmergencyConsole = Mod + "scp5001_emergency_console";
    public static final String EmergencyIntake = Mod + "scp5001_emergency_intake";
    public static final String SkyEmitter = Mod + "scp5001_sky_emitter";
    public static final String Concrete = "minecraft:gray_concrete";
    public static final String ConcreteLight = "minecraft:light_gray_concrete";
    public static final String ShaftStairs = "minecraft:polished_andesite_stairs";
    public static final String ShaftFloor = "minecraft:smooth_stone";
    public static final String Lantern = "minecraft:lantern[hanging=true]";
    public static final String IronDoor = "minecraft:iron_door";
    public static final String Button = "minecraft:stone_button";
    public static final String IronTrapdoor = "minecraft:iron_trapdoor";

    private Scp5001Blocks() {
    }

    public static String facing(String Id, String Facing) {
        return Id + "[facing=" + Facing + "]";
    }

    public static String idOf(String State) {
        int Bracket = State.indexOf('[');
        return Bracket < 0 ? State : State.substring(0, Bracket);
    }

    public static boolean eventBlock(String Id) {
        return Id.equals(ChamberCore) || Id.equals(RodEmitter) || Id.equals(LaserEmitter) || Id.equals(ConsoleMain) || Id.equals(Generator) || Id.equals(GeneratorConsole)
                || Id.equals(EmergencyConsole) || Id.equals(EmergencyIntake) || Id.equals(SkyEmitter);
    }

    public static boolean solid(String State) {
        String Id = idOf(State);
        return Id.equals(Graphite) || Id.equals(GraphiteSeam) || Id.equals(GraphitePanel) || Id.equals(Rib) || Id.equals(Hatch) || Id.equals(Panel) || Id.equals(PanelSeam) || Id.equals(PanelGlyph) || Id.equals(Floor) || Id.equals(FloorHex)
                || Id.equals(FloorGrate) || Id.equals(FloorSeam) || Id.equals(FloorGlyph) || Id.equals(ChamberFloor) || Id.equals(Ceiling) || Id.equals(CeilingLight)
                || Id.equals(Trim) || Id.equals(TrimAmber) || Id.equals(ChamberWall) || Id.equals(Vent) || Id.equals(Glass) || Id.startsWith(Mod + "scp5001_shell")
                || Id.equals(Concrete) || Id.equals(ConcreteLight) || Id.equals(ShaftFloor);
    }
}
