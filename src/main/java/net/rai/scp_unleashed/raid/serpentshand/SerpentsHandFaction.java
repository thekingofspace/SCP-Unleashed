package net.rai.scp_unleashed.raid.serpentshand;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.rai.scp_unleashed.raid.hostile.HostileFaction;
import net.rai.scp_unleashed.raid.hostile.HostileRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import org.jetbrains.annotations.Nullable;

public final class SerpentsHandFaction extends HostileFaction {
    public static final ResourceLocation Id = HostileRegistry.location("serpents_hand");
    public static final int Colour = 0x2FB8A6;
    public static final double StartDays = 10.0;
    public static final double MinDays = 6.0;
    public static final double RampDays = 0.8;
    public static final SerpentsHandFaction Instance = new SerpentsHandFaction();

    private SerpentsHandFaction() {
        super(Id, Colour);
    }

    @Override
    public HostileRaidStyle hostileStyle() {
        return SerpentsHandRaidStyle.Instance;
    }

    @Nullable
    @Override
    public GameRules.Key<GameRules.BooleanValue> rule() {
        return HostileRegistry.SerpentRule;
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
}
