package net.rai.scp_unleashed.door;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum DoorKind implements StringRepresentable {
    Sliding("sliding_door", DoorSize.Tall, 0, 12, DoorMotion.Split, 3, 3, 6, 10, DoorVoice.Standard, false, false),
    Room("room_door", DoorSize.Room, 0, 12, DoorMotion.Split, 3, 2, 6, 10, DoorVoice.Standard, false, false),
    Lcz("lcz_door", DoorSize.Tall, 0, 12, DoorMotion.Split, 3, 3, 6, 10, DoorVoice.Standard, false, false),
    LczRoom("lcz_room_door", DoorSize.Room, 0, 12, DoorMotion.Split, 3, 2, 6, 10, DoorVoice.Standard, false, false),
    LczSingle("lcz_single_door", DoorSize.Single, 0, 12, DoorMotion.Slide, 2, 2, 6, 10, DoorVoice.Light, false, false),
    Hcz("hcz_door", DoorSize.Tall, 0, 16, DoorMotion.Split, 4, 4, 5, 11, DoorVoice.Heavy, false, false),
    HczRoom("hcz_room_door", DoorSize.Room, 0, 16, DoorMotion.Split, 4, 2, 5, 11, DoorVoice.Heavy, false, false),
    HczSingle("hcz_single_door", DoorSize.Single, 0, 16, DoorMotion.Slide, 2, 2, 5, 11, DoorVoice.Heavy, false, false),
    Ez("ez_door", DoorSize.Tall, 0, 12, DoorMotion.Split, 2, 3, 7, 9, DoorVoice.Quiet, true, true),
    EzRoom("ez_room_door", DoorSize.Room, 0, 12, DoorMotion.Split, 2, 2, 7, 9, DoorVoice.Quiet, true, true),
    EzSingle("ez_single_door", DoorSize.Single, 0, 12, DoorMotion.Slide, 2, 2, 7, 9, DoorVoice.Quiet, true, true),
    Glass("glass_sliding_door", DoorSize.Tall, 0, 12, DoorMotion.Split, 2, 3, 7, 9, DoorVoice.Crisp, true, false),
    GlassRoom("glass_room_door", DoorSize.Room, 0, 12, DoorMotion.Split, 2, 2, 7, 9, DoorVoice.Crisp, true, false),
    GlassSingle("glass_single_door", DoorSize.Single, 0, 12, DoorMotion.Slide, 2, 2, 7, 9, DoorVoice.Crisp, true, false),
    Single("single_sliding_door", DoorSize.Single, 0, 12, DoorMotion.Slide, 2, 2, 6, 10, DoorVoice.Light, false, false),
    Containment("containment_door", DoorSize.Chamber, 1, 40, DoorMotion.Rise, 4, 4, 3, 13, DoorVoice.Chamber, false, false),
    ContainmentXl("containment_door_xl", DoorSize.Bulkhead, 2, 40, DoorMotion.Bulkhead, 6, 6, 3, 13, DoorVoice.Chamber, false, false),
    ContainmentTall("containment_tall_door", DoorSize.Tall, 0, 40, DoorMotion.Rise, 4, 4, 3, 13, DoorVoice.Chamber, false, false),
    ContainmentRoom("containment_room_door", DoorSize.Room, 0, 27, DoorMotion.Rise, 4, 2, 3, 13, DoorVoice.Chamber, false, false),
    ContainmentSingle("containment_single_door", DoorSize.Single, 0, 27, DoorMotion.Rise, 2, 2, 3, 13, DoorVoice.Chamber, false, false),
    BlastGate("blast_gate", DoorSize.Gate, 1, 55, DoorMotion.Rise, 4, 4, 3, 13, DoorVoice.Gate, false, false),
    BlastGateTall("tall_blast_gate", DoorSize.Tall, 0, 41, DoorMotion.Rise, 4, 4, 3, 13, DoorVoice.Gate, false, false),
    BlastGateRoom("room_blast_gate", DoorSize.Room, 0, 28, DoorMotion.Rise, 4, 2, 3, 13, DoorVoice.Gate, false, false),
    BlastGateSingle("single_blast_gate", DoorSize.Single, 0, 28, DoorMotion.Rise, 2, 2, 3, 13, DoorVoice.Gate, false, false);

    public static final Codec<DoorKind> KindCodec = StringRepresentable.fromEnum(DoorKind::values);

    private final String Name;
    private final DoorSize Size;
    private final int ClickedColumn;
    private final int TravelTicks;
    private final DoorMotion Motion;
    private final int Jamb;
    private final int Header;
    private final int Front;
    private final int Back;
    private final DoorVoice Voice;
    private final boolean Glazed;
    private final boolean Manual;

    DoorKind(String Name, DoorSize Size, int ClickedColumn, int TravelTicks, DoorMotion Motion, int Jamb, int Header, int Front, int Back, DoorVoice Voice, boolean Glazed, boolean Manual) {
        this.Name = Name;
        this.Size = Size;
        this.ClickedColumn = ClickedColumn;
        this.TravelTicks = TravelTicks;
        this.Motion = Motion;
        this.Jamb = Jamb;
        this.Header = Header;
        this.Front = Front;
        this.Back = Back;
        this.Voice = Voice;
        this.Glazed = Glazed;
        this.Manual = Manual;
    }

    public DoorSize getSize() {
        return Size;
    }

    public int getWidth() {
        return Size.getWidth();
    }

    public int getHeight() {
        return Size.getHeight();
    }

    public int getCellCount() {
        return Size.getCellCount();
    }

    public int getClickedColumn() {
        return ClickedColumn;
    }

    public int getTravelTicks() {
        return TravelTicks;
    }

    public DoorMotion getMotion() {
        return Motion;
    }

    public int getJamb() {
        return Jamb;
    }

    public int getHeader() {
        return Header;
    }

    public int getFront() {
        return Front;
    }

    public int getBack() {
        return Back;
    }

    public DoorVoice getVoice() {
        return Voice;
    }

    public boolean isGlazed() {
        return Glazed;
    }

    public boolean isManual() {
        return Manual;
    }

    public boolean hasWarningLamps() {
        return Motion == DoorMotion.Bulkhead;
    }

    public int partOf(int Column, int Row) {
        return Column + Row * getWidth();
    }

    public int columnOf(int Part) {
        return Part % getWidth();
    }

    public int rowOf(int Part) {
        return Part / getWidth();
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
