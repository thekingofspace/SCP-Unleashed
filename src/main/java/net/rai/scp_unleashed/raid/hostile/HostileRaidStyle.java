package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionWorld;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidMark;
import net.rai.scp_unleashed.raid.RaidOutcome;
import net.rai.scp_unleashed.raid.RaidRegistry;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.RaidStyle;
import net.rai.scp_unleashed.raid.RaidWorld;
import net.rai.scp_unleashed.raid.serpentshand.SerpentPodItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class HostileRaidStyle implements RaidStyle {
    public static final int WarningTicks = 400;
    public static final long DurationTicks = 6000L;
    public static final int MaxAlive = 8;
    public static final String SecondsKey = "Seconds";
    public static final String LevelKey = "Level";
    public static final String PendingKey = "Pending";
    public static final String SquadKey = "Squad";
    public static final String SeekersKey = "Seekers";
    public static final String SpawnedKey = "SpawnedCount";
    public static final String BegunKey = "Begun";
    public static final double OutsideMin = 10.0;
    public static final double OutsideMax = 22.0;

    public abstract HostileFaction faction();

    public abstract String key();

    protected abstract int[] squadTable();

    protected abstract int seekers(int Level);

    protected abstract boolean arrival(RaidContext Context);

    protected abstract void upkeep(RaidContext Context, int Seconds);

    protected abstract void retreating(RaidContext Context);

    protected abstract void close(RaidContext Context, RaidOutcome Outcome);

    protected abstract void fade(ServerLevel World, HostileRaider Raider);

    public abstract void arrive(RaidContext Context, HostileRaider Raider);

    protected abstract void equip(RaidContext Context, HostileRaider Raider);

    public void sinking(RaidContext Context, HostileRaider Raider) {
    }

    @Override
    public int warningTicks() {
        return WarningTicks;
    }

    @Override
    public long durationTicks() {
        return DurationTicks;
    }

    @Override
    public long cooldownTicks() {
        return HostileFaction.DayTicks;
    }

    @Override
    public int maxAlive() {
        return MaxAlive;
    }

    @Override
    public boolean allowsDimension(ResourceKey<Level> Dimension) {
        return Dimension == Level.OVERWORLD;
    }

    @Override
    public String noticeKey(String Event) {
        return "raid.scp_unleashed." + key() + "." + Event;
    }

    public static int level(RaidContext Context) {
        CompoundTag Data = Context.data();
        return Data.contains(LevelKey) ? Data.getInt(LevelKey) : HostileFaction.level(Context.State());
    }

    @Override
    public boolean begin(RaidContext Context) {
        CompoundTag Data = Context.data();
        int Level = HostileFaction.level(Context.State());
        int Squad = HostileLevels.squad(squadTable(), Level, Context.World().getDifficulty());
        Data.putInt(LevelKey, Level);
        Data.putInt(SquadKey, Squad);
        Data.putInt(PendingKey, Squad);
        Data.putInt(SeekersKey, Math.min(Squad, seekers(Level)));
        Data.putInt(SpawnedKey, 0);
        Data.putInt(SecondsKey, 0);
        Data.putBoolean(BegunKey, true);
        Context.changed();
        return arrival(Context);
    }

    @Override
    public void tick(RaidContext Context) {
        CompoundTag Data = Context.data();
        int Seconds = Data.getInt(SecondsKey) + 1;
        Data.putInt(SecondsKey, Seconds);
        if (!Context.Current().forced() && !faction().ruleOn(Context.Server())) {
            RaidDirector.end(Context, RaidOutcome.Stopped);
            return;
        }
        upkeep(Context, Seconds);
        Context.changed();
    }

    @Override
    public boolean exhausted(RaidContext Context) {
        return Context.data().getInt(PendingKey) <= 0;
    }

    @Override
    public void retreat(RaidContext Context) {
        for (Mob Raider : Context.loadedRaiders()) {
            Raider.setTarget(null);
            Raider.setAggressive(false);
        }
        retreating(Context);
    }

    @Override
    public void tickRetreat(RaidContext Context) {
        CompoundTag Data = Context.data();
        int Seconds = Data.getInt(SecondsKey) + 1;
        Data.putInt(SecondsKey, Seconds);
        upkeep(Context, Seconds);
        Context.changed();
    }

    public int pending(RaidContext Context) {
        return Context.data().getInt(PendingKey);
    }

    @Nullable
    public HostileRaider spawn(RaidContext Context, Vec3 At, float Yaw) {
        CompoundTag Data = Context.data();
        int Pending = Data.getInt(PendingKey);
        if (Pending <= 0 || Context.room() <= 0) {
            return null;
        }
        HostileRaider Made = create(Context.World());
        if (Made == null) {
            return null;
        }
        int Index = Data.getInt(SpawnedKey);
        int Seekers = Data.getInt(SeekersKey);
        RandomSource Random = Context.random();
        Made.configure(level(Context), Index < Seekers ? HostileRaider.RoleSeeker : HostileRaider.RoleFighter, Index, Random);
        Made.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Made.setYHeadRot(Yaw);
        Made.setYBodyRot(Yaw);
        Context.enlist(Made, RaidMark.Raider);
        equip(Context, Made);
        Data.putInt(PendingKey, Pending - 1);
        Data.putInt(SpawnedKey, Index + 1);
        Context.changed();
        return Made;
    }

    @Nullable
    protected abstract HostileRaider create(ServerLevel World);

    public void secure(RaidContext Context, HostileTrophy Trophy) {
        FactionState State = Context.State();
        ServerLevel World = Context.World();
        HostileLedger.add(State, Trophy, Context.Now(), HostileLedger.dayOf(World.getServer().overworld().getDayTime()), HostileFaction.level(State));
        faction().addXp(State, faction().securedXp(State));
        Context.notice("carried_off", Trophy.label(World.registryAccess()));
    }

    public void escape(RaidContext Context, HostileRaider Raider) {
        Raid Current = Context.Current();
        HostileTrophy Haul = HostileHauls.take(Current, Raider.getUUID());
        if (Haul != null) {
            secure(Context, Haul);
        }
        HostileHauls.unissue(Current, Raider.getUUID());
        Raider.clearIssue();
        Current.release(Raider.getUUID(), false);
        fade(Context.World(), Raider);
        Raider.discard();
    }

    @Override
    public void dismiss(ServerLevel World, Mob Body) {
        if (Body instanceof HostileRaider Raider) {
            Raid Current = record(Raider);
            if (Current != null) {
                HostileTrophy Haul = HostileHauls.take(Current, Raider.getUUID());
                HostileHauls.unissue(Current, Raider.getUUID());
                if (Haul != null) {
                    RaidContext Context = RaidDirector.context(World.getServer(), Current, World.getServer().overworld().getGameTime());
                    if (Current.outcome() == RaidOutcome.Stopped || Context == null) {
                        drop(World, Raider.position(), Haul);
                    } else {
                        secure(Context, Haul);
                    }
                }
            }
            Raider.clearIssue();
            fade(World, Raider);
        }
        Body.discard();
    }

    @Override
    public void ended(RaidContext Context, RaidOutcome Outcome) {
        Raid Current = Context.Current();
        ServerLevel World = Context.World();
        Map<UUID, HostileTrophy> Left = HostileHauls.all(Current);
        for (HostileTrophy Haul : Left.values()) {
            BlockPos Target = Current.target();
            if (Outcome == RaidOutcome.Stopped && World.isLoaded(Target)) {
                drop(World, Vec3.atBottomCenterOf(Target).add(0.0, 0.5, 0.0), Haul);
            } else {
                secure(Context, Haul);
            }
        }
        HostileHauls.clear(Current);
        close(Context, Outcome);
        faction().schedule(Context.Server(), Context.State(), Outcome, Context.data().getBoolean(BegunKey), Context.random());
    }

    @Nullable
    public static Raid record(HostileRaider Raider) {
        if (!(Raider.level() instanceof ServerLevel World) || !Raider.hasData(RaidRegistry.Mark)) {
            return null;
        }
        RaidMark Mark = Raider.getData(RaidRegistry.Mark);
        return Mark.marked() ? RaidWorld.get(World.getServer()).raid(Mark.Raid()) : null;
    }

    @Nullable
    public static RaidContext context(HostileRaider Raider) {
        Raid Current = Raider.raid();
        if (Current == null || !(Raider.level() instanceof ServerLevel World)) {
            return null;
        }
        return RaidDirector.context(World.getServer(), Current, World.getServer().overworld().getGameTime());
    }

    @Nullable
    public static HostileRaidStyle styleOf(@Nullable RaidContext Context) {
        return Context != null && Context.Style() instanceof HostileRaidStyle Style ? Style : null;
    }

    public static void departed(HostileRaider Raider) {
        RaidContext Context = context(Raider);
        HostileRaidStyle Style = styleOf(Context);
        if (Style == null) {
            Raider.discard();
            return;
        }
        Style.escape(Context, Raider);
    }

    public static void arrived(HostileRaider Raider) {
        RaidContext Context = context(Raider);
        HostileRaidStyle Style = styleOf(Context);
        if (Style == null) {
            return;
        }
        Style.arrive(Context, Raider);
    }

    public static void sinking(HostileRaider Raider) {
        RaidContext Context = context(Raider);
        HostileRaidStyle Style = styleOf(Context);
        if (Style != null) {
            Style.sinking(Context, Raider);
        }
    }

    public static ItemStack item(HostileTrophy Trophy, HolderLookup.Provider Lookup) {
        return Trophy.creature() ? SerpentPodItem.filled(Trophy, Lookup) : Trophy.stack(Lookup);
    }

    public static void drop(ServerLevel World, Vec3 At, HostileTrophy Trophy) {
        ItemStack Stack = item(Trophy, World.registryAccess());
        if (Stack.isEmpty()) {
            return;
        }
        ItemEntity Dropped = new ItemEntity(World, At.x, At.y + 0.5, At.z, Stack);
        Dropped.setDefaultPickUpDelay();
        Dropped.setDeltaMovement(World.random.nextGaussian() * 0.05, 0.2, World.random.nextGaussian() * 0.05);
        World.addFreshEntity(Dropped);
    }

    public static void died(HostileRaider Raider) {
        if (!(Raider.level() instanceof ServerLevel World)) {
            return;
        }
        Raid Current = record(Raider);
        Vec3 At = Raider.position();
        if (Current != null) {
            HostileTrophy Haul = HostileHauls.take(Current, Raider.getUUID());
            if (Haul != null) {
                drop(World, At, Haul);
                RaidContext Context = RaidDirector.context(World.getServer(), Current, World.getServer().overworld().getGameTime());
                if (Context != null) {
                    Context.notice("dropped", Haul.label(World.registryAccess()));
                }
            }
            HostileHauls.unissue(Current, Raider.getUUID());
        }
        int EntryId = Raider.issued();
        if (EntryId >= 0) {
            FactionState State = FactionWorld.state(World.getServer(), Raider.faction());
            HostileLedger.Entry Found = HostileLedger.find(State, EntryId);
            ItemStack Held = Raider.takeIssued();
            if (Found != null) {
                HostileLedger.remove(State, EntryId);
                if (Held.isEmpty() || Found.Trophy().creature()) {
                    drop(World, At, Found.Trophy());
                } else {
                    ItemEntity Dropped = new ItemEntity(World, At.x, At.y + 0.5, At.z, Held);
                    Dropped.setDefaultPickUpDelay();
                    World.addFreshEntity(Dropped);
                }
            }
        }
    }

    public static Vec3 outside(RaidContext Context, double Extra, double Spread) {
        Raid Current = Context.Current();
        RandomSource Random = Context.random();
        double Reach = Current.footprint() + OutsideMin + Extra + Random.nextDouble() * (OutsideMax - OutsideMin);
        double Angle = Current.heading() + (Random.nextDouble() - 0.5) * Spread;
        BlockPos Target = Current.target();
        return new Vec3(Target.getX() + 0.5 + Math.cos(Angle) * Reach, Target.getY(), Target.getZ() + 0.5 + Math.sin(Angle) * Reach);
    }

    public static float yawToward(Vec3 From, Vec3 To) {
        return (float) (Mth.atan2(To.z - From.z, To.x - From.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public static List<HostileRaider> raiders(RaidContext Context) {
        return Context.loadedRaiders().stream().filter(Body -> Body instanceof HostileRaider).map(Body -> (HostileRaider) Body).toList();
    }

    public static boolean active(RaidContext Context) {
        return Context.Current().stage() == RaidStage.Active;
    }
}
