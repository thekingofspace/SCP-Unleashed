package net.rai.scp_unleashed.raid.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidLoot;

import java.util.EnumSet;

public class RaidFleeGoal extends RaidGoal {
    public static final double Escape = 3.0;
    private final double Speed;
    private int Wait;

    public RaidFleeGoal(Mob Owner, ResourceLocation Objective, double Speed) {
        super(Owner, Objective);
        this.Speed = Speed;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return raid() != null && RaidLoot.carrying(Owner);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        Wait = 0;
        Owner.setTarget(null);
    }

    @Override
    public void stop() {
        Owner.getNavigation().stop();
    }

    @Override
    public void tick() {
        Raid Current = raid();
        if (Current == null) {
            return;
        }
        BlockPos Exit = RaidDirector.exitOf(Current);
        if (Owner.distanceToSqr(Exit.getX() + 0.5, Owner.getY(), Exit.getZ() + 0.5) <= Escape * Escape) {
            RaidDirector.escape(Owner);
            return;
        }
        if (--Wait <= 0) {
            Wait = 15;
            Owner.getNavigation().moveTo(Exit.getX() + 0.5, Exit.getY(), Exit.getZ() + 0.5, Speed);
        }
    }
}
