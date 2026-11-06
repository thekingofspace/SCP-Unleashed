package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionRules;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.faction.FactionWorld;
import net.rai.scp_unleashed.faction.Factions;
import net.rai.scp_unleashed.raid.RaidOutcome;
import net.rai.scp_unleashed.raid.RaidRules;
import net.rai.scp_unleashed.raid.RaidStyle;
import net.rai.scp_unleashed.raid.RaidWorld;
import org.jetbrains.annotations.Nullable;

public abstract class HostileFaction extends FactionType {
    public static final long DayTicks = 24000L;
    public static final long AwakenTicks = 3L * DayTicks;
    public static final double DailyXp = 6.0;
    public static final double CaptureXp = 90.0;
    public static final double SpacingDays = 2.0;
    public static final double FirstShareMin = 0.5;
    public static final double FirstShareMax = 0.8;
    public static final double JitterMin = 0.75;
    public static final double JitterMax = 1.35;
    public static final String NextRaidKey = "NextRaidAt";
    public static final String LastEndKey = "LastRaidEnd";

    private boolean Ready;

    protected HostileFaction(ResourceLocation Id, int Colour) {
        super(Id, Colour, new FactionRules(HostileLevels.MaxXp, 0.0, DailyXp / DayTicks, 0, 0, 0L, 0L, 0, 0, 0L));
    }

    public abstract HostileRaidStyle hostileStyle();

    @Nullable
    public abstract GameRules.Key<GameRules.BooleanValue> rule();

    public abstract double startDays();

    public abstract double minDays();

    public abstract double rampDays();

    public double securedXp(FactionState State) {
        return CaptureXp;
    }

    @Override
    public RaidStyle raidStyle() {
        return hostileStyle();
    }

    public static double xp(FactionState State) {
        return State.power();
    }

    public static int level(FactionState State) {
        return HostileLevels.level(State.power());
    }

    public void setXp(FactionState State, double Value) {
        State.setPower(Value, rules().MaxPower());
    }

    public void addXp(FactionState State, double Value) {
        State.setPower(State.power() + Value, rules().MaxPower());
    }

    public void setLevel(FactionState State, int Level) {
        setXp(State, HostileLevels.threshold(Level));
    }

    public boolean ruleOn(MinecraftServer Server) {
        return HostileRegistry.ruleOn(Server, rule());
    }

    public double baseDays(int Level) {
        return Math.max(minDays(), startDays() - rampDays() * (Level - 1));
    }

    public long cooldown(MinecraftServer Server, int Level, RaidOutcome Outcome, RandomSource Random) {
        double Days = baseDays(Level) * (JitterMin + Random.nextDouble() * (JitterMax - JitterMin));
        Days *= RaidRules.cooldownScale(Server.overworld().getDifficulty());
        if (Outcome == RaidOutcome.Defeated) {
            Days *= RaidRules.DefeatedStretch;
        }
        return (long) (Math.max(minDays() * 0.6, Days) * DayTicks);
    }

    public void schedule(MinecraftServer Server, FactionState State, RaidOutcome Outcome, boolean Began, RandomSource Random) {
        long Now = Server.overworld().getGameTime();
        if (!Began && Outcome == RaidOutcome.Stopped) {
            State.setTimer(NextRaidKey, Now + RaidRules.RetryTicks);
            return;
        }
        State.setTimer(NextRaidKey, Now + cooldown(Server, level(State), Outcome, Random));
        State.setTimer(LastEndKey, Now);
    }

    public long nextRaid(FactionState State) {
        return State.timer(NextRaidKey);
    }

    @Override
    public void awaken(ServerLevel World, FactionState State, BlockPos Near, @Nullable Entity Cause) {
        long Now = World.getServer().overworld().getGameTime();
        double Share = FirstShareMin + World.random.nextDouble() * (FirstShareMax - FirstShareMin);
        State.setTimer(NextRaidKey, Now + (long) (startDays() * Share * DayTicks));
    }

    @Override
    public boolean canRaid(FactionState State) {
        return Ready && State.phase().active();
    }

    public boolean ready() {
        return Ready;
    }

    @Override
    public void tick(MinecraftServer Server, FactionState State) {
        long Now = Server.overworld().getGameTime();
        boolean Unlocked = HostileGate.unlocked(Server);
        if (State.phase() == FactionPhase.Dormant && Now >= AwakenTicks && Unlocked) {
            ServerLevel Overworld = Server.overworld();
            FactionControl.awaken(Overworld, this, Overworld.getSharedSpawnPos(), null);
        }
        Ready = Unlocked && State.phase().active() && ruleOn(Server) && Now >= nextRaid(State) && calm(Server, Now);
    }

    private boolean calm(MinecraftServer Server, long Now) {
        RaidWorld Data = RaidWorld.get(Server);
        long Spacing = (long) (SpacingDays * DayTicks);
        for (FactionType Other : Factions.all()) {
            if (!(Other instanceof HostileFaction Rival) || Rival == this) {
                continue;
            }
            if (Data.activeFor(Rival.id()) != null) {
                return false;
            }
            FactionState RivalState = FactionWorld.state(Server, Rival);
            long Ended = RivalState.timer(LastEndKey);
            if (Ended > 0L && Now - Ended < Spacing) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean idleMember(Mob Member) {
        return false;
    }
}
