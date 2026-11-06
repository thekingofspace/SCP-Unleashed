package net.rai.scp_unleashed.researchoffice;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;

import static net.rai.scp_unleashed.props.PropShapes.box;

public enum WingProp implements StringRepresentable {
    ResearchFumeHoodBottom("research_fume_hood_bottom", "research_fume_hood", WingMount.Floor, MapColor.CLAY, SoundType.METAL, 2.5f, 0, Shapes.block()),
    ResearchFumeHoodMiddle("research_fume_hood_middle", "research_fume_hood", WingMount.Floor, MapColor.CLAY, SoundType.METAL, 2.5f, 9,
            PropShapes.of(box(0, 0, 0, 2, 16, 16), box(14, 0, 0, 16, 16, 16), box(2, 0, 14, 14, 16, 16), box(2, 0, 0, 14, 1, 14), box(2, 12, 0, 14, 16, 14), box(2, 4, 0, 14, 12, 2))),
    ResearchFumeHoodTop("research_fume_hood_top", "research_fume_hood", WingMount.Floor, MapColor.CLAY, SoundType.METAL, 2.5f, 0,
            PropShapes.of(box(0, 0, 0, 16, 7, 16), box(2, 7, 2, 14, 9, 14), box(5, 9, 5, 11, 16, 11))),
    ResearchSpecimenFridgeBottom("research_specimen_fridge_bottom", "research_specimen_fridge", WingMount.Floor, MapColor.SNOW, SoundType.METAL, 2.0f, 5, Shapes.block()),
    ResearchSpecimenFridgeTop("research_specimen_fridge_top", "research_specimen_fridge", WingMount.Floor, MapColor.SNOW, SoundType.METAL, 2.0f, 5, Shapes.block()),
    ResearchCryoUnit("research_cryo_unit", "research_cryo_unit", WingMount.Floor, MapColor.ICE, SoundType.METAL, 2.5f, 3,
            PropShapes.of(box(1, 0, 1, 15, 11, 15), box(0, 11, 0, 16, 14, 16), box(11, 14, 10, 13, 16, 12))),
    ResearchMicroscopeStation("research_microscope_station", "research_microscope_station", WingMount.Floor, MapColor.QUARTZ, SoundType.METAL, 1.0f, 2,
            PropShapes.of(box(1, 0, 3, 15, 1, 13), box(2, 1, 5, 8, 14, 12), box(9, 1, 4, 14, 8, 11))),
    ResearchBenchCentrifuge("research_bench_centrifuge", "research_bench_centrifuge", WingMount.Floor, MapColor.QUARTZ, SoundType.METAL, 1.5f, 1,
            PropShapes.of(box(3, 0, 1, 13, 4, 13), box(3, 4, 4, 13, 9, 13))),
    ResearchEyewashStationBottom("research_eyewash_station_bottom", "research_eyewash_station", WingMount.Floor, MapColor.COLOR_GREEN, SoundType.METAL, 1.5f, 0,
            PropShapes.of(box(4, 0, 4, 12, 1, 12), box(6, 1, 6, 10, 16, 10), box(5, 1, 1, 11, 3, 7))),
    ResearchEyewashStationTop("research_eyewash_station_top", "research_eyewash_station", WingMount.Floor, MapColor.COLOR_GREEN, SoundType.METAL, 1.5f, 0,
            PropShapes.of(box(7, 0, 7, 9, 4, 9), box(2, 4, 1, 14, 9, 14))),
    ResearchAnalyserPanel("research_analyser_panel", "research_analyser_panel", WingMount.Wall, MapColor.CLAY, SoundType.METAL, 1.5f, 4,
            PropShapes.of(box(1, 1, 12, 15, 15, 16))),
    OfficeRecordsCabinetBottom("office_records_cabinet_bottom", "office_records_cabinet", WingMount.Floor, MapColor.WOOD, SoundType.WOOD, 2.0f, 0, Shapes.block()),
    OfficeRecordsCabinetMiddle("office_records_cabinet_middle", "office_records_cabinet", WingMount.Floor, MapColor.WOOD, SoundType.WOOD, 2.0f, 0, Shapes.block()),
    OfficeRecordsCabinetTop("office_records_cabinet_top", "office_records_cabinet", WingMount.Floor, MapColor.WOOD, SoundType.WOOD, 2.0f, 0, Shapes.block()),
    OfficePartitionBottom("office_partition_bottom", "office_partition", WingMount.Floor, MapColor.COLOR_GRAY, SoundType.WOOL, 1.0f, 0,
            PropShapes.of(box(0, 0, 6, 16, 16, 10), box(0, 0, 4, 1, 1, 12), box(15, 0, 4, 16, 1, 12))),
    OfficePartitionMiddle("office_partition_middle", "office_partition", WingMount.Floor, MapColor.COLOR_GRAY, SoundType.WOOL, 1.0f, 0,
            PropShapes.of(box(0, 0, 6, 16, 16, 10))),
    OfficePartitionTop("office_partition_top", "office_partition", WingMount.Floor, MapColor.COLOR_GRAY, SoundType.WOOL, 1.0f, 0,
            PropShapes.of(box(0, 0, 6, 16, 14, 10))),
    OfficePhotocopier("office_photocopier", "office_photocopier", WingMount.Floor, MapColor.TERRACOTTA_WHITE, SoundType.METAL, 2.0f, 3,
            PropShapes.of(box(1, 0, 0, 16, 14, 14))),
    OfficeShredder("office_shredder", "office_shredder", WingMount.Floor, MapColor.COLOR_BLACK, SoundType.METAL, 1.0f, 1,
            PropShapes.of(box(3, 0, 4, 13, 11, 12), box(2, 11, 3, 14, 15, 13))),
    OfficeNoticeBoard("office_notice_board", "office_notice_board", WingMount.Wall, MapColor.WOOD, SoundType.WOOD, 0.8f, 0,
            PropShapes.of(box(0, 1, 14, 16, 15, 16))),
    OfficeWallCalendar("office_wall_calendar", "office_wall_calendar", WingMount.Wall, MapColor.SNOW, SoundType.WOOL, 0.5f, 0,
            PropShapes.of(box(4, 1, 14, 12, 15, 16)));

    public static final Codec<WingProp> KindCodec = StringRepresentable.fromEnum(WingProp::values);

    private final String Name;
    private final String Family;
    private final WingMount Mount;
    private final MapColor Colour;
    private final SoundType Sound;
    private final float Hardness;
    private final int Light;
    private final VoxelShape Shape;

    WingProp(String Name, String Family, WingMount Mount, MapColor Colour, SoundType Sound, float Hardness, int Light, VoxelShape Shape) {
        this.Name = Name;
        this.Family = Family;
        this.Mount = Mount;
        this.Colour = Colour;
        this.Sound = Sound;
        this.Hardness = Hardness;
        this.Light = Light;
        this.Shape = Shape;
    }

    public String getFamily() {
        return Family;
    }

    public WingMount getMount() {
        return Mount;
    }

    public MapColor getColour() {
        return Colour;
    }

    public SoundType getSound() {
        return Sound;
    }

    public float getHardness() {
        return Hardness;
    }

    public int getLight() {
        return Light;
    }

    public VoxelShape getShape() {
        return Shape;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
