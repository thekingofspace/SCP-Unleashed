package net.rai.scp_unleashed.raid.chaos;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.base.BaseHeatmap;
import net.rai.scp_unleashed.raid.base.RaidBases;
import net.rai.scp_unleashed.raid.hostile.HostileHauls;
import net.rai.scp_unleashed.raid.hostile.HostileRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.hostile.HostileTargets;
import net.rai.scp_unleashed.raid.hostile.HostileTrophy;
import net.rai.scp_unleashed.raid.objective.RaidAccess;
import net.rai.scp_unleashed.raid.objective.RaidStash;
import net.rai.scp_unleashed.raid.underground.RaidBreach;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class ChaosSecureGoal extends Goal {
    public static final int SearchEvery = 40;
    public static final double SearchMargin = 24.0;
    public static final int OwnReach = 32;
    public static final int RouteChecks = 3;
    public static final double Comfort = 2.2;
    public static final int StallLimit = 60;
    public static final long ShunTicks = 200L;
    public static final int LockerTicks = 60;
    public static final int ChestTicks = 20;
    public static final int StrikeEvery = 15;
    public static final int GiveUp = 600;
    public static final double Speed = 1.1;
    public static final double LockerWeight = 0.0;
    public static final double ChestWeight = 16.0;
    public static final double LooseWeight = 24.0;
    public static final Predicate<ItemStack> Wanted = HostileTargets::isObjectAnomaly;
    private final ChaosInsurgent Owner;
    private final Set<Long> Refused = new HashSet<>();
    private final Map<Long, Long> Shunned = new HashMap<>();
    @Nullable
    private BlockPos Stash;
    @Nullable
    private Entity Loose;
    private int Cooldown;
    private int Time;
    private int Work;
    private int Repath;
    private int Stall;

    public ChaosSecureGoal(ChaosInsurgent Owner) {
        this.Owner = Owner;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static long looseKey(Entity Body) {
        return Body.getUUID().getMostSignificantBits() ^ Body.getUUID().getLeastSignificantBits() ^ 0xC4A05L;
    }

    private boolean ready() {
        Raid Current = Owner.raid();
        return Current != null && Current.stage() == RaidStage.Active && Owner.seeker() && Owner.active() && !Owner.hauling()
                && !(Owner.recentlyHurt() && Owner.getTarget() != null) && !RaidBreach.held(Owner);
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

    public record Pick(@Nullable BlockPos Pos, @Nullable Entity Body, double Score) {
    }

    public static List<BlockPos> stashes(ServerLevel World, Raid Current, BlockPos Near, int Radius) {
        List<BlockPos> Found = new ArrayList<>();
        BaseHeatmap Known = RaidBases.get(World.getServer()).find(Current.group(), Current.dimension());
        if (Known != null) {
            for (Map.Entry<Long, Long> Entry : Known.stashes().entrySet()) {
                BlockPos Pos = BlockPos.of(Entry.getKey());
                if (Pos.distSqr(Near) <= (double) Radius * Radius) {
                    Found.add(Pos);
                }
            }
        }
        for (BlockPos Pos : RaidStash.nearby(World, Near, Radius)) {
            if (!Found.contains(Pos)) {
                Found.add(Pos);
            }
        }
        return Found;
    }

    @Nullable
    public static Pick find(ServerLevel World, Raid Current, ChaosInsurgent Seeker, Set<Long> Refused) {
        List<Pick> Found = picks(World, Current, Seeker, Key -> Refused.contains(Key));
        return Found.isEmpty() ? null : Found.get(0);
    }

    public static List<Pick> picks(ServerLevel World, Raid Current, ChaosInsurgent Seeker, Predicate<Long> Skip) {
        BlockPos Centre = Current.estimate();
        int Radius = (int) Math.ceil(Current.footprint() + SearchMargin);
        List<Pick> Found = new ArrayList<>();
        List<BlockPos> Places = stashes(World, Current, Centre, Radius);
        for (BlockPos Pos : stashes(World, Current, Seeker.blockPosition(), OwnReach)) {
            if (!Places.contains(Pos)) {
                Places.add(Pos);
            }
        }
        for (BlockPos Pos : Places) {
            if (Skip.test(Pos.asLong()) || Current.claimed(Pos.asLong()) || !RaidStash.holds(World, Pos, Wanted)) {
                continue;
            }
            double Weight = World.getBlockEntity(Pos) instanceof StorageContainerBlockEntity ? LockerWeight : ChestWeight;
            double Score = Math.sqrt(Pos.distSqr(Seeker.blockPosition())) + Weight;
            Found.add(new Pick(Pos, null, Score));
        }
        AABB Area = new AABB(Centre).inflate(Radius, 16.0, Radius).minmax(Seeker.getBoundingBox().inflate(OwnReach, 12.0, OwnReach));
        for (Entity Body : World.getEntities((Entity) null, Area, Candidate -> loose(Candidate) && !Skip.test(looseKey(Candidate)))) {
            if (Current.claimed(looseKey(Body))) {
                continue;
            }
            double Score = Body.distanceTo(Seeker) + LooseWeight;
            Found.add(new Pick(null, Body, Score));
        }
        Found.sort(Comparator.comparingDouble(Pick::Score));
        return Found;
    }

    public static long keyOf(Pick Found) {
        return Found.Pos() != null ? Found.Pos().asLong() : looseKey(Found.Body());
    }

    public static boolean reachable(ChaosInsurgent Seeker, Pick Found) {
        if (Found.Pos() != null) {
            return RaidAccess.touch(Seeker, Found.Pos()) || !RaidAccess.worthChecking(Seeker, Found.Pos()) || RaidAccess.reachable(Seeker, Found.Pos());
        }
        Entity Body = Found.Body();
        return Body != null && (RaidAccess.touch(Seeker, Body) || !RaidAccess.worthChecking(Seeker, Body.blockPosition()) || RaidAccess.reachable(Seeker, Body.blockPosition()));
    }

    public static boolean loose(Entity Body) {
        if (!Body.isAlive()) {
            return false;
        }
        if (Body instanceof ItemEntity Dropped) {
            return Wanted.test(Dropped.getItem());
        }
        return Body instanceof ItemFrame Frame && Wanted.test(Frame.getItem());
    }

    @Override
    public boolean canUse() {
        if (!ready() || --Cooldown > 0 || !(Owner.level() instanceof ServerLevel World)) {
            return false;
        }
        Cooldown = SearchEvery / 2 + Owner.getRandom().nextInt(SearchEvery / 2);
        Raid Current = Owner.raid();
        if (Current == null) {
            return false;
        }
        long Now = World.getGameTime();
        int Checked = 0;
        for (Pick Found : picks(World, Current, Owner, Key -> Refused.contains(Key) || shunned(Key, Now))) {
            if (Checked++ >= RouteChecks) {
                break;
            }
            long Key = keyOf(Found);
            if (!reachable(Owner, Found)) {
                shun(Key);
                continue;
            }
            if (!Current.claim(Key)) {
                continue;
            }
            Stash = Found.Pos();
            Loose = Found.Body();
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return (Stash != null || Loose != null && loose(Loose)) && ready() && Time < GiveUp;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        Time = 0;
        Work = 0;
        Repath = 0;
        Stall = 0;
        Owner.setTarget(null);
    }

    @Override
    public void stop() {
        Raid Current = Owner.raid();
        long Key = Stash != null ? Stash.asLong() : Loose != null ? looseKey(Loose) : 0L;
        if (Current != null && (Stash != null || Loose != null)) {
            Current.unclaim(Key);
        }
        if (Time >= GiveUp && (Stash != null || Loose != null)) {
            shun(Key);
        }
        Stash = null;
        Loose = null;
        Work = 0;
        Owner.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(Owner.level() instanceof ServerLevel World)) {
            return;
        }
        Time++;
        Vec3 Spot = Stash != null ? Vec3.atCenterOf(Stash) : Loose != null ? Loose.position() : null;
        if (Spot == null) {
            return;
        }
        Owner.getLookControl().setLookAt(Spot.x, Spot.y, Spot.z);
        boolean Reached = Stash != null ? RaidAccess.touch(Owner, Stash) : Loose != null && RaidAccess.touch(Owner, Loose);
        if (!Reached) {
            Work = 0;
            if (--Repath <= 0) {
                Repath = 15;
                Owner.getNavigation().moveTo(Spot.x, Spot.y, Spot.z, Speed);
            }
            if (Owner.getNavigation().isDone() && ++Stall > StallLimit) {
                Time = GiveUp;
            }
            return;
        }
        Stall = 0;
        AABB Box = Stash != null ? RaidAccess.box(World, Stash) : Loose.getBoundingBox();
        if (Box.distanceToSqr(Owner.getEyePosition()) > Comfort * Comfort && !Owner.getNavigation().isDone() && Work == 0) {
            return;
        }
        Owner.getNavigation().stop();
        if (Stash != null) {
            boolean Locker = World.getBlockEntity(Stash) instanceof StorageContainerBlockEntity;
            int Needed = Locker ? LockerTicks : ChestTicks;
            if (Work % StrikeEvery == 0) {
                Owner.swing(InteractionHand.MAIN_HAND);
                if (Locker) {
                    World.playSound(null, Stash, HostileRegistry.ForceLock.get(), SoundSource.HOSTILE, 1.0f, 0.9f + Owner.getRandom().nextFloat() * 0.2f);
                    World.playSound(null, Stash, HostileRegistry.ForceStrike.get(), SoundSource.HOSTILE, 0.8f, 1.0f);
                }
            }
            if (++Work < Needed) {
                return;
            }
            take(World, Stash);
            return;
        }
        if (Loose != null) {
            takeLoose(World, Loose);
        }
    }

    private void take(ServerLevel World, BlockPos Pos) {
        if (!RaidAccess.touch(Owner, Pos)) {
            Work = 0;
            return;
        }
        BlockEntity Tile = World.getBlockEntity(Pos);
        if (Tile instanceof StorageContainerBlockEntity Locker) {
            BlockState State = World.getBlockState(Pos);
            if (State.getBlock() instanceof StorageContainerBlock Cabinet && !State.getValue(StorageContainerBlock.Open) && !Cabinet.isDoorBlocked(World, Pos, State)) {
                Cabinet.toggle(World, Pos, State, null, Locker);
            }
        }
        ItemStack Taken = RaidStash.take(World, Pos, Wanted);
        if (Taken.isEmpty()) {
            Refused.add(Pos.asLong());
            Stash = null;
            return;
        }
        secured(World, Taken);
        Raid Current = Owner.raid();
        if (Current != null && !RaidStash.holds(World, Pos, Wanted)) {
            RaidBases.get(World.getServer()).forgetStash(Current.group(), Current.dimension(), Pos);
        }
        Stash = null;
    }

    private void takeLoose(ServerLevel World, Entity Body) {
        ItemStack Taken = ItemStack.EMPTY;
        if (!RaidAccess.touch(Owner, Body)) {
            return;
        }
        if (Body instanceof ItemEntity Dropped && Wanted.test(Dropped.getItem())) {
            Taken = Dropped.getItem().copy();
            Dropped.discard();
        } else if (Body instanceof ItemFrame Frame && Wanted.test(Frame.getItem())) {
            Taken = Frame.getItem().copy();
            Frame.setItem(ItemStack.EMPTY);
        }
        Loose = null;
        if (!Taken.isEmpty()) {
            secured(World, Taken);
        }
    }

    private void secured(ServerLevel World, ItemStack Taken) {
        Raid Current = Owner.raid();
        RaidContext Context = HostileRaidStyle.context(Owner);
        HostileTrophy Trophy = HostileTrophy.of(Taken, World.registryAccess());
        if (Current == null || Trophy == null) {
            Owner.spawnAtLocation(Taken);
            return;
        }
        Owner.swing(InteractionHand.MAIN_HAND);
        HostileHauls.put(Current, Owner.getUUID(), Trophy);
        Owner.carry(Taken);
        Current.addStolen();
        if (Context != null) {
            Context.notice("stolen", Taken.getHoverName());
        }
    }
}
