package net.rai.scp_unleashed.discord;

public enum DiscordCoordinates {
    Default,
    On,
    Off;

    public boolean resolve(boolean Global) {
        return this == On || this == Default && Global;
    }
}
