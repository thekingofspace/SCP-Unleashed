package net.rai.scp_unleashed.scp024.plan;

public final class Scp024Layouts {
    public static final int HalfWidth = 11;
    public static final int Back = -16;
    public static final int WallTop = 7;
    public static final int RoofY = 8;
    public static final int DoorX = 5;
    public static final int DoorRows = 4;
    public static final int FrontReach = 4;
    public static final int SignWidth = 5;
    public static final int LobbyHalf = 12;
    public static final int LobbyBack = -22;
    public static final int LobbyHeight = 11;
    public static final int LobbyDoorX = -1;

    private Scp024Layouts() {
    }

    public static Scp024Plan warehouse() {
        Scp024Plan Plan = new Scp024Plan("warehouse");
        Plan.fill(-HalfWidth, 0, Back, HalfWidth, 0, 0, Scp024Mat.Concrete);
        for (int Y = 1; Y <= WallTop; Y++) {
            Scp024Mat Course = Y == 1 ? Scp024Mat.Concrete : Scp024Mat.Siding;
            for (int X = -HalfWidth; X <= HalfWidth; X++) {
                Plan.set(X, Y, 0, Course);
                Plan.set(X, Y, Back, Course);
            }
            for (int Z = Back; Z <= 0; Z++) {
                Plan.set(-HalfWidth, Y, Z, Course);
                Plan.set(HalfWidth, Y, Z, Course);
            }
            for (int Z = Back + 1; Z < 0; Z++) {
                for (int X = -HalfWidth + 1; X < HalfWidth; X++) {
                    Plan.set(X, Y, Z, Scp024Mat.Air);
                }
            }
        }
        for (int Y = 1; Y <= RoofY + 1; Y++) {
            Plan.set(-HalfWidth, Y, 0, Scp024Mat.Trim);
            Plan.set(HalfWidth, Y, 0, Scp024Mat.Trim);
            Plan.set(-HalfWidth, Y, Back, Scp024Mat.Trim);
            Plan.set(HalfWidth, Y, Back, Scp024Mat.Trim);
        }
        Plan.fill(-HalfWidth, RoofY, Back, HalfWidth, RoofY, 0, Scp024Mat.Roof);
        for (int X = -HalfWidth; X <= HalfWidth; X++) {
            Plan.set(X, RoofY + 1, 0, Scp024Mat.Trim);
            Plan.set(X, RoofY + 1, Back, Scp024Mat.Trim);
        }
        for (int Z = Back; Z <= 0; Z++) {
            Plan.set(-HalfWidth, RoofY + 1, Z, Scp024Mat.Trim);
            Plan.set(HalfWidth, RoofY + 1, Z, Scp024Mat.Trim);
        }
        vent(Plan, -6, -9);
        vent(Plan, 4, -11);
        Plan.fill(-10, 0, 1, 1, 1, 3, Scp024Mat.Concrete);
        dockDoor(Plan, -9);
        dockDoor(Plan, -4);
        Plan.fill(3, 0, 1, 8, 0, 3, Scp024Mat.Concrete);
        for (int Y = 1; Y <= 5; Y++) {
            Plan.set(DoorX - 1, Y, 0, Scp024Mat.Trim);
            Plan.set(DoorX + 2, Y, 0, Scp024Mat.Trim);
        }
        Plan.set(DoorX, 5, 0, Scp024Mat.Trim);
        Plan.set(DoorX + 1, 5, 0, Scp024Mat.Trim);
        door(Plan, DoorX, 1, 0, Scp024Mat.StageDoor, Scp024Plan.South);
        Plan.set(DoorX - 2, 3, 1, Scp024Mat.Lamp, Scp024Plan.South);
        Plan.set(DoorX + 3, 3, 1, Scp024Mat.Lamp, Scp024Plan.South);
        for (int Part = 0; Part < SignWidth; Part++) {
            Plan.set(-2 + Part, WallTop, 1, Scp024Mat.Sign, Scp024Plan.South, Part);
        }
        Plan.mark("door", DoorX, 1, 0, Scp024Plan.South);
        Plan.mark("doorstep", DoorX, 1, 3, Scp024Plan.South);
        Plan.include(-HalfWidth - 1, -1, Back - 1);
        Plan.include(HalfWidth + 1, RoofY + 3, FrontReach);
        return Plan;
    }

    private static void vent(Scp024Plan Plan, int X, int Z) {
        Plan.fill(X, RoofY + 1, Z, X + 1, RoofY + 1, Z + 1, Scp024Mat.Trim);
        Plan.fill(X, RoofY + 2, Z, X + 1, RoofY + 2, Z + 1, Scp024Mat.Roof);
    }

    private static void dockDoor(Scp024Plan Plan, int Left) {
        for (int Y = 2; Y <= 6; Y++) {
            Plan.set(Left - 1, Y, 0, Scp024Mat.Trim);
            Plan.set(Left + 3, Y, 0, Scp024Mat.Trim);
        }
        for (int X = Left; X <= Left + 2; X++) {
            Plan.set(X, 6, 0, Scp024Mat.Trim);
            for (int Y = 2; Y <= 5; Y++) {
                Plan.set(X, Y, 0, Scp024Mat.DockDoor);
            }
        }
        Plan.set(Left, 1, 4, Scp024Mat.Bumper, Scp024Plan.South);
        Plan.set(Left + 2, 1, 4, Scp024Mat.Bumper, Scp024Plan.South);
    }

    public static void door(Scp024Plan Plan, int Left, int Bottom, int Z, Scp024Mat Kind, int Facing) {
        boolean Mirrored = Facing == Scp024Plan.North || Facing == Scp024Plan.East;
        for (int Column = 0; Column < 2; Column++) {
            for (int Row = 0; Row < DoorRows; Row++) {
                int X = Mirrored ? Left + 1 - Column : Left + Column;
                Plan.set(X, Bottom + Row, Z, Kind, Facing, Column * DoorRows + Row);
            }
        }
    }

    public static Scp024Plan lobby() {
        Scp024Plan Plan = new Scp024Plan("lobby");
        int Width = LobbyHalf * 2 + 1;
        int Depth = -LobbyBack + 1;
        Scp024Plan Room = new Scp024Plan("lobby_shell");
        Room.stage(Width, LobbyHeight, Depth, true, 0, true);
        Room.forEach((X, Y, Z, Mat, Facing, Variant) -> Plan.set(X - LobbyHalf, Y, Z + LobbyBack, Mat, Facing, Variant));
        for (int X = -8; X <= 8; X++) {
            for (int Y = 1; Y <= LobbyHeight - 1; Y++) {
                Plan.set(X, Y, LobbyBack - 1, Scp024Mat.Curtain);
            }
        }
        Plan.fill(-6, 1, LobbyBack, 6, 1, -17, Scp024Mat.StageWall);
        Plan.fill(-6, 2, LobbyBack, 6, 2, -17, Scp024Mat.StageFloor);
        for (int X = -6; X <= 6; X++) {
            Plan.set(X, 2, -16, Scp024Mat.Marquee);
            Plan.set(X, 1, -16, Scp024Mat.StageWall);
        }
        Plan.set(-6, 2, -17, Scp024Mat.Marquee);
        Plan.set(6, 2, -17, Scp024Mat.Marquee);
        for (int Step = 0; Step < 6; Step++) {
            Plan.set(-5 + Step * 2, 8, LobbyBack, Scp024Mat.BlackConcrete);
            Plan.mark("score", -5 + Step * 2, 8, LobbyBack);
        }
        for (int Row = 0; Row < 3; Row++) {
            int Z = -11 + Row * 3;
            for (int X = -10; X <= -4; X++) {
                Plan.set(X, 1, Z, Scp024Mat.Seat, Scp024Plan.South);
            }
            for (int X = 4; X <= 10; X++) {
                Plan.set(X, 1, Z, Scp024Mat.Seat, Scp024Plan.South);
            }
        }
        for (int Y = 1; Y <= 5; Y++) {
            Plan.set(LobbyDoorX - 1, Y, 1, Scp024Mat.Marquee);
            Plan.set(LobbyDoorX + 2, Y, 1, Scp024Mat.Marquee);
        }
        Plan.set(LobbyDoorX, 5, 1, Scp024Mat.Marquee);
        Plan.set(LobbyDoorX + 1, 5, 1, Scp024Mat.Marquee);
        door(Plan, LobbyDoorX, 1, 1, Scp024Mat.StageDoor, Scp024Plan.North);
        Plan.mark("door", LobbyDoorX, 1, 1, Scp024Plan.North);
        Plan.mark("spawn", 0, 1, -3, Scp024Plan.North);
        Plan.mark("host", 0, 3, -19, Scp024Plan.South);
        Plan.mark("cannon", -6, 3, -16);
        Plan.mark("cannon", 6, 3, -16);
        Plan.mark("cannon", -LobbyHalf + 1, 1, -6);
        Plan.mark("cannon", LobbyHalf - 1, 1, -6);
        Plan.mark("cannon", -5, LobbyHeight - 2, -10);
        Plan.mark("cannon", 5, LobbyHeight - 2, -10);
        Plan.mark("rig", -LobbyHalf + 1, LobbyHeight - 1, -2);
        Plan.mark("rig", LobbyHalf - 1, LobbyHeight - 1, -2);
        Plan.mark("rig", -6, LobbyHeight - 1, -15);
        Plan.mark("rig", 6, LobbyHeight - 1, -15);
        return Plan;
    }
}
