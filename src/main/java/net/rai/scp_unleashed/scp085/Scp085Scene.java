package net.rai.scp_unleashed.scp085;

import net.minecraft.util.StringRepresentable;

import java.util.List;

public enum Scp085Scene implements StringRepresentable {
    Room("room", 9, 55, new int[][]{{2, 62, 50}},
            List.of(new Spot(Scp085Pose.SitSide, 39, 43, 1), new Spot(Scp085Pose.LookOut, 15, 50, 1), new Spot(Scp085Pose.Twirl, 23, 50, 1))),
    Window("window", 9, 55, new int[][]{{2, 62, 50}},
            List.of(new Spot(Scp085Pose.LookOut, 32, 50, 1), new Spot(Scp085Pose.Crouch, 50, 50, 1))),
    Garden("garden", 9, 55, new int[][]{{2, 62, 50}},
            List.of(new Spot(Scp085Pose.SitGround, 22, 50, 1), new Spot(Scp085Pose.Crouch, 45, 50, 1))),
    Street("street", 9, 55, new int[][]{{2, 62, 50}},
            List.of(new Spot(Scp085Pose.SitFront, 37, 45, 1), new Spot(Scp085Pose.Twirl, 29, 50, 1))),
    Stairs("stairs", 9, 55, new int[][]{{2, 14, 52}, {14, 20, 48}, {20, 26, 44}, {26, 32, 40}, {32, 38, 36}, {38, 62, 32}},
            List.of(new Spot(Scp085Pose.SitSide, 29, 41, -1), new Spot(Scp085Pose.LookOut, 52, 32, 1))),
    Field("field", 9, 55, new int[][]{{2, 62, 51}},
            List.of(new Spot(Scp085Pose.SitGround, 26, 51, 1), new Spot(Scp085Pose.Crouch, 46, 51, 1), new Spot(Scp085Pose.Twirl, 36, 51, 1))),
    Home("home", 9, 55, new int[][]{{2, 62, 47}},
            List.of(new Spot(Scp085Pose.Lie, 16, 41, 1), new Spot(Scp085Pose.SitFront, 21, 42, 1), new Spot(Scp085Pose.Write, 44, 47, 1)));

    public static final int SheetLeft = 2;
    public static final int SheetRight = 62;
    public static final int SheetTop = 8;
    public static final int SheetBottom = 56;
    public static final List<Scp085Scene> Drawings = List.of(Room, Window, Garden, Street, Stairs, Field);

    private final String Name;
    private final int WalkMin;
    private final int WalkMax;
    private final int[][] Ground;
    private final List<Spot> Spots;

    Scp085Scene(String Name, int WalkMin, int WalkMax, int[][] Ground, List<Spot> Spots) {
        this.Name = Name;
        this.WalkMin = WalkMin;
        this.WalkMax = WalkMax;
        this.Ground = Ground;
        this.Spots = Spots;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    public boolean isDrawing() {
        return this != Home;
    }

    public int walkMin() {
        return WalkMin;
    }

    public int walkMax() {
        return WalkMax;
    }

    public List<Spot> spots() {
        return Spots;
    }

    public int ground(int X) {
        for (int[] Segment : Ground) {
            if (X >= Segment[0] && X < Segment[1]) {
                return Segment[2];
            }
        }
        return Ground[Ground.length - 1][2];
    }

    public String titleKey() {
        return "scene.scp_unleashed.scp085." + Name;
    }

    public static Scp085Scene byIndex(int Index) {
        Scp085Scene[] Values = values();
        return Values[Math.floorMod(Index, Values.length)];
    }

    public record Spot(Scp085Pose Pose, int X, int Y, int Facing) {
    }
}
