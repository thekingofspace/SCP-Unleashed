package net.rai.scp_unleashed.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class FactionTicker {
    public static final int HomeRadius = 48;
    public static final int AtSiteRadius = 64;
    public static final int PatrolMin = 3;
    public static final int PatrolSize = 3;

    private FactionTicker() {
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        if (Server.getTickCount() % FactionRules.TickInterval != 0 || Factions.all().isEmpty()) {
            return;
        }
        long Now = Server.overworld().getGameTime();
        for (FactionType Type : Factions.all()) {
            FactionState State = FactionWorld.state(Server, Type);
            if (State.phase().active()) {
                long Elapsed = accrue(Type, State, Now);
                sites(Server, Type, State, Now, Elapsed);
                expand(Server, Type, State, Now);
                patrol(Server, Type, State, Now);
                raid(Server, Type, State, Now);
            } else {
                State.setLastAccrual(Now);
            }
            Type.tick(Server, State);
        }
    }

    private static long accrue(FactionType Type, FactionState State, long Now) {
        long Last = State.lastAccrual();
        if (Last < 0L || Last > Now) {
            State.setLastAccrual(Now);
            return 0L;
        }
        long Elapsed = Now - Last;
        if (Elapsed <= 0L) {
            return 0L;
        }
        double Gain = Type.accrual(State, State.roster().size()) * Elapsed;
        if (Gain != 0.0 && State.power() < Type.rules().MaxPower()) {
            State.setPower(State.power() + Gain, Type.rules().MaxPower());
        }
        State.setLastAccrual(Now);
        return Elapsed;
    }

    private static void sites(MinecraftServer Server, FactionType Type, FactionState State, long Now, long Elapsed) {
        for (FactionSite Site : List.copyOf(State.sites())) {
            ServerLevel World = Server.getLevel(Site.dimension());
            if (World == null) {
                continue;
            }
            if (!Site.built()) {
                if (Site.readyToTry(Now) && FactionGround.areaLoaded(World, Site.anchor(), Type.siteRadius(Site))) {
                    if (Type.buildSite(World, State, Site)) {
                        Site.setLastReinforce(Now);
                        Site.setLastSeen(Now);
                        State.changed();
                        Type.siteBuilt(World, State, Site);
                    } else {
                        Site.failedTry(Now, FactionRules.BuildRetry);
                        State.changed();
                    }
                }
                continue;
            }
            int Cap = Type.siteCap(State, Site);
            int Present = State.rosterAt(Site.id());
            long Interval = Math.max(20L, Type.reinforceInterval(State, Site));
            if (!World.isPositionEntityTicking(Site.anchor())) {
                if (Present < Cap && Elapsed > 0L) {
                    Site.setReserve(Math.min(Cap - Present, Site.reserve() + (double) Elapsed / Interval));
                }
                Site.setLastReinforce(Now);
                continue;
            }
            Site.setLastSeen(Now);
            if (Present < Cap && FactionLive.total() < FactionRules.GlobalLoadedCap) {
                boolean Owed = Site.reserve() >= 1.0;
                if (Owed || Now - Site.lastReinforce() >= Interval) {
                    Mob Recruit = Type.recruit(World, State, Site);
                    if (Recruit != null) {
                        if (Owed) {
                            Site.setReserve(Site.reserve() - 1.0);
                        } else {
                            Site.setLastReinforce(Now);
                        }
                        State.changed();
                    }
                }
            } else {
                Site.setReserve(0.0);
                Site.setLastReinforce(Now);
            }
            Type.tickSite(World, State, Site);
        }
    }

    private static void expand(MinecraftServer Server, FactionType Type, FactionState State, long Now) {
        long Interval = Type.rules().ExpansionInterval();
        if (Interval <= 0L || Now - State.lastExpansion() < Interval) {
            return;
        }
        State.setLastExpansion(Now);
        if (State.sites().size() < Type.rules().SiteCap()) {
            Type.planExpansion(Server, State, null);
        }
    }

    private static void patrol(MinecraftServer Server, FactionType Type, FactionState State, long Now) {
        if (!Type.patrolsEnabled(State) || Now - State.lastPatrol() < Type.rules().PatrolInterval()) {
            return;
        }
        State.setLastPatrol(Now);
        List<FactionSite> Order = new ArrayList<>(State.sites());
        Collections.shuffle(Order);
        for (FactionSite Site : Order) {
            ServerLevel World = Server.getLevel(Site.dimension());
            if (World == null || !Site.built() || !World.isPositionEntityTicking(Site.anchor())) {
                continue;
            }
            List<Mob> Idle = new ArrayList<>();
            for (Mob Member : FactionLive.atSite(Type.id(), Site.id())) {
                if (Type.idleMember(Member) && Member.blockPosition().distSqr(Site.anchor()) < (double) HomeRadius * HomeRadius) {
                    Idle.add(Member);
                }
            }
            if (Idle.size() < PatrolMin) {
                continue;
            }
            Vec3 Target = patrolTarget(World, Site.anchor(), Type.rules().PatrolRange());
            if (Target == null) {
                continue;
            }
            Collections.shuffle(Idle);
            List<Mob> Group = Idle.subList(0, Math.min(PatrolSize, Idle.size() - 1));
            Type.patrol(World, State, Site, Target, new ArrayList<>(Group));
            return;
        }
    }

    @Nullable
    public static Vec3 patrolTarget(ServerLevel World, BlockPos From, int Range) {
        ServerPlayer Best = null;
        double BestGap = (double) Range * Range;
        for (ServerPlayer User : World.players()) {
            if (User.isSpectator() || User.isCreative() || Scp268.isNeglected(User)) {
                continue;
            }
            double Gap = User.blockPosition().distSqr(From);
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = User;
            }
        }
        if (Best != null) {
            return Best.position();
        }
        Optional<BlockPos> Village = World.getPoiManager().findClosest(Holder -> Holder.is(PoiTypes.MEETING), From, Range, PoiManager.Occupancy.ANY);
        return Village.map(Vec3::atBottomCenterOf).orElse(null);
    }

    private static void raid(MinecraftServer Server, FactionType Type, FactionState State, long Now) {
        if (!Type.patrolsEnabled(State) || Now - State.lastRaid() < Type.rules().RaidInterval()) {
            return;
        }
        State.setLastRaid(Now);
        List<ServerPlayer> Users = new ArrayList<>(Server.getPlayerList().getPlayers());
        Collections.shuffle(Users);
        double Territory = (double) Type.rules().TerritoryRange() * Type.rules().TerritoryRange();
        for (ServerPlayer User : Users) {
            if (User.isSpectator() || User.isCreative() || Scp268.isNeglected(User) || !AnomalyRealm.allows(User.level())) {
                continue;
            }
            FactionSite Nearest = null;
            double NearestGap = Double.MAX_VALUE;
            boolean AtSite = false;
            for (FactionSite Site : State.sites()) {
                if (!Site.built() || !Site.dimension().equals(User.level().dimension())) {
                    continue;
                }
                double Gap = User.blockPosition().distSqr(Site.anchor());
                if (Gap < (double) AtSiteRadius * AtSiteRadius) {
                    AtSite = true;
                }
                if (Gap < NearestGap) {
                    NearestGap = Gap;
                    Nearest = Site;
                }
            }
            if (AtSite || Nearest == null || NearestGap > Territory) {
                continue;
            }
            Type.raid(User.serverLevel(), State, User, Nearest);
            return;
        }
    }
}
