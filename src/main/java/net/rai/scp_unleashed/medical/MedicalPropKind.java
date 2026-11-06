package net.rai.scp_unleashed.medical;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.furnishing.FurnishingShapes;

import static net.rai.scp_unleashed.furnishing.FurnishingShapes.box;

public enum MedicalPropKind implements StringRepresentable {
    DispensaryBottom("medical_dispensary_bottom", FurnishingShapes.of(box(0, 0, 2, 16, 16, 16)), MapColor.SNOW, MedicalMaterial.Metal, 0),
    DispensaryMiddle("medical_dispensary_middle", FurnishingShapes.of(box(0, 0, 6, 16, 16, 16)), MapColor.SNOW, MedicalMaterial.Metal, 0),
    DispensaryTop("medical_dispensary_top", FurnishingShapes.of(box(0, 0, 6, 16, 16, 16)), MapColor.SNOW, MedicalMaterial.Metal, 0),
    HeadwallBottom("medical_headwall_bottom", FurnishingShapes.of(box(0, 0, 13, 16, 16, 16)), MapColor.SNOW, MedicalMaterial.Metal, 0),
    HeadwallMiddle("medical_headwall_middle", FurnishingShapes.of(box(0, 0, 12, 16, 16, 16), box(2, 6, 9, 14, 15, 12)), MapColor.SNOW, MedicalMaterial.Metal, 3),
    HeadwallTop("medical_headwall_top", FurnishingShapes.of(box(0, 0, 12, 16, 16, 16)), MapColor.SNOW, MedicalMaterial.Metal, 10),
    CurtainBottom("medical_curtain_bottom", FurnishingShapes.of(box(0, 3, 7, 16, 16, 9)), MapColor.GLOW_LICHEN, MedicalMaterial.Cloth, 0),
    CurtainMiddle("medical_curtain_middle", FurnishingShapes.of(box(0, 0, 7, 16, 16, 9)), MapColor.GLOW_LICHEN, MedicalMaterial.Cloth, 0),
    CurtainTop("medical_curtain_top", FurnishingShapes.of(box(0, 0, 6, 16, 16, 10)), MapColor.GLOW_LICHEN, MedicalMaterial.Cloth, 0),
    ScrubSinkBottom("medical_scrub_sink_bottom", FurnishingShapes.of(box(0, 0, 3, 16, 16, 16)), MapColor.METAL, MedicalMaterial.Metal, 0),
    ScrubSinkMiddle("medical_scrub_sink_middle", FurnishingShapes.of(box(0, 0, 14, 16, 16, 16), box(7, 0, 8, 9, 10, 14), box(11, 4, 12, 14, 11, 14), box(3, 1, 12, 6, 3, 14)),
            MapColor.METAL, MedicalMaterial.Metal, 0),
    ScrubSinkTop("medical_scrub_sink_top", FurnishingShapes.of(box(0, 0, 11, 16, 16, 16)), MapColor.METAL, MedicalMaterial.Metal, 0),
    SterilizerBottom("medical_sterilizer_bottom", FurnishingShapes.of(box(0, 0, 3, 16, 16, 16)), MapColor.METAL, MedicalMaterial.Metal, 0),
    SterilizerMiddle("medical_sterilizer_middle", FurnishingShapes.of(box(0, 0, 2, 16, 16, 16), box(3, 3, 0, 13, 13, 2)), MapColor.METAL, MedicalMaterial.Metal, 0),
    SterilizerTop("medical_sterilizer_top", FurnishingShapes.of(box(0, 0, 3, 16, 13, 16), box(2, 1, 2, 14, 9, 3)), MapColor.METAL, MedicalMaterial.Metal, 3),
    ExamLampTop("medical_exam_lamp_top", FurnishingShapes.of(box(5, 14, 5, 11, 16, 11), box(7, 0, 7, 9, 14, 9)), MapColor.SNOW, MedicalMaterial.Metal, 0),
    ExamLampMiddle("medical_exam_lamp_middle", FurnishingShapes.of(box(6, 0, 6, 10, 16, 10)), MapColor.SNOW, MedicalMaterial.Metal, 0),
    ExamLampBottom("medical_exam_lamp_bottom", FurnishingShapes.of(box(7, 9, 7, 9, 16, 9), box(3, 5, 3, 13, 10, 13), box(7, 3, 7, 9, 5, 9)), MapColor.SNOW, MedicalMaterial.Metal, 14),
    Lightbox("medical_lightbox", FurnishingShapes.of(box(0, 1, 12, 16, 15, 16)), MapColor.SNOW, MedicalMaterial.Metal, 8),
    DefibCabinet("medical_defib_cabinet", FurnishingShapes.of(box(3, 2, 11, 13, 14, 16)), MapColor.SNOW, MedicalMaterial.Metal, 0);

    public static final Codec<MedicalPropKind> KindCodec = StringRepresentable.fromEnum(MedicalPropKind::values);

    private final String Name;
    private final VoxelShape Shape;
    private final MapColor Colour;
    private final MedicalMaterial Material;
    private final int Light;

    MedicalPropKind(String Name, VoxelShape Shape, MapColor Colour, MedicalMaterial Material, int Light) {
        this.Name = Name;
        this.Shape = Shape;
        this.Colour = Colour;
        this.Material = Material;
        this.Light = Light;
    }

    public VoxelShape getShape() {
        return Shape;
    }

    public MapColor getMapColour() {
        return Colour;
    }

    public MedicalMaterial getMaterial() {
        return Material;
    }

    public int getLight() {
        return Light;
    }

    public boolean isLightable() {
        return Light > 0;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
