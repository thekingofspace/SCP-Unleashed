package net.rai.scp_unleashed.discord;

public enum DiscordContainment {
    Off,
    Breaches,
    All;

    public boolean allows(boolean Breach) {
        return this == All || this == Breaches && Breach;
    }
}
