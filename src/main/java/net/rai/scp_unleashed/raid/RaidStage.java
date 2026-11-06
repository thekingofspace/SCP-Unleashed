package net.rai.scp_unleashed.raid;

public enum RaidStage {
    Warning("warning"),
    Active("active"),
    Retreating("retreating"),
    Ended("ended");

    private final String Name;

    RaidStage(String Name) {
        this.Name = Name;
    }

    public String serialName() {
        return Name;
    }

    public static RaidStage byName(String Text) {
        for (RaidStage Stage : values()) {
            if (Stage.Name.equals(Text)) {
                return Stage;
            }
        }
        return Ended;
    }
}
