package net.rai.scp_unleashed.discord;

public enum DiscordWarhead {
    Off,
    Countdown,
    All;

    public boolean allows(boolean Lever) {
        return this == All || this == Countdown && !Lever;
    }
}
