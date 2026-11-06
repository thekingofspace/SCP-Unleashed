package net.rai.scp_unleashed.utility;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public enum AxisFacing implements StringRepresentable {
    UpX(Direction.UP, Direction.Axis.X),
    UpZ(Direction.UP, Direction.Axis.Z),
    DownX(Direction.DOWN, Direction.Axis.X),
    DownZ(Direction.DOWN, Direction.Axis.Z),
    NorthX(Direction.NORTH, Direction.Axis.X),
    NorthY(Direction.NORTH, Direction.Axis.Y),
    SouthX(Direction.SOUTH, Direction.Axis.X),
    SouthY(Direction.SOUTH, Direction.Axis.Y),
    EastZ(Direction.EAST, Direction.Axis.Z),
    EastY(Direction.EAST, Direction.Axis.Y),
    WestZ(Direction.WEST, Direction.Axis.Z),
    WestY(Direction.WEST, Direction.Axis.Y);

    private final Direction Facing;
    private final Direction.Axis Along;
    private final String Name;
    private final int TurnX;
    private final int TurnY;
    private final int PreTurnY;

    AxisFacing(Direction Facing, Direction.Axis Along) {
        this.Facing = Facing;
        this.Along = Along;
        this.Name = Facing.getSerializedName() + "_" + Along.getSerializedName();
        int[] Found = search(Facing, Along, Direction.EAST);
        int Alternate = 0;
        if (Found == null) {
            Found = search(Facing, Along, Direction.SOUTH);
            Alternate = 90;
        }
        if (Found == null) {
            Found = new int[]{0, 0};
        }
        this.TurnX = Found[0];
        this.TurnY = Found[1];
        this.PreTurnY = Alternate;
    }

    private static int[] search(Direction Facing, Direction.Axis Along, Direction Reference) {
        for (int First : UtilityShapes.Quarters) {
            for (int Second : UtilityShapes.Quarters) {
                if (UtilityShapes.turn(Direction.UP, First, Second) == Facing && UtilityShapes.turn(Reference, First, Second).getAxis() == Along) {
                    return new int[]{First, Second};
                }
            }
        }
        return null;
    }

    public int getPreTurnY() {
        return PreTurnY;
    }

    public Direction getFacing() {
        return Facing;
    }

    public Direction.Axis getAlong() {
        return Along;
    }

    public int getTurnX() {
        return TurnX;
    }

    public int getTurnY() {
        return TurnY;
    }

    public static AxisFacing of(Direction Facing, Direction.Axis Along) {
        AxisFacing Fallback = null;
        for (AxisFacing Candidate : values()) {
            if (Candidate.Facing == Facing) {
                if (Candidate.Along == Along) {
                    return Candidate;
                }
                if (Fallback == null) {
                    Fallback = Candidate;
                }
            }
        }
        return Fallback == null ? UpX : Fallback;
    }

    public AxisFacing rotated(Rotation Turn) {
        Direction.Axis Turned = Along == Direction.Axis.Y ? Along : Turn.rotate(Direction.get(Direction.AxisDirection.POSITIVE, Along)).getAxis();
        return of(Turn.rotate(Facing), Turned);
    }

    public AxisFacing mirrored(Mirror Flip) {
        return of(Flip.mirror(Facing), Along);
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
