package net.rai.scp_unleashed.raid.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidLoot;
import net.rai.scp_unleashed.raid.RaidStage;

import java.util.EnumSet;

public class RaidMarchGoal extends RaidGoal {
    public static final int Repath = 20;
    public static final double Arrive = 4.0;
    private final double Speed;
    private int Wait;

    public RaidMarchGoal(Mob Owner, ResourceLocation Objective, double Speed) {
        super(Owner, Objective);
        this.Speed = Speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private BlockPos goal(Raid Current) {
        return Current.stage() == RaidStage.Retreating ? RaidDirector.exitOf(Current) : Current.target();
    }

    @Override
    public boolean canUse() {
        Raid Current = raid();
        if (Current == null || Current.stage() == RaidStage.Warning || Owner.getTarget() != null || RaidLoot.carrying(Owner)) {
            return false;
        }
        BlockPos Destination = goal(Current);
        return Owner.blockPosition().distSqr(Destination) > Arrive * Arrive;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        Wait = 0;
    }

    @Override
    public void stop() {
        Owner.getNavigation().stop();
    }

    @Override
    public void tick() {
        Raid Current = raid();
        if (Current == null || --Wait > 0) {
            return;
        }
        Wait = Repath + Owner.getRandom().nextInt(6);
        BlockPos Destination = goal(Current);
        Owner.getNavigation().moveTo(Destination.getX() + 0.5, Destination.getY(), Destination.getZ() + 0.5, Speed);
    }
}
