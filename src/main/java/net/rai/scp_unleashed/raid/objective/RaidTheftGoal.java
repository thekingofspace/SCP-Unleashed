package net.rai.scp_unleashed.raid.objective;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidLoot;
import net.rai.scp_unleashed.raid.base.BaseHeatmap;
import net.rai.scp_unleashed.raid.base.RaidBases;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class RaidTheftGoal extends RaidGoal {
    public static final int SearchEvery = 40;
    public static final int GiveUp = 600;
    public static final int RouteChecks = 3;
    public static final int StallLimit = 60;
    public static final long ShunTicks = 200L;
    private final Predicate<ItemStack> Wanted;
    private final int Radius;
    private final double Speed;
    private final Map<Long, Long> Shunned = new HashMap<>();
    @Nullable
    private BlockPos Stash;
    private int Cooldown;
    private int Time;
    private int Wait;
    private int Stall;

    public RaidTheftGoal(Mob Owner, ResourceLocation Objective, Predicate<ItemStack> Wanted, int Radius, double Speed) {
        super(Owner, Objective);
        this.Wanted = Wanted;
        this.Radius = Math.max(4, Radius);
        this.Speed = Speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private List<BlockPos> candidates(ServerLevel World, Raid Current) {
        List<BlockPos> Found = new ArrayList<>();
        BlockPos Centre = Owner.blockPosition();
        double Square = (double) Radius * Radius;
        BaseHeatmap Known = RaidBases.get(World.getServer()).find(Current.group(), Current.dimension());
        if (Known != null) {
            for (Map.Entry<Long, Long> Entry : Known.stashes().entrySet()) {
                BlockPos Pos = BlockPos.of(Entry.getKey());
                if (Pos.distSqr(Centre) <= Square) {
                    Found.add(Pos);
                }
            }
        }
        for (BlockPos Pos : RaidStash.nearby(World, Centre, Radius)) {
            if (!Found.contains(Pos)) {
                Found.add(Pos);
            }
        }
        return Found;
    }

    private boolean shunned(long Key, long Now) {
        Long Until = Shunned.get(Key);
        if (Until == null) {
            return false;
        }
        if (Until <= Now) {
            Shunned.remove(Key);
            return false;
        }
        return true;
    }

    private void shun(long Key) {
        Shunned.put(Key, Owner.level().getGameTime() + ShunTicks);
    }

    @Override
    public boolean canUse() {
        ServerLevel World = world();
        Raid Current = activeRaid();
        if (World == null || Current == null || Owner.getTarget() != null || RaidLoot.carrying(Owner)) {
            return false;
        }
        if (--Cooldown > 0) {
            return false;
        }
        Cooldown = SearchEvery / 2 + Owner.getRandom().nextInt(SearchEvery / 2);
        long Now = World.getGameTime();
        List<BlockPos> Choices = new ArrayList<>();
        for (BlockPos Pos : candidates(World, Current)) {
            if (!Current.claimed(Pos.asLong()) && !shunned(Pos.asLong(), Now) && RaidStash.holds(World, Pos, Wanted)) {
                Choices.add(Pos);
            }
        }
        Choices.sort(Comparator.comparingDouble(Pos -> Pos.distSqr(Owner.blockPosition())));
        int Checked = 0;
        for (BlockPos Pos : Choices) {
            if (Checked++ >= RouteChecks) {
                break;
            }
            if (!RaidAccess.touch(Owner, Pos) && RaidAccess.worthChecking(Owner, Pos) && !RaidAccess.reachable(Owner, Pos)) {
                shun(Pos.asLong());
                continue;
            }
            if (Current.claim(Pos.asLong())) {
                Stash = Pos;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return Stash != null && Time < GiveUp && activeRaid() != null && !RaidLoot.carrying(Owner);
    }

    @Override
    public void start() {
        Time = 0;
        Wait = 0;
        Stall = 0;
    }

    @Override
    public void stop() {
        Raid Current = raid();
        if (Current != null && Stash != null) {
            Current.unclaim(Stash.asLong());
            if (Time >= GiveUp) {
                shun(Stash.asLong());
            }
        }
        Stash = null;
        Owner.getNavigation().stop();
    }

    @Override
    public void tick() {
        ServerLevel World = world();
        Raid Current = raid();
        if (World == null || Current == null || Stash == null) {
            return;
        }
        Time++;
        Owner.getLookControl().setLookAt(Stash.getX() + 0.5, Stash.getY() + 0.5, Stash.getZ() + 0.5);
        if (!RaidAccess.touch(Owner, Stash)) {
            if (--Wait <= 0) {
                Wait = 15;
                Owner.getNavigation().moveTo(Stash.getX() + 0.5, Stash.getY(), Stash.getZ() + 0.5, Speed);
            }
            if (Owner.getNavigation().isDone() && ++Stall > StallLimit) {
                shun(Stash.asLong());
                Time = GiveUp;
            }
            return;
        }
        Stall = 0;
        ItemStack Taken = RaidStash.takeBy(Owner, Stash, Wanted);
        if (Taken.isEmpty()) {
            Time = GiveUp;
            return;
        }
        Owner.swing(InteractionHand.MAIN_HAND);
        RaidLoot.carry(Owner, Taken);
        RaidDirector.stolen(World, Current, Owner, Taken);
        if (!RaidStash.holds(World, Stash, Wanted)) {
            RaidBases.get(World.getServer()).forgetStash(Current.group(), Current.dimension(), Stash);
        }
    }
}
