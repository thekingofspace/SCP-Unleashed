package net.rai.scp_unleashed.seasonal;

import java.util.Locale;

public enum SeasonMode {
    Auto,
    On,
    Off;

    public String getKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
