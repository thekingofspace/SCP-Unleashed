package net.rai.scp_unleashed.security;

public enum TurretCategory {
    Hostile("hostile", true),
    Scp("scp", true),
    DClass("dclass", false),
    Personnel("personnel", false),
    Animals("animals", false),
    Players("players", false);

    public static final int DefaultMask = defaults();

    private final String Name;
    private final boolean Default;

    TurretCategory(String Name, boolean Default) {
        this.Name = Name;
        this.Default = Default;
    }

    public String getName() {
        return Name;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.sentry_turret.category." + Name;
    }

    public String getHintKey() {
        return "gui.scp_unleashed.sentry_turret.category." + Name + ".hint";
    }

    public static TurretCategory byIndex(int Index) {
        TurretCategory[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }

    private static int defaults() {
        int Mask = 0;
        for (TurretCategory Category : values()) {
            if (Category.Default) {
                Mask |= Category.bit();
            }
        }
        return Mask;
    }
}
