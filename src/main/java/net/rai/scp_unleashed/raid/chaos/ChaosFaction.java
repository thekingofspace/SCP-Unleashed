package net.rai.scp_unleashed.raid.chaos;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.raid.hostile.HostileFaction;
import net.rai.scp_unleashed.raid.hostile.HostileLevels;
import net.rai.scp_unleashed.raid.hostile.HostileRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import org.jetbrains.annotations.Nullable;

public final class ChaosFaction extends HostileFaction {
    public static final ResourceLocation Id = HostileRegistry.location("chaos_insurgency");
    public static final int Colour = 0x6B7A3A;
    public static final double StartDays = 9.0;
    public static final double MinDays = 5.5;
    public static final double RampDays = 0.7;
    public static final ChaosFaction Instance = new ChaosFaction();

    private ChaosFaction() {
        super(Id, Colour);
    }

    @Override
    public HostileRaidStyle hostileStyle() {
        return ChaosRaidStyle.Instance;
    }

    @Nullable
    @Override
    public GameRules.Key<GameRules.BooleanValue> rule() {
        return HostileRegistry.ChaosRule;
    }

    @Override
    public double startDays() {
        return StartDays;
    }

    @Override
    public double minDays() {
        return MinDays;
    }

    @Override
    public double rampDays() {
        return RampDays;
    }

    @Override
    public double securedXp(FactionState State) {
        double Needed = HostileLevels.toNext(xp(State));
        return Needed > 0.0 ? Math.max(CaptureXp, Needed) : CaptureXp;
    }
}
