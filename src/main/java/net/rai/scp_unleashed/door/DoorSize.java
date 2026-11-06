package net.rai.scp_unleashed.door;

public enum DoorSize {
    Single(1, 2),
    Room(2, 2),
    Tall(2, 3),
    Chamber(3, 3),
    Gate(4, 4),
    Bulkhead(5, 3);

    private final int Width;
    private final int Height;

    DoorSize(int Width, int Height) {
        this.Width = Width;
        this.Height = Height;
    }

    public int getWidth() {
        return Width;
    }

    public int getHeight() {
        return Height;
    }

    public int getCellCount() {
        return Width * Height;
    }

    public int risingSplit() {
        return 16 * ((Height + 1) / 2);
    }

    public int risingHeadroom() {
        return Math.max(risingSplit(), Height * 16 - risingSplit()) / 16;
    }
}
