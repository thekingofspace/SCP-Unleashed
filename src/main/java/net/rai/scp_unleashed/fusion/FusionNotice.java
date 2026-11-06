package net.rai.scp_unleashed.fusion;

public enum FusionNotice {
    None("none"),
    Stopped("stopped"),
    NoFuel("no_fuel"),
    NoWater("no_water"),
    Aborted("aborted");

    private static final FusionNotice[] Values = values();

    private final String Name;

    FusionNotice(String Name) {
        this.Name = Name;
    }

    public static FusionNotice byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : None;
    }

    public String getSerializedName() {
        return Name;
    }

    public String getTranslationKey() {
        return "fusion.scp_unleashed.notice." + Name;
    }
}
