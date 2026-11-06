package net.rai.scp_unleashed.grenade;

public enum GrenadeKind {
    Frag("frag_grenade"),
    Incendiary("incendiary_grenade"),
    Flash("flash_grenade"),
    Gas("gas_grenade");

    private final String Name;

    GrenadeKind(String Name) {
        this.Name = Name;
    }

    public String getName() {
        return Name;
    }

    public static GrenadeKind byIndex(int Index) {
        GrenadeKind[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }
}
