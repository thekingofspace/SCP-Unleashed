package net.rai.scp_unleashed.rocketry.platform;

public enum LaunchCheck {
    Payload("payload"),
    Crew("crew"),
    Fuel("fuel"),
    Power("power"),
    Sky("sky"),
    Route("route");

    private static final LaunchCheck[] Values = values();

    private final String Name;

    LaunchCheck(String Name) {
        this.Name = Name;
    }

    public static LaunchCheck byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Payload;
    }

    public String getName() {
        return Name;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public boolean passes(int Mask) {
        return (Mask & bit()) != 0;
    }

    public String labelKey() {
        return "gui.scp_unleashed.rocketry.check." + Name;
    }

    public String failKey() {
        return "message.scp_unleashed.rocketry.fail." + Name;
    }
}
