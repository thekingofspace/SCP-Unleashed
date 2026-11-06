package net.rai.scp_unleashed.raid;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.faction.FactionWorld;
import net.rai.scp_unleashed.faction.Factions;
import net.rai.scp_unleashed.raid.base.BaseEstimate;
import net.rai.scp_unleashed.raid.base.BaseSignals;
import net.rai.scp_unleashed.raid.base.BaseWeights;
import net.rai.scp_unleashed.raid.base.RaidBases;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class RaidDirector {
    private static final Logger Log = LogUtils.getLogger();
    public static final String ExitKey = "Exit";

    public record Candidate(ServerPlayer User, String Group, ResourceKey<Level> Dimension, BaseEstimate Estimate) {
    }

    private RaidDirector() {
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        int Tick = Server.getTickCount();
        if (Tick % BaseWeights.SampleEvery == 0) {
            BaseSignals.sample(Server);
        }
        if (Tick % RaidRules.TickEvery != 0) {
            return;
        }
        long Now = Server.overworld().getGameTime();
        RaidWorld Data = RaidWorld.get(Server);
        for (Raid Current : Data.snapshot()) {
            try {
                tickRaid(Server, Data, Current, Now);
            } catch (RuntimeException Problem) {
                Log.error("Raid {} failed to tick and was called off", Current.id(), Problem);
                abandon(Server, Data, Current, Now);
            }
        }
        if (Tick % RaidRules.PlanEvery == 0) {
            plan(Server, Data, Now);
        }
        if (Tick % RaidRules.PruneEvery == 0) {
            RaidBases.get(Server).prune(Now);
        }
    }

    public static boolean defender(Player User) {
        return !User.isSpectator() && !User.isCreative() && User.isAlive() && Targeting.isTargetable(User);
    }

    public static boolean unattended(MinecraftServer Server, Raid Current) {
        return !Current.forced() && !groupOnline(Server, Current.group());
    }

    public static boolean groupOnline(MinecraftServer Server, String Group) {
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            if (Group.equals(RaidBases.groupOf(User))) {
                return true;
            }
        }
        return false;
    }

    public static boolean peaceful(MinecraftServer Server) {
        return Server.overworld().getDifficulty() == Difficulty.PEACEFUL;
    }

    public static boolean enabled(MinecraftServer Server) {
        return RaidRegistry.enabled(Server) && !peaceful(Server);
    }

    @Nullable
    public static RaidContext context(MinecraftServer Server, Raid Current, long Now) {
        ServerLevel World = Server.getLevel(Current.dimension());
        FactionType Type = Factions.get(Current.faction());
        RaidStyle Style = Type == null ? null : Type.raidStyle();
        if (World == null || Type == null || Style == null) {
            return null;
        }
        FactionState State = FactionWorld.state(Server, Type);
        RaidLedger Ledger = RaidWorld.get(Server).ledger(Type.id(), State.era());
        return new RaidContext(Server, World, Current, Type, State, Style, Ledger, Now);
    }

    private static void plan(MinecraftServer Server, RaidWorld Data, long Now) {
        if (!enabled(Server) || Data.active().size() >= RaidRules.MaxActive) {
            return;
        }
        RandomSource Random = Server.overworld().random;
        for (FactionType Type : Factions.all()) {
            RaidStyle Style = Type.raidStyle();
            if (Style == null || Data.activeFor(Type.id()) != null) {
                continue;
            }
            FactionState State = FactionWorld.state(Server, Type);
            if (!Type.canRaid(State)) {
                continue;
            }
            RaidLedger Ledger = Data.ledger(Type.id(), State.era());
            if (Ledger.nextRaid() <= 0L) {
                Ledger.setNextRaid(Now + (long) (cooldown(Server, Style, RaidOutcome.None, Random) * RaidRules.FirstDelayShare));
                continue;
            }
            if (Now < Ledger.nextRaid() || Random.nextDouble() > RaidRules.PlanChance) {
                continue;
            }
            Candidate Chosen = pick(Server, Type, Style, State, null);
            if (Chosen != null && launch(Server, Data, Type, Style, State, Chosen, false, Now) != null) {
                return;
            }
        }
    }

    private static boolean nearSite(FactionState State, ServerPlayer User) {
        for (FactionSite Site : State.sites()) {
            if (Site.dimension().equals(User.level().dimension()) && Site.anchor().distSqr(User.blockPosition()) < RaidRules.SiteClearance * RaidRules.SiteClearance) {
                return true;
            }
        }
        return false;
    }

    public static boolean home(ServerPlayer User, BaseEstimate Estimate) {
        double Reach = RaidRules.HomeReach + Estimate.Footprint();
        return Estimate.horizontalGap(User.getX(), User.getZ()) <= Reach && Math.abs(User.getY() - Estimate.Y()) <= RaidRules.HomeDepth;
    }

    @Nullable
    public static Candidate pick(MinecraftServer Server, FactionType Type, RaidStyle Style, FactionState State, @Nullable ServerPlayer Prefer) {
        long Now = Server.overworld().getGameTime();
        RaidBases Bases = RaidBases.get(Server);
        Map<String, BaseEstimate> Cache = new HashMap<>();
        List<Candidate> Found = new ArrayList<>();
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            if (!defender(User) || !Style.allowsDimension(User.level().dimension()) || !AnomalyRealm.allows(User.level()) || nearSite(State, User) || Prefer != null && User != Prefer) {
                continue;
            }
            String Group = RaidBases.groupOf(User);
            ResourceKey<Level> Dimension = User.level().dimension();
            String Key = Group + "|" + Dimension.location();
            BaseEstimate Estimate = Cache.computeIfAbsent(Key, Ignored -> Bases.estimate(Group, Dimension, Now));
            if (Estimate == null || Estimate.Confidence() < Style.minConfidence() || !home(User, Estimate)) {
                continue;
            }
            Found.add(new Candidate(User, Group, Dimension, Estimate));
        }
        if (Found.isEmpty()) {
            return null;
        }
        return Found.get(Server.overworld().random.nextInt(Found.size()));
    }

    public static Candidate forcedCandidate(ServerPlayer User, RaidStyle Style) {
        MinecraftServer Server = User.server;
        long Now = Server.overworld().getGameTime();
        String Group = RaidBases.groupOf(User);
        ResourceKey<Level> Dimension = User.level().dimension();
        BaseEstimate Estimate = RaidBases.get(Server).estimate(Group, Dimension, Now);
        if (Estimate == null || !home(User, Estimate)) {
            Estimate = new BaseEstimate(User.getX(), User.getY(), User.getZ(), 16.0, 0.5, 1.0, 0.0, 0.0, 1, 0);
        }
        return new Candidate(User, Group, Dimension, Estimate);
    }

    @Nullable
    public static Raid launch(MinecraftServer Server, RaidWorld Data, FactionType Type, RaidStyle Style, FactionState State, Candidate Chosen, boolean Forced, long Now) {
        ServerLevel World = Server.getLevel(Chosen.Dimension());
        if (World == null) {
            return null;
        }
        RandomSource Random = World.random;
        BaseEstimate Estimate = Chosen.Estimate();
        BlockPos Centre = BlockPos.containing(Estimate.X(), Estimate.Y(), Estimate.Z());
        RaidLedger Ledger = Data.ledger(Type.id(), State.era());
        int Intel = Ledger.intelNear(Chosen.Dimension(), Centre);
        double[] Aim = Estimate.aim(Random.nextDouble(), Random.nextDouble(), Intel);
        int AimX = Mth.floor(Aim[0]);
        int AimZ = Mth.floor(Aim[1]);
        BlockPos Ground = RaidGround.standNear(World, AimX, AimZ, Centre.getY(), 12);
        BlockPos Target = Ground != null ? Ground : new BlockPos(AimX, Centre.getY(), AimZ);
        double Heading = Style.heading(World, Type, State, Target);
        Raid Current = new Raid(Data.nextId(), Type.id(), State.era(), Chosen.Dimension(), Chosen.Group(), Centre, Estimate.error(Intel), Estimate.Footprint(),
                Estimate.Confidence(), Heading, Forced, Now, Target);
        BlockPos Exit = BlockPos.containing(Centre.getX() + Math.cos(Heading) * (Estimate.Footprint() + 40.0), Centre.getY(),
                Centre.getZ() + Math.sin(Heading) * (Estimate.Footprint() + 40.0));
        Current.extra().putLong(ExitKey, Exit.asLong());
        Data.add(Current);
        Ledger.learn(Chosen.Dimension(), Centre, Now);
        RaidContext Context = new RaidContext(Server, World, Current, Type, State, Style, Ledger, Now);
        Style.warn(Context);
        Context.notice("warning");
        Log.info("Raid {} by {} begins its warning near {} {} {} (estimate error {} blocks)", Current.id(), Type.id(), Target.getX(), Target.getY(), Target.getZ(),
                Math.round(Current.error()));
        return Current;
    }

    private static void tickRaid(MinecraftServer Server, RaidWorld Data, Raid Current, long Now) {
        RaidContext Context = context(Server, Current, Now);
        if (Context == null) {
            abandon(Server, Data, Current, Now);
            return;
        }
        FactionState State = Context.State();
        boolean Gone = State.era() != Current.era() || State.phase() == FactionPhase.Defeated || !Current.forced() && !State.phase().active();
        if (Gone || peaceful(Server) || !Current.forced() && !RaidRegistry.enabled(Server) || unattended(Server, Current)) {
            end(Context, RaidOutcome.Stopped);
            return;
        }
        RaidStyle Style = Context.Style();
        switch (Current.stage()) {
            case Warning -> {
                Style.tickWarning(Context);
                if (Now - Current.stageAt() >= Style.warningTicks()) {
                    if (Style.begin(Context)) {
                        Current.setStage(RaidStage.Active, Now);
                        Current.setDeadline(Now + Style.durationTicks());
                        Current.setLastPresence(Now);
                        Context.notice("start");
                    } else {
                        end(Context, RaidOutcome.Stopped);
                    }
                }
            }
            case Active -> {
                Style.tick(Context);
                spotting(Context);
                if (!Context.defenders(RaidRules.PresenceReach).isEmpty()) {
                    Current.setLastPresence(Now);
                }
                if (Current.remaining() == 0 && Style.exhausted(Context)) {
                    end(Context, RaidOutcome.Defeated);
                } else if (Now >= Current.deadline()) {
                    retreat(Context, RaidOutcome.TimedOut);
                } else if (!Current.forced() && Now - Current.lastPresence() >= RaidRules.AbandonTicks) {
                    retreat(Context, RaidOutcome.Retreated);
                }
            }
            case Retreating -> {
                Style.tickRetreat(Context);
                if (Current.remaining() == 0 || Now - Current.stageAt() >= Style.retreatTicks()) {
                    end(Context, Current.outcome());
                }
            }
            default -> Data.remove(Current);
        }
    }

    private static void spotting(RaidContext Context) {
        for (Mob Raider : Context.loadedRaiders()) {
            LivingEntity Prey = Raider.getTarget();
            if (Prey instanceof Player User && defender(User) && Raider.hasLineOfSight(User)) {
                Context.spot(User.blockPosition());
                return;
            }
        }
    }

    public static void retreat(RaidContext Context, RaidOutcome Outcome) {
        Raid Current = Context.Current();
        if (Current.stage() != RaidStage.Active) {
            return;
        }
        Current.setOutcome(Outcome);
        Current.setStage(RaidStage.Retreating, Context.Now());
        Context.Style().retreat(Context);
        Context.notice(Outcome == RaidOutcome.TimedOut ? "timed_out" : "retreated");
    }

    public static void end(RaidContext Context, RaidOutcome Outcome) {
        Raid Current = Context.Current();
        if (!Current.live()) {
            return;
        }
        boolean Began = Current.stage() != RaidStage.Warning;
        Current.setOutcome(Outcome);
        Current.setStage(RaidStage.Ended, Context.Now());
        for (UUID Id : List.copyOf(Current.raiders())) {
            Mob Raider = Context.raider(Id);
            if (Raider != null) {
                stow(Context.Ledger(), Raider);
                Context.Style().dismiss(Context.World(), Raider);
            }
        }
        try {
            Context.Style().ended(Context, Outcome);
        } catch (RuntimeException Problem) {
            Log.warn("Raid style {} failed while ending raid {}", Context.Style().id(), Current.id(), Problem);
        }
        if (Outcome == RaidOutcome.Defeated || Outcome == RaidOutcome.Stopped) {
            Context.notice(Outcome.serialName());
        }
        Context.Ledger().record(Outcome);
        long Wait = !Began && Outcome == RaidOutcome.Stopped ? RaidRules.RetryTicks : (long) cooldown(Context.Server(), Context.Style(), Outcome, Context.random());
        Context.Ledger().setNextRaid(Context.Now() + Wait);
        RaidWorld.get(Context.Server()).remove(Current);
        Log.info("Raid {} by {} ended: {}", Current.id(), Current.faction(), Outcome.serialName());
    }

    private static void abandon(MinecraftServer Server, RaidWorld Data, Raid Current, long Now) {
        Current.setOutcome(RaidOutcome.Stopped);
        Current.setStage(RaidStage.Ended, Now);
        ServerLevel World = Server.getLevel(Current.dimension());
        if (World != null) {
            for (UUID Id : List.copyOf(Current.raiders())) {
                if (World.getEntity(Id) instanceof Mob Raider) {
                    RaidLoot.drop(Raider);
                    Raider.discard();
                }
            }
        }
        Data.remove(Current);
    }

    public static int stopAll(MinecraftServer Server) {
        RaidWorld Data = RaidWorld.get(Server);
        long Now = Server.overworld().getGameTime();
        int Count = 0;
        for (Raid Current : Data.snapshot()) {
            RaidContext Context = context(Server, Current, Now);
            if (Context != null) {
                end(Context, RaidOutcome.Stopped);
            } else {
                abandon(Server, Data, Current, Now);
            }
            Count++;
        }
        return Count;
    }

    public static double cooldown(MinecraftServer Server, RaidStyle Style, RaidOutcome Outcome, RandomSource Random) {
        double Scale = RaidRules.cooldownScale(Server.overworld().getDifficulty());
        if (Outcome == RaidOutcome.Defeated) {
            Scale *= RaidRules.DefeatedStretch;
        }
        Scale *= 1.0 + (Random.nextDouble() * 2.0 - 1.0) * RaidRules.Jitter;
        return Style.cooldownTicks() * Scale;
    }

    public static BlockPos exitOf(Raid Current) {
        if (Current.extra().contains(ExitKey)) {
            return BlockPos.of(Current.extra().getLong(ExitKey));
        }
        double Reach = Current.footprint() + 40.0;
        BlockPos Centre = Current.estimate();
        return BlockPos.containing(Centre.getX() + Math.cos(Current.heading()) * Reach, Centre.getY(), Centre.getZ() + Math.sin(Current.heading()) * Reach);
    }

    @Nullable
    public static Raid raidOf(Mob Raider) {
        if (!(Raider.level() instanceof ServerLevel World) || !Raider.hasData(RaidRegistry.Mark)) {
            return null;
        }
        RaidMark Mark = Raider.getData(RaidRegistry.Mark);
        return Mark.marked() ? RaidWorld.get(World.getServer()).raid(Mark.Raid()) : null;
    }

    private static void stow(RaidLedger Ledger, Mob Raider) {
        for (ItemStack Stack : RaidLoot.takeAll(Raider)) {
            Ledger.stow(Stack);
        }
    }

    public static void escape(Mob Raider) {
        if (!(Raider.level() instanceof ServerLevel World)) {
            return;
        }
        Raid Current = raidOf(Raider);
        RaidContext Context = Current == null ? null : context(World.getServer(), Current, World.getServer().overworld().getGameTime());
        if (Context == null) {
            RaidLoot.drop(Raider);
            Raider.discard();
            return;
        }
        List<ItemStack> Carried = RaidLoot.takeAll(Raider);
        for (ItemStack Stack : Carried) {
            Context.Ledger().stow(Stack);
        }
        Current.release(Raider.getUUID(), false);
        Context.Style().dismiss(World, Raider);
        if (!Carried.isEmpty()) {
            Context.notice("carried_off", Carried.get(0).getHoverName());
        }
    }

    public static void stolen(ServerLevel World, Raid Current, Mob Raider, ItemStack Stack) {
        Current.addStolen();
        RaidContext Context = context(World.getServer(), Current, World.getServer().overworld().getGameTime());
        if (Context != null) {
            Context.notice("stolen", Stack.getHoverName());
        }
    }
}
