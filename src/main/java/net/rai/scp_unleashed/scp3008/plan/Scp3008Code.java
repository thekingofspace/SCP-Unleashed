package net.rai.scp_unleashed.scp3008.plan;

public final class Scp3008Code {
    public static final int Floor = 1;
    public static final int Line = 2;
    public static final int Arrow = 3;
    public static final int Hatch = 4;
    public static final int Pillar = 5;
    public static final int Ceiling = 6;
    public static final int Cable = 7;
    public static final int Light = 8;
    public static final int Sign = 9;
    public static final int Emergency = 10;
    public static final int DisplayWall = 11;
    public static final int Shelf = 12;
    public static final int RackPost = 13;
    public static final int RackBeam = 14;
    public static final int Crate = 15;
    public static final int Counter = 16;
    public static final int Menu = 17;
    public static final int FoodTable = 18;
    public static final int Tray = 19;
    public static final int Stool = 20;
    public static final int Planter = 21;
    public static final int ExitDoor = 22;
    public static final int ExitSign = 23;
    public static final int ExitWall = 24;
    public static final int Furniture = 25;
    public static final int Rug = 26;
    public static final int Rafter = 27;
    public static final int Kinds = 28;

    public static final int Down = 0;
    public static final int Up = 1;
    public static final int North = 2;
    public static final int South = 3;
    public static final int West = 4;
    public static final int East = 5;

    public static final int StyleConcrete = 0;
    public static final int StyleFoodCourt = 13;
    public static final int StyleRug = 14;
    public static final String[] RugColours = {"grey", "beige", "cream", "blue", "green", "rust", "charcoal", "yellow", "pink", "teal"};
    public static final int Styles = StyleRug + RugColours.length * 2;

    public static final int CableSpan = 0;
    public static final int CableClamp = 1;
    public static final int CableCanopy = 2;

    public static final int PartBase = 0;
    public static final int PartShaft = 1;
    public static final int PartBand = 2;

    public static final int RoleFocal = 0;
    public static final int RoleContainer = 1;
    public static final int RoleSmall = 2;
    public static final int RolePlant = 3;
    public static final int RoleWall = 4;
    public static final int RoleCeiling = 5;

    public static final int ArrowLeft = 0;
    public static final int ArrowRight = 1;
    public static final int ArrowAhead = 2;
    public static final int ArrowBack = 3;

    public static final int FoodNone = 0;
    public static final int FoodMeatballs = 1;
    public static final int FoodFries = 2;
    public static final int FoodDrink = 3;
    public static final int FoodJam = 4;

    private Scp3008Code() {
    }

    public static int of(int Kind, int Variant, int Dir) {
        return Kind << 16 | (Variant & 0x1FFF) << 3 | Dir & 7;
    }

    public static int of(int Kind, int Variant) {
        return of(Kind, Variant, 0);
    }

    public static int kind(int Code) {
        return Code >>> 16;
    }

    public static int variant(int Code) {
        return Code >>> 3 & 0x1FFF;
    }

    public static int dir(int Code) {
        return Code & 7;
    }

    public static int rugStyle(int Colour, boolean Edge) {
        return StyleRug + Math.floorMod(Colour, RugColours.length) * 2 + (Edge ? 1 : 0);
    }

    public static int slot(int Department, int Role, int Salt) {
        return (Department & 15) | (Role & 7) << 4 | (Salt & 63) << 7;
    }

    public static int slotDepartment(int Variant) {
        return Variant & 15;
    }

    public static int slotRole(int Variant) {
        return Variant >>> 4 & 7;
    }

    public static int slotSalt(int Variant) {
        return Variant >>> 7 & 63;
    }

    public static int sign(int Department, int Part, int Axis) {
        return (Department * 8 + Part) * 2 + Axis;
    }

    public static int signDepartment(int Variant) {
        return Variant / 2 / 8;
    }

    public static int signPart(int Variant) {
        return Variant / 2 % 8;
    }

    public static int signAxis(int Variant) {
        return Variant % 2;
    }

    public static int crate(boolean Loot, boolean FoodCourt) {
        return (Loot ? 1 : 0) | (FoodCourt ? 2 : 0);
    }

    public static boolean crateLoot(int Variant) {
        return (Variant & 1) != 0;
    }

    public static boolean crateFoodCourt(int Variant) {
        return (Variant & 2) != 0;
    }

    public static int rackPost(boolean AlongZ, boolean Low, boolean High, boolean Foot) {
        return (AlongZ ? 1 : 0) | (Low ? 2 : 0) | (High ? 4 : 0) | (Foot ? 8 : 0);
    }

    public static int door(boolean Upper, boolean Right) {
        return (Upper ? 1 : 0) | (Right ? 2 : 0);
    }

    public static boolean decor(int Code) {
        int Kind = kind(Code);
        return Kind == Furniture || Kind == Crate || Kind == Counter;
    }

    public static int opposite(int Dir) {
        return switch (Dir) {
            case North -> South;
            case South -> North;
            case West -> East;
            case East -> West;
            case Up -> Down;
            default -> Up;
        };
    }

    public static int clockwise(int Dir) {
        return switch (Dir) {
            case North -> East;
            case East -> South;
            case South -> West;
            case West -> North;
            default -> Dir;
        };
    }

    public static int stepX(int Dir) {
        return Dir == East ? 1 : Dir == West ? -1 : 0;
    }

    public static int stepZ(int Dir) {
        return Dir == South ? 1 : Dir == North ? -1 : 0;
    }
}
