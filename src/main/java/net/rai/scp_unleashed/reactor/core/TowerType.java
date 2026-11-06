package net.rai.scp_unleashed.reactor.core;

public enum TowerType {
    Fuel("fuel_tower"),
    Rods("control_rod_tower"),
    Coolant("coolant_tower"),
    Channel("steam_channel"),
    Booster("booster_tower");

    private static final TowerType[] Values = values();

    private final String Name;

    TowerType(String Name) {
        this.Name = Name;
    }

    public static TowerType byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Fuel;
    }

    public String getName() {
        return Name;
    }

    public boolean makesSteam() {
        return this == Coolant || this == Channel;
    }
}
