package net.rai.scp_unleashed.decor;

import net.minecraft.util.StringRepresentable;

public enum ZonePart implements StringRepresentable {
    Floor("floor"),
    WallBottom("wall_bottom"),
    WallMiddle("wall_middle"),
    WallTop("wall_top"),
    Ceiling("ceiling"),
    Lamp("lamp"),
    FloorMarked("floor_marked"),
    FloorMarkedCorner("floor_marked_corner"),
    FloorMarkedOuter("floor_marked_outer"),
    FloorArrow("floor_arrow"),
    WallWindow("wall_window"),
    Pillar("pillar"),
    Stairs("stairs"),
    Slab("slab"),
    WallStairs("wall_stairs"),
    WallSlab("wall_slab");

    private final String Name;

    ZonePart(String Name) {
        this.Name = Name;
    }

    public boolean isFloorSurface() {
        return this == Floor || this == FloorMarked || this == FloorMarkedCorner || this == FloorMarkedOuter || this == FloorArrow || this == Stairs || this == Slab;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
