package net.rai.scp_unleashed.decor;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public enum FacilityZone implements StringRepresentable {
    Lcz("lcz", MapColor.QUARTZ, MapColor.COLOR_LIGHT_GRAY, MapColor.SNOW, MapColor.QUARTZ, SoundType.STONE, SoundType.DEEPSLATE_TILES, SoundType.STONE),
    Hcz("hcz", MapColor.COLOR_GRAY, MapColor.METAL, MapColor.COLOR_GRAY, MapColor.COLOR_ORANGE, SoundType.METAL, SoundType.COPPER_GRATE, SoundType.METAL),
    Ez("ez", MapColor.SAND, MapColor.TERRACOTTA_WHITE, MapColor.QUARTZ, MapColor.SAND, SoundType.STONE, SoundType.POLISHED_DEEPSLATE, SoundType.STONE),
    Greenhouse("greenhouse", MapColor.SNOW, MapColor.COLOR_GRAY, MapColor.SNOW, MapColor.COLOR_MAGENTA, SoundType.METAL, SoundType.MUD_BRICKS, SoundType.METAL),
    Research("research", MapColor.CLAY, MapColor.COLOR_GRAY, MapColor.SNOW, MapColor.SNOW, SoundType.METAL, SoundType.POLISHED_DEEPSLATE, SoundType.STONE),
    Office("office", MapColor.TERRACOTTA_LIGHT_GREEN, MapColor.COLOR_GRAY, MapColor.QUARTZ, MapColor.SNOW, SoundType.CALCITE, SoundType.WOOL, SoundType.STONE),
    Medical("medical", MapColor.GLOW_LICHEN, MapColor.QUARTZ, MapColor.SNOW, MapColor.SNOW, SoundType.DEEPSLATE_TILES, SoundType.PACKED_MUD, SoundType.CALCITE),
    Power("power", MapColor.TERRACOTTA_CYAN, MapColor.METAL, MapColor.COLOR_BLACK, MapColor.COLOR_ORANGE, SoundType.METAL, SoundType.NETHERITE_BLOCK, SoundType.COPPER_GRATE),
    Containment("containment_room", MapColor.TERRACOTTA_CYAN, MapColor.STONE, MapColor.COLOR_GRAY, MapColor.SNOW, SoundType.NETHERITE_BLOCK, SoundType.STONE, SoundType.METAL);

    private final String Name;
    private final MapColor WallColour;
    private final MapColor FloorColour;
    private final MapColor CeilingColour;
    private final MapColor GlowColour;
    private final SoundType WallSound;
    private final SoundType FloorSound;
    private final SoundType CeilingSound;

    FacilityZone(String Name, MapColor WallColour, MapColor FloorColour, MapColor CeilingColour, MapColor GlowColour, SoundType WallSound, SoundType FloorSound, SoundType CeilingSound) {
        this.Name = Name;
        this.WallColour = WallColour;
        this.FloorColour = FloorColour;
        this.CeilingColour = CeilingColour;
        this.GlowColour = GlowColour;
        this.WallSound = WallSound;
        this.FloorSound = FloorSound;
        this.CeilingSound = CeilingSound;
    }

    public MapColor getMapColour(ZonePart Part) {
        if (Part.isFloorSurface()) {
            return FloorColour;
        }
        return switch (Part) {
            case Ceiling -> CeilingColour;
            case Lamp -> GlowColour;
            default -> WallColour;
        };
    }

    public SoundType getSound(ZonePart Part) {
        if (Part.isFloorSurface()) {
            return FloorSound;
        }
        return switch (Part) {
            case Ceiling -> CeilingSound;
            case Lamp -> SoundType.GLASS;
            default -> WallSound;
        };
    }

    public String blockName(ZonePart Part) {
        return Name + "_" + Part.getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
