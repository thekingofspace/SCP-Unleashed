package net.rai.scp_unleashed.scp106;

import net.minecraft.util.StringRepresentable;

public enum Scp106DoorwayPart implements StringRepresentable {
    Single("single", 0),
    Lower("lower", 0),
    Middle("middle", 1),
    Upper("upper", 2);

    private final String Name;
    private final int Row;

    Scp106DoorwayPart(String Name, int Row) {
        this.Name = Name;
        this.Row = Row;
    }

    public int getRow() {
        return Row;
    }

    public boolean isTall() {
        return this != Single;
    }

    public static Scp106DoorwayPart atRow(int Row) {
        return switch (Row) {
            case 0 -> Lower;
            case 1 -> Middle;
            default -> Upper;
        };
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
