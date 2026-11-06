package net.rai.scp_unleashed.scp902;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;

public enum Scp902Prop {
    Duckboard("scp902_duckboard", PropShapes.box(0, 0, 0, 16, 3, 16), SoundType.WOOD, MapColor.COLOR_BROWN, true, false),
    Sandbags("scp902_sandbags", PropShapes.box(0, 0, 1, 16, 15, 15), SoundType.WOOL, MapColor.TERRACOTTA_BROWN, true, false),
    Revetment("scp902_revetment", PropShapes.box(0, 0, 12, 16, 16, 16), SoundType.WOOD, MapColor.COLOR_BROWN, true, false),
    FiringStep("scp902_firing_step", PropShapes.box(0, 0, 4, 16, 7, 16), SoundType.WOOD, MapColor.COLOR_BROWN, true, false),
    BarbedWire("scp902_barbed_wire", PropShapes.box(0, 0, 2, 16, 13, 14), SoundType.CHAIN, MapColor.METAL, false, true),
    WirePicket("scp902_wire_picket", PropShapes.box(5, 0, 5, 11, 16, 11), SoundType.CHAIN, MapColor.METAL, true, false),
    Helmet("scp902_helmet", PropShapes.box(2, 0, 2, 14, 7, 14), SoundType.LANTERN, MapColor.COLOR_GREEN, false, false),
    AmmoCrate("scp902_ammo_crate", PropShapes.box(1, 0, 2, 15, 9, 14), SoundType.WOOD, MapColor.WOOD, true, false),
    CrateStack("scp902_crate_stack", PropShapes.box(1, 0, 1, 15, 15, 15), SoundType.WOOD, MapColor.WOOD, true, false),
    DugoutFrame("scp902_dugout_frame", PropShapes.of(PropShapes.box(0, 0, 12, 3, 16, 16), PropShapes.box(13, 0, 12, 16, 16, 16), PropShapes.box(0, 13, 12, 16, 16, 16)),
            SoundType.WOOD, MapColor.COLOR_BROWN, true, false),
    Stump("scp902_stump", PropShapes.box(3, 0, 3, 13, 16, 13), SoundType.WOOD, MapColor.COLOR_BLACK, true, false),
    SignBoard("scp902_sign", PropShapes.box(6, 0, 6, 10, 16, 10), SoundType.WOOD, MapColor.COLOR_BROWN, true, false);

    private final String Id;
    private final VoxelShape Shape;
    private final SoundType Sound;
    private final MapColor Colour;
    private final boolean Solid;
    private final boolean Snags;

    Scp902Prop(String Id, VoxelShape Shape, SoundType Sound, MapColor Colour, boolean Solid, boolean Snags) {
        this.Id = Id;
        this.Shape = Shape;
        this.Sound = Sound;
        this.Colour = Colour;
        this.Solid = Solid;
        this.Snags = Snags;
    }

    public String id() {
        return Id;
    }

    public VoxelShape shape() {
        return Shape;
    }

    public VoxelShape collision() {
        return Solid ? Shape : Shapes.empty();
    }

    public SoundType sound() {
        return Sound;
    }

    public MapColor colour() {
        return Colour;
    }

    public boolean snags() {
        return Snags;
    }
}
