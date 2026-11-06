package net.rai.scp_unleashed.decor;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public enum FacilityMaterial implements StringRepresentable {
    Concrete("concrete", MaterialKind.Solid, MapColor.STONE, SoundType.STONE, 2.0f, true),
    ConcreteCracked("concrete_cracked", MaterialKind.Solid, MapColor.STONE, SoundType.STONE, 2.0f, false),
    PrecastConcrete("precast_concrete", MaterialKind.Solid, MapColor.STONE, SoundType.STONE, 2.0f, false),
    CinderBlock("cinder_block", MaterialKind.Solid, MapColor.STONE, SoundType.STONE, 2.0f, true),
    PaintedConcrete("painted_concrete", MaterialKind.Solid, MapColor.QUARTZ, SoundType.STONE, 2.0f, true),
    WayfindingRed("wayfinding_red", MaterialKind.Solid, MapColor.QUARTZ, SoundType.STONE, 2.0f, false),
    WayfindingOrange("wayfinding_orange", MaterialKind.Solid, MapColor.QUARTZ, SoundType.STONE, 2.0f, false),
    WayfindingYellow("wayfinding_yellow", MaterialKind.Solid, MapColor.QUARTZ, SoundType.STONE, 2.0f, false),
    WayfindingGreen("wayfinding_green", MaterialKind.Solid, MapColor.QUARTZ, SoundType.STONE, 2.0f, false),
    WayfindingBlue("wayfinding_blue", MaterialKind.Solid, MapColor.QUARTZ, SoundType.STONE, 2.0f, false),
    Baseboard("baseboard", MaterialKind.Solid, MapColor.QUARTZ, SoundType.STONE, 2.0f, false),
    CrownTrim("crown_trim", MaterialKind.Solid, MapColor.QUARTZ, SoundType.STONE, 2.0f, false),
    DiamondPlate("diamond_plate", MaterialKind.Solid, MapColor.METAL, SoundType.METAL, 3.0f, true),
    CorrugatedSteel("corrugated_steel", MaterialKind.Solid, MapColor.METAL, SoundType.METAL, 3.0f, false),
    RivetedSteel("riveted_steel", MaterialKind.Solid, MapColor.COLOR_GRAY, SoundType.METAL, 3.0f, false),
    RivetedSteelRusted("riveted_steel_rusted", MaterialKind.Solid, MapColor.COLOR_BROWN, SoundType.METAL, 3.0f, false),
    MetalMesh("metal_mesh", MaterialKind.Mesh, MapColor.METAL, SoundType.METAL, 3.0f, false),
    LabTile("lab_tile", MaterialKind.Solid, MapColor.SNOW, SoundType.DEEPSLATE_TILES, 2.0f, true),
    LabTileCracked("lab_tile_cracked", MaterialKind.Solid, MapColor.SNOW, SoundType.DEEPSLATE_TILES, 2.0f, false),
    LabTileGrimy("lab_tile_grimy", MaterialKind.Solid, MapColor.TERRACOTTA_WHITE, SoundType.DEEPSLATE_TILES, 2.0f, false),
    MedicalTile("medical_tile", MaterialKind.Solid, MapColor.COLOR_LIGHT_BLUE, SoundType.DEEPSLATE_TILES, 2.0f, true),
    ShowerTile("shower_tile", MaterialKind.Solid, MapColor.COLOR_LIGHT_GRAY, SoundType.DEEPSLATE_TILES, 2.0f, true),
    PaddedWall("padded_wall", MaterialKind.Soft, MapColor.SNOW, SoundType.WOOL, 1.2f, false),
    PaddedWallStained("padded_wall_stained", MaterialKind.Soft, MapColor.TERRACOTTA_WHITE, SoundType.WOOL, 1.2f, false),
    Drywall("drywall", MaterialKind.Solid, MapColor.QUARTZ, SoundType.CALCITE, 1.5f, false),
    CarpetBlue("carpet_blue", MaterialKind.Soft, MapColor.COLOR_BLUE, SoundType.WOOL, 1.2f, true),
    CarpetMaroon("carpet_maroon", MaterialKind.Soft, MapColor.TERRACOTTA_RED, SoundType.WOOL, 1.2f, true),
    AcousticCeiling("acoustic_ceiling", MaterialKind.Solid, MapColor.SNOW, SoundType.CALCITE, 1.0f, false),
    Brick("brick", MaterialKind.Solid, MapColor.COLOR_RED, SoundType.STONE, 2.0f, true),
    ColdRoomWall("cold_room_wall", MaterialKind.Solid, MapColor.SNOW, SoundType.METAL, 2.0f, false),
    RubberMatting("rubber_matting", MaterialKind.Soft, MapColor.COLOR_BLACK, SoundType.WOOL, 1.5f, false),
    EpoxyFloor("epoxy_floor", MaterialKind.Solid, MapColor.COLOR_GRAY, SoundType.POLISHED_DEEPSLATE, 2.0f, true),
    EpoxyFloorLine("epoxy_floor_line", MaterialKind.Marked, MapColor.COLOR_GRAY, SoundType.POLISHED_DEEPSLATE, 2.0f, false),
    HazardBlock("hazard_block", MaterialKind.Solid, MapColor.COLOR_YELLOW, SoundType.METAL, 3.0f, false);

    private final String Name;
    private final MaterialKind Kind;
    private final MapColor Colour;
    private final SoundType Sound;
    private final float Hardness;
    private final boolean Shapes;

    FacilityMaterial(String Name, MaterialKind Kind, MapColor Colour, SoundType Sound, float Hardness, boolean Shapes) {
        this.Name = Name;
        this.Kind = Kind;
        this.Colour = Colour;
        this.Sound = Sound;
        this.Hardness = Hardness;
        this.Shapes = Shapes;
    }

    public MaterialKind getKind() {
        return Kind;
    }

    public MapColor getMapColour() {
        return Colour;
    }

    public SoundType getSound() {
        return Sound;
    }

    public float getHardness() {
        return Hardness;
    }

    public float getBlastResistance() {
        return Kind == MaterialKind.Soft || Hardness < 2.0f ? 3.0f : 6.0f;
    }

    public boolean needsTool() {
        return Kind != MaterialKind.Soft;
    }

    public boolean hasShapes() {
        return Shapes;
    }

    public NoteBlockInstrument getInstrument() {
        if (Sound == SoundType.METAL) {
            return NoteBlockInstrument.IRON_XYLOPHONE;
        }
        return Sound == SoundType.WOOL ? NoteBlockInstrument.GUITAR : NoteBlockInstrument.BASEDRUM;
    }

    public String blockName() {
        return "facility_" + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
