package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionLive;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionRules;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.faction.FactionWorld;
import net.rai.scp_unleashed.faction.Factions;
import net.rai.scp_unleashed.raid.RaidStyle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp610Faction extends FactionType {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "flesh_that_hates");
    public static final int Colour = 0x6E3330;
    public static final double MaxPower = 4000.0;
    public static final double KillPower = 5.0;
    public static final int AscendFlesh = 192;
    public static final int AscendHives = 2;
    public static final int ApexFlesh = 640;
    public static final int ApexHive = 256;
    public static final double Hysteresis = 0.75;
    public static final int BaseUnits = 2;
    public static final int FleshPerUnit = 48;
    public static final int MaxUnits = 10;
    public static final long ReinforceTicks = 600L;
    public static final long ExpansionInterval = 48000L;
    public static final double BudShare = 0.6;
    public static final int BudFloor = 1024;
    public static final int FleshPerWorm = 128;
    public static final int MaxWorms = 2;
    public static final String RoleBrood = "brood";
    public static final String MatriarchKey = "Matriarch";
    public static final long MatriarchCheck = 200L;
    public static final int RecruitReach = 8;
    public static final String RoleUnit = "unit";
    public static final String RoleWorm = "worm";
    public static final Scp610Faction Instance = new Scp610Faction();

    private Scp610Faction() {
        super(Id, Colour, new FactionRules(MaxPower, 0.0, 0.0, 64, 160, ExpansionInterval, 6000L, 64, 256, Long.MAX_VALUE / 4));
    }

    @Override
    public double accrual(FactionState State, int Members) {
        return 0.0;
    }

    @Override
    public int siteCap(FactionState State, FactionSite Site) {
        Scp610Hive Hive = hiveOf(Site);
        int Flesh = Hive == null ? 0 : Hive.flesh();
        return Math.min(MaxUnits, BaseUnits + Flesh / FleshPerUnit);
    }

    @Override
    public long reinforceInterval(FactionState State, FactionSite Site) {
        return ReinforceTicks;
    }

    @Override
    public int siteRadius(FactionSite Site) {
        return 16;
    }

    @Nullable
    private static Scp610Hive hiveOf(FactionSite Site) {
        if (!Site.is(Scp610Hive.Kind)) {
            return null;
        }
        Scp610Hive Cached = Scp610Hives.cached(Site);
        return Cached != null ? Cached : Scp610Hive.load(Site);
    }

    public void refresh(MinecraftServer Server, FactionState State) {
        if (!State.phase().active()) {
            return;
        }
        int Total = 0;
        int Largest = 0;
        int Count = 0;
        for (Scp610Hive Hive : Scp610Hives.list(Server)) {
            Total += Hive.flesh();
            Largest = Math.max(Largest, Hive.flesh());
            Count++;
        }
        double Power = Math.min(MaxPower, Total + State.kills() * KillPower);
        if (Math.abs(Power - State.power()) > 0.01) {
            State.setPower(Power, MaxPower);
        }
        FactionPhase Wanted = phaseFor(State.phase(), Total, Largest, Count);
        if (Wanted != State.phase()) {
            State.setPhase(Wanted);
        }
    }

    public static FactionPhase phaseFor(FactionPhase Current, int Total, int Largest, int Hives) {
        boolean Apex = Current == FactionPhase.Apex ? Total >= ApexFlesh * Hysteresis && Largest >= ApexHive * Hysteresis : Total >= ApexFlesh && Largest >= ApexHive;
        if (Apex) {
            return FactionPhase.Apex;
        }
        boolean Ascend = Current == FactionPhase.Ascendant || Current == FactionPhase.Apex
                ? Total >= AscendFlesh * Hysteresis || Hives >= AscendHives
                : Total >= AscendFlesh || Hives >= AscendHives;
        return Ascend ? FactionPhase.Ascendant : FactionPhase.Awakened;
    }

    @Override
    public void advance(ServerLevel World, FactionState State, BlockPos Near) {
        Scp610Founding.found(World, Near, 0.0f);
    }

    @Override
    public void defeat(MinecraftServer Server, FactionState State, @Nullable BlockPos Where) {
        collapseAll(Server);
        Scp610Pact.clear(State);
    }

    @Override
    public void reset(MinecraftServer Server, FactionState State) {
        collapseAll(Server);
    }

    private static void collapseAll(MinecraftServer Server) {
        List<Entity> Doomed = new ArrayList<>(FactionLive.members(Id));
        for (ServerLevel World : Server.getAllLevels()) {
            for (Entity Body : World.getAllEntities()) {
                if (Body instanceof Scp610Creature && !Doomed.contains(Body)) {
                    Doomed.add(Body);
                }
            }
        }
        for (Entity Body : Doomed) {
            if (Body instanceof Scp610Creature Creature && Creature.isAlive()) {
                Creature.collapse();
            }
        }
    }

    @Override
    public boolean buildSite(ServerLevel World, FactionState State, FactionSite Site) {
        return true;
    }

    @Override
    public void tickSite(ServerLevel World, FactionState State, FactionSite Site) {
        Scp610Hive Hive = Scp610Hives.hive(World.getServer(), Site.id());
        if (Hive == null) {
            return;
        }
        Scp610Merging.tick(World, State, Hive);
        worms(World, State, Hive);
        broods(World, State, Hive);
    }

    private void broods(ServerLevel World, FactionState State, Scp610Hive Hive) {
        if (World.getGameTime() % 200L != 0L || Hive.flesh() < Scp610BroodSac.MinFlesh) {
            return;
        }
        if (State.rosterAt(Hive.id(), RoleBrood) > 0 || Scp610Biomass.countAt(Hive.id(), Scp610Form.BroodSac) > 0) {
            return;
        }
        BlockPos Spot = fleshTop(World, Hive, World.random, 3);
        if (Spot == null) {
            return;
        }
        Scp610Creature Made = Scp610Spawning.raise(World, Scp610Form.BroodSac, Vec3.atBottomCenterOf(Spot), World.random.nextFloat() * 360.0f, true);
        if (Made != null) {
            Made.setHome(Hive.anchor());
            FactionControl.enlist(this, State, Made, Hive.id(), RoleBrood);
        }
    }

    public boolean matriarchMade(FactionState State) {
        return State.extra().getBoolean(MatriarchKey);
    }

    @Nullable
    public Scp610Creature matriarch(MinecraftServer Server, FactionState State) {
        if (State.phase() != FactionPhase.Apex || matriarchMade(State) || !Scp610Lord.formed(State) || Scp610Lord.slain(State)) {
            return null;
        }
        Scp610HiveLord Eye = Scp610Lord.lord(Server, State);
        AABB Room = Scp610Lord.bounds(State);
        if (Eye == null || Room == null || !(Eye.level() instanceof ServerLevel World)) {
            return null;
        }
        Scp610Hive Payer = null;
        for (Scp610Hive Hive : Scp610Hives.list(Server)) {
            if (Hive.biomass() >= Scp610Biomass.MatriarchCost && (Payer == null || Hive.id() < Payer.id())) {
                Payer = Hive;
            }
        }
        if (Payer == null) {
            return null;
        }
        Vec3 Floor = new Vec3((Room.minX + Room.maxX) * 0.5, Room.minY + 1.0, (Room.minZ + Room.maxZ) * 0.5);
        Scp610Creature Made = Scp610Spawning.raise(World, Scp610Form.Matriarch, Floor, World.random.nextFloat() * 360.0f, true);
        if (Made == null) {
            return null;
        }
        Scp610Biomass.spend(Payer, Scp610Biomass.MatriarchCost);
        Made.setHome(BlockPos.containing(Floor));
        FactionControl.enlist(this, State, Made, Payer.id(), RoleUnit);
        State.extra().putBoolean(MatriarchKey, true);
        State.changed();
        return Made;
    }

    private void worms(ServerLevel World, FactionState State, Scp610Hive Hive) {
        if (World.getGameTime() % 200L != 0L || Hive.flesh() < FleshPerWorm) {
            return;
        }
        int Wanted = Math.min(MaxWorms, Hive.flesh() / FleshPerWorm);
        if (State.rosterAt(Hive.id(), RoleWorm) >= Wanted) {
            return;
        }
        BlockPos Spot = fleshTop(World, Hive, World.random, 3);
        if (Spot == null) {
            return;
        }
        Scp610Creature Made = Scp610Spawning.raise(World, Scp610Form.TendrilWorm, Vec3.atBottomCenterOf(Spot), World.random.nextFloat() * 360.0f, true);
        if (Made != null) {
            Made.setHome(Hive.anchor());
            FactionControl.enlist(this, State, Made, Hive.id(), RoleWorm);
        }
    }

    @Nullable
    public static BlockPos fleshTop(ServerLevel World, Scp610Hive Hive, RandomSource Random, int Clearance) {
        for (int Try = 0; Try < 12 && Hive.flesh() > 0; Try++) {
            BlockPos Ground = BlockPos.of(Hive.Frontier.size() > 0 && Random.nextInt(4) != 0 ? Hive.Frontier.random(Random) : Hive.Owned.random(Random));
            if (Ground.distSqr(Hive.anchor()) > RecruitReach * RecruitReach * 4 || Ground.distSqr(Hive.anchor()) < 9.0) {
                continue;
            }
            boolean Clear = true;
            for (int Up = 1; Up <= Clearance; Up++) {
                if (!World.getBlockState(Ground.above(Up)).getCollisionShape(World, Ground.above(Up)).isEmpty()) {
                    Clear = false;
                    break;
                }
            }
            if (Clear && World.getFluidState(Ground.above()).isEmpty()) {
                return Ground.above();
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Mob recruit(ServerLevel World, FactionState State, FactionSite Site) {
        Scp610Hive Hive = Scp610Hives.hive(World.getServer(), Site.id());
        if (Hive == null) {
            return null;
        }
        BlockPos Spot = fleshTop(World, Hive, World.random, 3);
        if (Spot == null) {
            Spot = Hive.anchor().offset(World.random.nextInt(7) - 3, 0, World.random.nextInt(7) - 3);
        }
        Scp610Form Form = Scp610Biomass.pickHiveRecruit(World.random, Hive);
        double Cost = Scp610Biomass.costOf(Form);
        if (Hive.biomass() < Cost) {
            return null;
        }
        Scp610Creature Made = Scp610Spawning.raise(World, Form, Vec3.atBottomCenterOf(Spot), World.random.nextFloat() * 360.0f, true);
        if (Made == null) {
            return null;
        }
        Scp610Biomass.spend(Hive, Cost);
        Made.setHome(Hive.anchor());
        FactionControl.enlist(this, State, Made, Site.id(), RoleUnit);
        return Made;
    }

    public static Scp610Form pickRecruit(RandomSource Random) {
        int Roll = Random.nextInt(11);
        if (Roll < 3) {
            return Scp610Form.Shambler;
        }
        if (Roll < 6) {
            return Scp610Form.Crawler;
        }
        return switch (Roll) {
            case 6 -> Scp610Form.Pig;
            case 7 -> Scp610Form.Cow;
            case 8 -> Scp610Form.Sheep;
            case 9 -> Scp610Form.Chicken;
            default -> Scp610Form.Villager;
        };
    }

    @Override
    public boolean planExpansion(MinecraftServer Server, FactionState State, @Nullable BlockPos Near) {
        if (State.phase() != FactionPhase.Ascendant && State.phase() != FactionPhase.Apex || Scp610Pact.active(State) || !Scp610Rules.spreads(Server)) {
            return false;
        }
        if (Scp610Hives.list(Server).size() >= Scp610Config.maxHives()) {
            return false;
        }
        for (Scp610Hive Hive : Scp610Hives.list(Server)) {
            if (Hive.flesh() < Math.min(Scp610Config.maxFleshPerHive() * BudShare, BudFloor)) {
                continue;
            }
            ServerLevel World = Server.getLevel(Hive.Site.dimension());
            if (World == null || !World.isPositionEntityTicking(Hive.anchor())) {
                continue;
            }
            BlockPos Spot = fleshTop(World, Hive, World.random, 3);
            if (Spot == null) {
                continue;
            }
            Scp610Creature Made = Scp610Spawning.raise(World, Scp610Form.Apostle, Vec3.atBottomCenterOf(Spot), World.random.nextFloat() * 360.0f, true);
            if (Made != null) {
                FactionControl.stamp(this, State, Made);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean patrolsEnabled(FactionState State) {
        return State.phase() == FactionPhase.Ascendant || State.phase() == FactionPhase.Apex;
    }

    @Override
    public void patrol(ServerLevel World, FactionState State, FactionSite Site, Vec3 Target, List<Mob> Group) {
        for (Mob Member : Group) {
            Member.getNavigation().moveTo(Target.x, Target.y, Target.z, 1.0);
        }
    }

    @Override
    public void raid(ServerLevel World, FactionState State, ServerPlayer Target, FactionSite From) {
    }

    @Override
    public RaidStyle raidStyle() {
        return Scp610RaidStyle.Instance;
    }

    @Override
    public boolean canRaid(FactionState State) {
        return Scp610RaidStyle.ready(State);
    }

    @Override
    public void tick(MinecraftServer Server, FactionState State) {
        if (!State.phase().active()) {
            return;
        }
        refresh(Server, State);
        if (Server.overworld().getGameTime() % MatriarchCheck < FactionRules.TickInterval) {
            matriarch(Server, State);
        }
        if (Scp610Lord.due(State) && Scp610Rules.spreads(Server) && !Scp610Pact.active(State)) {
            Scp610Lord.plan(Server.overworld(), State);
        }
        Scp610Pact.tick(Server, State);
    }

    @Override
    public boolean claims(Entity Body) {
        return Body instanceof Scp610Creature;
    }

    @Override
    public void staleMember(ServerLevel World, Entity Member) {
        if (Factions.bondOf(Member).Era() >= FactionWorld.state(World.getServer(), this).era()) {
            return;
        }
        if (Member instanceof Scp610Creature Creature) {
            Creature.collapse();
        } else {
            Member.discard();
        }
    }

    @Override
    public void memberLost(ServerLevel World, FactionState State, LivingEntity Member, DamageSource Source) {
    }

    @Override
    public boolean idleMember(Mob Member) {
        if (Member instanceof Scp610Creature Creature && (Creature.form().utility() || !Creature.form().mobile() || Creature.form() == Scp610Form.Matriarch)) {
            return false;
        }
        return Member.getTarget() == null && !(Member instanceof Scp610Unit Unit && Unit.merging());
    }

    @Override
    public boolean fallsWithLastSite() {
        return true;
    }

    @Override
    public void siteRazed(ServerLevel World, FactionState State, FactionSite Site, @Nullable BlockPos Where) {
        Scp610Hive Hive = Scp610Hives.hive(World.getServer(), Site.id());
        if (Hive != null) {
            Scp610Hives.forget(World, Hive);
        }
        if (Scp610Lord.planned(State) && !Scp610Lord.formed(State) && Scp610Lord.data(State).getInt("Site") == Site.id()) {
            State.extra().remove(Scp610Lord.LordKey);
            State.changed();
        }
    }
}
