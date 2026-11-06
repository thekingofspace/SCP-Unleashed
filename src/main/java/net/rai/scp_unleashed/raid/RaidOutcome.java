package net.rai.scp_unleashed.raid;

public enum RaidOutcome {
    None("none"),
    Defeated("defeated"),
    Retreated("retreated"),
    TimedOut("timed_out"),
    Stopped("stopped");

    private final String Name;

    RaidOutcome(String Name) {
        this.Name = Name;
    }

    public String serialName() {
        return Name;
    }

    public static RaidOutcome byName(String Text) {
        for (RaidOutcome Outcome : values()) {
            if (Outcome.Name.equals(Text)) {
                return Outcome;
            }
        }
        return None;
    }
}
