package net.rai.scp_unleashed.utility;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public enum ElbowOrientation implements StringRepresentable {
    DownNorth(Direction.DOWN, Direction.NORTH),
    DownEast(Direction.DOWN, Direction.EAST),
    DownSouth(Direction.DOWN, Direction.SOUTH),
    DownWest(Direction.DOWN, Direction.WEST),
    UpNorth(Direction.UP, Direction.NORTH),
    UpEast(Direction.UP, Direction.EAST),
    UpSouth(Direction.UP, Direction.SOUTH),
    UpWest(Direction.UP, Direction.WEST),
    NorthEast(Direction.NORTH, Direction.EAST),
    SouthEast(Direction.SOUTH, Direction.EAST),
    SouthWest(Direction.SOUTH, Direction.WEST),
    NorthWest(Direction.NORTH, Direction.WEST);

    private final Direction First;
    private final Direction Second;
    private final String Name;
    private final int TurnX;
    private final int TurnY;
    private final int PreTurnY;

    ElbowOrientation(Direction First, Direction Second) {
        this.First = First;
        this.Second = Second;
        this.Name = First.getSerializedName() + "_" + Second.getSerializedName();
        int[] Found = search(First, Second, Direction.NORTH);
        int Alternate = 0;
        if (Found == null) {
            Found = search(First, Second, Direction.EAST);
            Alternate = 90;
        }
        if (Found == null) {
            Found = new int[]{0, 0};
        }
        this.TurnX = Found[0];
        this.TurnY = Found[1];
        this.PreTurnY = Alternate;
    }

    private static int[] search(Direction First, Direction Second, Direction Outlet) {
        for (int StepX : UtilityShapes.Quarters) {
            for (int StepY : UtilityShapes.Quarters) {
                Direction Low = UtilityShapes.turn(Direction.DOWN, StepX, StepY);
                Direction Out = UtilityShapes.turn(Outlet, StepX, StepY);
                if ((Low == First && Out == Second) || (Low == Second && Out == First)) {
                    return new int[]{StepX, StepY};
                }
            }
        }
        return null;
    }

    public int getPreTurnY() {
        return PreTurnY;
    }

    public Direction getFirst() {
        return First;
    }

    public Direction getSecond() {
        return Second;
    }

    public int getTurnX() {
        return TurnX;
    }

    public int getTurnY() {
        return TurnY;
    }

    public boolean opensTo(Direction Side) {
        return First == Side || Second == Side;
    }

    public static ElbowOrientation of(Direction One, Direction Other) {
        for (ElbowOrientation Candidate : values()) {
            if ((Candidate.First == One && Candidate.Second == Other) || (Candidate.First == Other && Candidate.Second == One)) {
                return Candidate;
            }
        }
        return DownNorth;
    }

    public ElbowOrientation rotated(Rotation Turn) {
        return of(Turn.rotate(First), Turn.rotate(Second));
    }

    public ElbowOrientation mirrored(Mirror Flip) {
        return of(Flip.mirror(First), Flip.mirror(Second));
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
