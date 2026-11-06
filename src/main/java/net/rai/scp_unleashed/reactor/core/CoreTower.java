package net.rai.scp_unleashed.reactor.core;

import java.util.ArrayList;
import java.util.List;

public final class CoreTower {
    public final TowerType Type;
    public final int X;
    public final int Z;
    public final int Bottom;
    public final int Height;
    public final float[] RodHeat;
    public final float[] BurnLeft;
    public final float[] LevelHeat;
    public final float[] LevelWithdrawal;
    public final float[] LevelBoost;
    public final boolean[] Spent;
    public final List<Contact> RodContacts = new ArrayList<>();
    public final List<Contact> FuelContacts = new ArrayList<>();
    public final List<Contact> CoolantContacts = new ArrayList<>();
    public final List<Contact> ChannelContacts = new ArrayList<>();
    public final List<Contact> FuelNeighbours = new ArrayList<>();
    public final List<Contact> BoosterContacts = new ArrayList<>();
    public float Temperature = ReactorTuning.Ambient;
    public float Integrity = ReactorTuning.FullIntegrity;
    public float Water;
    public float Steam;
    public float Gas;
    public float Insertion = 100.0f;
    public float Target = 100.0f;
    public boolean Forced;
    public float HeatMade;
    public float HeatRemoved;
    public float HeatShared;
    public float PassiveLost;
    public float SteamMade;
    public float WaterUsed;
    public float Overflow;
    public float GasOverflow;
    public float Demand;
    public float BoostPassed;
    public boolean Dry;
    public boolean Moving;
    public boolean Failed;

    public CoreTower(TowerType Type, int X, int Z, int Bottom, int Height) {
        this.Type = Type;
        this.X = X;
        this.Z = Z;
        this.Bottom = Bottom;
        this.Height = Height;
        int Levels = Type == TowerType.Fuel ? Height : 0;
        RodHeat = new float[Levels];
        BurnLeft = new float[Levels];
        LevelHeat = new float[Levels];
        LevelWithdrawal = new float[Levels];
        LevelBoost = new float[Levels];
        Spent = new boolean[Levels];
    }

    public record Contact(CoreTower Other, int Low, int High) {
        public int overlap() {
            return High - Low + 1;
        }
    }

    public int top() {
        return Bottom + Height - 1;
    }

    public boolean covers(int Y) {
        return Y >= Bottom && Y <= top();
    }

    public float heatCapacity() {
        return Height * ReactorTuning.SegmentCapacity;
    }

    public int waterCapacity() {
        return Type.makesSteam() ? Height * ReactorTuning.WaterPerSegment : 0;
    }

    public int steamCapacity() {
        return Type.makesSteam() ? Height * ReactorTuning.SteamPerSegment : 0;
    }

    public int gasCapacity() {
        return Type == TowerType.Fuel ? Height * ReactorTuning.GasPerSegment : 0;
    }

    public void setFuel(int Tier, float Heat, float Left) {
        RodHeat[Tier] = Heat;
        BurnLeft[Tier] = Math.max(0.0f, Left);
        Spent[Tier] = false;
    }

    public int loadedLevels() {
        int Count = 0;
        for (int Tier = 0; Tier < RodHeat.length; Tier++) {
            if (RodHeat[Tier] > 0.0f && BurnLeft[Tier] > 0.0f) {
                Count++;
            }
        }
        return Count;
    }

    public float withdrawal() {
        if (LevelWithdrawal.length == 0) {
            return 0.0f;
        }
        float Sum = 0.0f;
        for (float Value : LevelWithdrawal) {
            Sum += Value;
        }
        return Sum / LevelWithdrawal.length;
    }

    public float boost() {
        float Sum = 0.0f;
        int Count = 0;
        for (int Tier = 0; Tier < LevelBoost.length; Tier++) {
            if (LevelWithdrawal[Tier] > 0.0f) {
                Sum += LevelBoost[Tier];
                Count++;
            }
        }
        return Count == 0 ? 0.0f : Sum / Count;
    }

    public float strength() {
        return Type == TowerType.Booster ? ReactorTuning.boosterStrength(Height) : 0.0f;
    }

    public float levelHeat(int Low, int High) {
        float Sum = 0.0f;
        for (int Y = Math.max(Low, Bottom); Y <= Math.min(High, top()); Y++) {
            Sum += LevelHeat[Y - Bottom];
        }
        return Sum;
    }

    public boolean isReacting() {
        return HeatMade > ReactorTuning.ReactingFloor;
    }

    public float netHeat() {
        return HeatMade - HeatRemoved - PassiveLost + HeatShared;
    }

    void resetStats() {
        HeatMade = 0.0f;
        HeatRemoved = 0.0f;
        HeatShared = 0.0f;
        PassiveLost = 0.0f;
        SteamMade = 0.0f;
        WaterUsed = 0.0f;
        Demand = 0.0f;
        BoostPassed = 0.0f;
        Dry = false;
        Moving = false;
    }

    void addSteam(float Amount) {
        Steam += Amount;
        float Room = steamCapacity();
        if (Steam > Room) {
            Overflow += Steam - Room;
            Steam = Room;
        }
    }

    void addGas(float Amount) {
        Gas += Amount;
        float Room = gasCapacity();
        if (Gas > Room) {
            GasOverflow += Gas - Room;
            Gas = Room;
        }
    }
}
