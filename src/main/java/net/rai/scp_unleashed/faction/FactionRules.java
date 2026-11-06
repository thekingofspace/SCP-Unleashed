package net.rai.scp_unleashed.faction;

public record FactionRules(double MaxPower, double PowerPerMemberTick, double BasePowerTick, int SiteCap, int RosterCap, long ExpansionInterval,
                           long PatrolInterval, int PatrolRange, int TerritoryRange, long RaidInterval) {
    public static final int GlobalLoadedCap = 40;
    public static final int TickInterval = 20;
    public static final int BuildRetry = 40;
}
