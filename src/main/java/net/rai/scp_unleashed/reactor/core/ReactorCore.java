package net.rai.scp_unleashed.reactor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReactorCore {
    private final List<CoreTower> Towers;
    private final List<CoreTower> Fuel = new ArrayList<>();
    private final List<CoreTower> Rods = new ArrayList<>();
    private final List<CoreTower> Coolant = new ArrayList<>();
    private final List<CoreTower> Channels = new ArrayList<>();
    private final List<CoreTower> Boosters = new ArrayList<>();
    private final List<Pair> Pairs = new ArrayList<>();
    private boolean Scram;

    public ReactorCore(List<CoreTower> Towers) {
        this.Towers = Collections.unmodifiableList(new ArrayList<>(Towers));
        Map<Long, List<CoreTower>> Columns = new HashMap<>();
        for (CoreTower Tower : this.Towers) {
            Columns.computeIfAbsent(column(Tower.X, Tower.Z), Key -> new ArrayList<>()).add(Tower);
            switch (Tower.Type) {
                case Fuel -> Fuel.add(Tower);
                case Rods -> Rods.add(Tower);
                case Coolant -> Coolant.add(Tower);
                case Channel -> Channels.add(Tower);
                case Booster -> Boosters.add(Tower);
            }
        }
        for (CoreTower Rod : Rods) {
            for (int[] Step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                for (CoreTower Other : Columns.getOrDefault(column(Rod.X + Step[0], Rod.Z + Step[1]), List.of())) {
                    int Low = Math.max(Rod.Bottom, Other.Bottom);
                    int High = Math.min(Rod.top(), Other.top());
                    if (Other.Type == TowerType.Booster && Low <= High) {
                        Rod.BoosterContacts.add(new CoreTower.Contact(Other, Low, High));
                        Other.RodContacts.add(new CoreTower.Contact(Rod, Low, High));
                    }
                }
            }
        }
        for (CoreTower Tower : Fuel) {
            for (int[] Step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                for (CoreTower Other : Columns.getOrDefault(column(Tower.X + Step[0], Tower.Z + Step[1]), List.of())) {
                    int Low = Math.max(Tower.Bottom, Other.Bottom);
                    int High = Math.min(Tower.top(), Other.top());
                    if (Low > High) {
                        continue;
                    }
                    switch (Other.Type) {
                        case Fuel -> {
                            Tower.FuelNeighbours.add(new CoreTower.Contact(Other, Low, High));
                            if (Fuel.indexOf(Tower) < Fuel.indexOf(Other)) {
                                Pairs.add(new Pair(Tower, Other, High - Low + 1));
                            }
                        }
                        case Rods -> {
                            Tower.RodContacts.add(new CoreTower.Contact(Other, Low, High));
                            Other.FuelContacts.add(new CoreTower.Contact(Tower, Low, High));
                        }
                        case Coolant -> {
                            Tower.CoolantContacts.add(new CoreTower.Contact(Other, Low, High));
                            Other.FuelContacts.add(new CoreTower.Contact(Tower, Low, High));
                        }
                        case Channel -> {
                            Tower.ChannelContacts.add(new CoreTower.Contact(Other, Low, High));
                            Other.FuelContacts.add(new CoreTower.Contact(Tower, Low, High));
                        }
                        default -> {
                        }
                    }
                }
            }
        }
    }

    private static long column(int X, int Z) {
        return ((long) X << 32) ^ (Z & 0xFFFFFFFFL);
    }

    public List<CoreTower> getTowers() {
        return Towers;
    }

    public void setScram(boolean Value) {
        Scram = Value;
    }

    public boolean isScram() {
        return Scram;
    }

    public void step() {
        for (CoreTower Tower : Towers) {
            Tower.resetStats();
        }
        moveRods();
        generate();
        cool();
        boil();
        conduct();
        settle();
    }

    private void moveRods() {
        for (CoreTower Tower : Rods) {
            boolean Dropping = Scram || Tower.Forced;
            float Goal = Dropping ? 100.0f : Math.max(0.0f, Math.min(100.0f, Tower.Target));
            float Speed = Dropping ? ReactorTuning.ScramSpeed : ReactorTuning.RodSpeed;
            float Before = Tower.Insertion;
            if (Tower.Insertion < Goal) {
                Tower.Insertion = Math.min(Goal, Tower.Insertion + Speed);
            } else if (Tower.Insertion > Goal) {
                Tower.Insertion = Math.max(Goal, Tower.Insertion - Speed);
            }
            Tower.Moving = Tower.Insertion != Before;
        }
    }

    private void generate() {
        for (CoreTower Tower : Fuel) {
            for (int Tier = 0; Tier < Tower.Height; Tier++) {
                int Y = Tower.Bottom + Tier;
                float Sum = 0.0f;
                float Boost = 0.0f;
                int Count = 0;
                for (CoreTower.Contact Contact : Tower.RodContacts) {
                    if (Y < Contact.Low() || Y > Contact.High()) {
                        continue;
                    }
                    CoreTower Rod = Contact.Other();
                    float Open = 1.0f - Rod.Insertion / 100.0f;
                    Sum += Open;
                    Count++;
                    for (CoreTower.Contact Link : Rod.BoosterContacts) {
                        if (Y >= Link.Low() && Y <= Link.High()) {
                            Boost += Link.Other().strength() * Open;
                        }
                    }
                }
                float Withdrawal = Count == 0 ? 0.0f : Math.max(0.0f, Math.min(1.0f, Sum / Count));
                Boost = Math.max(0.0f, Math.min(ReactorTuning.MaxBoost, Boost));
                Tower.LevelWithdrawal[Tier] = Withdrawal;
                Tower.LevelBoost[Tier] = Withdrawal > 0.0f ? Boost : 0.0f;
                float Heat = 0.0f;
                if (Withdrawal > 0.0f && Tower.RodHeat[Tier] > 0.0f && Tower.BurnLeft[Tier] > 0.0f) {
                    float Burn = Math.min(Withdrawal * (1.0f + Boost), Tower.BurnLeft[Tier]);
                    Tower.BurnLeft[Tier] -= Burn;
                    Heat = Tower.RodHeat[Tier] * Burn;
                    if (Tower.BurnLeft[Tier] <= 0.0f) {
                        Tower.BurnLeft[Tier] = 0.0f;
                        Tower.Spent[Tier] = true;
                    }
                    if (Boost > 0.0f) {
                        share(Tower, Y, Boost);
                    }
                }
                Tower.LevelHeat[Tier] = Heat;
                Tower.HeatMade += Heat;
            }
            Tower.addGas(Tower.HeatMade * ReactorTuning.GasPerHeat);
        }
    }

    private static void share(CoreTower Tower, int Y, float Boost) {
        for (CoreTower.Contact Contact : Tower.RodContacts) {
            if (Y < Contact.Low() || Y > Contact.High()) {
                continue;
            }
            CoreTower Rod = Contact.Other();
            float Open = 1.0f - Rod.Insertion / 100.0f;
            for (CoreTower.Contact Link : Rod.BoosterContacts) {
                if (Y >= Link.Low() && Y <= Link.High()) {
                    float Passed = Link.Other().strength() * Open;
                    Rod.BoostPassed += Passed;
                    Link.Other().BoostPassed += Passed;
                }
            }
        }
    }

    private void cool() {
        for (CoreTower Tower : Coolant) {
            float Want = 0.0f;
            for (CoreTower.Contact Contact : Tower.FuelContacts) {
                Want += coolantDemand(Contact);
            }
            Tower.Demand = Want;
            if (Want <= 0.0f) {
                continue;
            }
            float Factor = Math.min(1.0f, Math.min(ReactorTuning.CoolantCap * Tower.Height / Want, Tower.Water / (Want * ReactorTuning.WaterPerSteam)));
            Tower.Dry = Tower.Water < Want * ReactorTuning.WaterPerSteam;
            if (Factor <= 0.0f) {
                continue;
            }
            for (CoreTower.Contact Contact : Tower.FuelContacts) {
                Contact.Other().HeatRemoved += coolantDemand(Contact) * Factor;
            }
            float Made = Want * Factor;
            Tower.Water = Math.max(0.0f, Tower.Water - Made * ReactorTuning.WaterPerSteam);
            Tower.WaterUsed += Made * ReactorTuning.WaterPerSteam;
            Tower.SteamMade += Made;
            Tower.addSteam(Made);
        }
    }

    private static float coolantDemand(CoreTower.Contact Contact) {
        return ReactorTuning.CoolantRate * Contact.overlap() * Math.max(0.0f, Contact.Other().Temperature - ReactorTuning.Boil);
    }

    private void boil() {
        for (CoreTower Tower : Channels) {
            float Want = 0.0f;
            for (CoreTower.Contact Contact : Tower.FuelContacts) {
                Want += channelDemand(Contact);
            }
            Tower.Demand = Want;
            if (Want <= 0.0f) {
                continue;
            }
            float Factor = Math.min(1.0f, Math.min(ReactorTuning.ChannelCap * Tower.Height / Want, Tower.Water / (Want * ReactorTuning.WaterPerSteam)));
            Tower.Dry = Tower.Water < Want * ReactorTuning.WaterPerSteam;
            if (Factor <= 0.0f) {
                continue;
            }
            float Made = Want * Factor;
            Tower.Water = Math.max(0.0f, Tower.Water - Made * ReactorTuning.WaterPerSteam);
            Tower.WaterUsed += Made * ReactorTuning.WaterPerSteam;
            Tower.SteamMade += Made;
            Tower.addSteam(Made);
        }
    }

    private static float channelDemand(CoreTower.Contact Contact) {
        CoreTower Source = Contact.Other();
        return ReactorTuning.ChannelYield * Source.levelHeat(Contact.Low(), Contact.High()) * ReactorTuning.channelEfficiency(Source.Temperature);
    }

    private void conduct() {
        for (Pair Link : Pairs) {
            float Flow = ReactorTuning.Conduction * Link.Overlap() * (Link.First().Temperature - Link.Second().Temperature);
            Link.First().HeatShared -= Flow;
            Link.Second().HeatShared += Flow;
        }
    }

    private record Pair(CoreTower First, CoreTower Second, int Overlap) {
    }

    private void settle() {
        for (CoreTower Tower : Fuel) {
            Tower.PassiveLost = ReactorTuning.PassiveLoss * Tower.Height * Math.max(0.0f, Tower.Temperature - ReactorTuning.Ambient);
            Tower.Temperature = Math.max(ReactorTuning.Ambient, Tower.Temperature + Tower.netHeat() / Tower.heatCapacity());
            if (Tower.Temperature >= ReactorTuning.CriticalLevel) {
                float Damage = ReactorTuning.damagePerTick(Tower.Temperature) * (Tower.isReacting() ? 1.0f : ReactorTuning.ResidualDamage);
                Tower.Integrity = Math.max(0.0f, Tower.Integrity - Damage);
            } else if (Tower.Temperature < ReactorTuning.HotLevel) {
                Tower.Integrity = Math.min(ReactorTuning.FullIntegrity, Tower.Integrity + ReactorTuning.RepairRate / ReactorTuning.TicksPerSecond);
            }
            if (Tower.Integrity <= 0.0f) {
                Tower.Failed = true;
            }
        }
    }

    public void shock(CoreTower Failed) {
        for (CoreTower.Contact Contact : Failed.FuelNeighbours) {
            Contact.Other().Temperature += ReactorTuning.HeatShock * Contact.overlap() / (float) Contact.Other().Height;
        }
    }

    public float totalSteam() {
        float Sum = 0.0f;
        for (CoreTower Tower : Coolant) {
            Sum += Tower.SteamMade;
        }
        for (CoreTower Tower : Channels) {
            Sum += Tower.SteamMade;
        }
        return Sum;
    }

    public float totalHeat() {
        float Sum = 0.0f;
        for (CoreTower Tower : Fuel) {
            Sum += Tower.HeatMade;
        }
        return Sum;
    }

    public float hottest() {
        float Hottest = ReactorTuning.Ambient;
        for (CoreTower Tower : Fuel) {
            Hottest = Math.max(Hottest, Tower.Temperature);
        }
        return Hottest;
    }

    public float weakest() {
        float Weakest = ReactorTuning.FullIntegrity;
        for (CoreTower Tower : Fuel) {
            Weakest = Math.min(Weakest, Tower.Integrity);
        }
        return Weakest;
    }

    public List<CoreTower> getFuel() {
        return Fuel;
    }

    public List<CoreTower> getRods() {
        return Rods;
    }

    public List<CoreTower> getCoolant() {
        return Coolant;
    }

    public List<CoreTower> getChannels() {
        return Channels;
    }

    public List<CoreTower> getBoosters() {
        return Boosters;
    }
}
