package net.rai.scp_unleashed.furnishing;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.rai.scp_unleashed.furnishing.FurnishingShapes.box;

public enum FurnishingKind implements StringRepresentable {
    ChemicalShelf("chemical_shelf", FurnishingShapes.of(box(1, 0, 8, 15, 16, 16)), 0),
    SpecimenShelf("specimen_shelf", FurnishingShapes.of(box(1, 0, 8, 15, 16, 16)), 0),
    BinderShelf("binder_shelf", FurnishingShapes.of(box(1, 0, 8, 15, 16, 16)), 0),
    SteelShelf("steel_shelf", FurnishingShapes.of(box(1, 0, 8, 15, 16, 16)), 0),
    MedicalSupplyShelf("medical_supply_shelf", FurnishingShapes.of(box(1, 0, 8, 15, 16, 16)), 0),
    PaperStack("paper_stack", FurnishingShapes.of(box(4, 0, 3, 13, 2, 13)), 0),
    ArchiveBoxes("archive_boxes", FurnishingShapes.of(box(1, 0, 2, 15, 6, 14), box(3, 6, 3, 12, 11, 13)), 0),
    TrashCan("trash_can", FurnishingShapes.of(box(4, 0, 4, 12, 9, 12)), 0),
    PottedPlant("potted_plant", FurnishingShapes.of(box(4, 0, 4, 12, 8, 12), box(3, 8, 3, 13, 16, 13)), 0),
    MopBucket("mop_bucket", FurnishingShapes.of(box(3, 0, 4, 13, 8, 13), box(4, 8, 10, 12, 11, 13), box(10, 11, 11, 12, 16, 12)), 0),
    MedicalCart("medical_cart", FurnishingShapes.of(box(2, 0, 3, 14, 13, 13), box(1, 13, 2, 15, 14, 14), box(3, 14, 4, 13, 16, 9), box(0, 2, 5, 2, 13, 7),
            box(14, 10, 5, 16, 11, 11)), 0),
    CafeteriaChair("cafeteria_chair", FurnishingShapes.of(box(2, 0, 2, 14, 8, 14), box(2, 8, 12, 14, 16, 13)), 8),
    LabStool("lab_stool", FurnishingShapes.of(box(2, 0, 2, 14, 2, 14), box(7, 2, 7, 9, 10, 9), box(4, 10, 4, 12, 12, 12)), 12),
    Wheelchair("wheelchair", FurnishingShapes.of(box(0, 0, 2, 16, 8, 16), box(2, 8, 12, 14, 16, 16)), 8),
    DeskLamp("desk_lamp", FurnishingShapes.of(box(4, 0, 6, 12, 2, 11), box(7, 2, 8, 9, 12, 9), box(3, 9, 4, 13, 13, 9)), 0),
    CoffeeMachine("coffee_machine", FurnishingShapes.of(box(4, 0, 3, 12, 13, 12), box(12, 0, 3, 16, 3, 6)), 0),
    Microwave("microwave", FurnishingShapes.of(box(1, 0, 2, 15, 9, 14)), 0),
    VitalsMonitor("vitals_monitor", FurnishingShapes.of(box(3, 0, 3, 13, 2, 13), box(7, 2, 7, 9, 8, 9), box(3, 8, 5, 13, 16, 10)), 0);

    public static final Codec<FurnishingKind> KindCodec = StringRepresentable.fromEnum(FurnishingKind::values);

    private final String Name;
    private final VoxelShape Shape;
    private final double SeatHeight;

    FurnishingKind(String Name, VoxelShape Shape, double SeatHeight) {
        this.Name = Name;
        this.Shape = Shape;
        this.SeatHeight = SeatHeight;
    }

    public VoxelShape getShape() {
        return Shape;
    }

    public double getSeatHeight() {
        return SeatHeight / 16.0;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
