package net.rai.scp_unleashed.scp001day.plan;

public final class Scp001DayCode {
    public static final int Air = 0;
    public static final int Bedrock = 1;
    public static final int Stone = 2;
    public static final int Road = 3;
    public static final int Sidewalk = 4;
    public static final int Curb = 5;
    public static final int Plaza = 6;
    public static final int Facade = 10;
    public static final int Materials = 5;
    public static final int FloorSlab = 15;
    public static final int InteriorWall = 16;
    public static final int CoreWall = 17;
    public static final int Glass = 18;
    public static final int Boards = 19;
    public static final int Bars = 20;
    public static final int Rebar = 21;
    public static final int StairSouth = 22;
    public static final int StairNorth = 23;
    public static final int LadderEast = 24;
    public static final int LadderSouth = 25;
    public static final int ManholeCover = 26;
    public static final int SewerBrick = 27;
    public static final int SewerStained = 28;
    public static final int SewerCracked = 29;
    public static final int Water = 30;
    public static final int Rubble = 31;
    public static final int Debris = 32;
    public static final int Stain = 33;
    public static final int Residue = 34;
    public static final int DeadLamp = 35;
    public static final int FlickerLamp = 36;
    public static final int WallPanel = 37;
    public static final int WallPaperBlue = 38;
    public static final int WallPaperYellow = 39;
    public static final int Desk = 40;
    public static final int Chair = 44;
    public static final int Cabinet = 48;
    public static final int Papers = 52;
    public static final int TippedChair = 56;
    public static final int Plant = 60;
    public static final int Copier = 61;
    public static final int StainDark = 62;
    public static final int FallenTile = 63;
    public static final int CarBody = 64;
    public static final int CarColours = 5;
    public static final int CarGlass = 69;
    public static final int Wheel = 70;
    public static final int LampPole = 71;
    public static final int LampArm = 72;
    public static final int LampHead = 73;
    public static final int SignalHead = 74;
    public static final int Bench = 75;
    public static final int ShelterRoof = 79;
    public static final int ShelterPost = 80;
    public static final int ShelterGlass = 81;
    public static final int PaperDrift = 82;
    public static final int PipeX = 83;
    public static final int PipeY = 84;
    public static final int PipeZ = 85;
    public static final int Valve = 86;
    public static final int Grate = 87;
    public static final int Bedroll = 88;
    public static final int Lantern = 89;
    public static final int EmptyTin = 90;
    public static final int BusBody = 91;
    public static final int BusTrim = 92;
    public static final int EmergencyLamp = 93;
    public static final int StormDrain = 94;
    public static final int RubbleSlab = 95;
    public static final int RubbleStair = 96;
    public static final int RubbleWall = 100;
    public static final int Gravel = 101;
    public static final int Count = 102;

    private Scp001DayCode() {
    }

    public static boolean facade(int Code) {
        return Code >= Facade && Code < Facade + Materials;
    }
}
