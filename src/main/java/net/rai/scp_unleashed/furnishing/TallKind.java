package net.rai.scp_unleashed.furnishing;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.rai.scp_unleashed.furnishing.FurnishingShapes.box;

public enum TallKind implements StringRepresentable {
    CoatRack("coat_rack", FurnishingShapes.of(box(2, 0, 2, 14, 2, 14), box(7, 2, 7, 9, 16, 9), box(3, 8, 3, 13, 16, 6)),
            FurnishingShapes.of(box(7, 0, 7, 9, 13, 9), box(3, 0, 3, 13, 10, 6), box(4, 10, 4, 12, 12, 12), box(6, 13, 6, 10, 15, 10))),
    IvStand("iv_stand", FurnishingShapes.of(box(2, 0, 2, 14, 2, 14), box(7, 2, 7, 9, 16, 9), box(4, 9, 3, 8, 13, 8)),
            FurnishingShapes.of(box(7, 0, 7, 9, 12, 9), box(4, 12, 7, 12, 14, 9), box(3, 2, 7, 7, 12, 9))),
    PrivacyScreen("privacy_screen", FurnishingShapes.of(box(0, 0, 6, 16, 1, 10), box(0, 1, 7, 16, 16, 9)),
            FurnishingShapes.of(box(0, 0, 7, 16, 15, 9))),
    Whiteboard("whiteboard", FurnishingShapes.of(box(1, 0, 3, 15, 2, 13), box(1, 2, 7, 15, 16, 9), box(2, 8, 5, 14, 10, 9)),
            FurnishingShapes.of(box(1, 0, 7, 15, 16, 9)));

    public static final Codec<TallKind> KindCodec = StringRepresentable.fromEnum(TallKind::values);

    private final String Name;
    private final VoxelShape Lower;
    private final VoxelShape Upper;

    TallKind(String Name, VoxelShape Lower, VoxelShape Upper) {
        this.Name = Name;
        this.Lower = Lower;
        this.Upper = Upper;
    }

    public VoxelShape getLower() {
        return Lower;
    }

    public VoxelShape getUpper() {
        return Upper;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
