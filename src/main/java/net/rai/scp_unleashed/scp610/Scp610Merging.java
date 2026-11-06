package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionLive;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class Scp610Merging {
    public static final long CheckTicks = 200L;
    public static final double Gather = 12.0;
    public static final int MinPlain = 3;
    public static final int EvolvedPerHive = 4;
    public static final int EvolvedCap = Scp610Biomass.AdvancedLoadedCap;
    public static final int AmalgamParts = 3;
    public static final int ColossusParts = 2;
    public static final int HeraldParts = 2;
    public static final long Converge = 200L;
    public static final double Touching = 1.5;

    public record Group(ResourceKey<Level> Dimension, int Site, Scp610Form Result, List<UUID> Parts, Vec3 Point, long Deadline) {
    }

    private static final List<Group> Pending = new ArrayList<>();

    private Scp610Merging() {
    }

    public static List<Group> pending() {
        return List.copyOf(Pending);
    }

    public static void clear() {
        Pending.clear();
    }

    public static boolean plain(Mob Member) {
        return Member instanceof Scp610Unit Unit && Unit.isAlive() && !Unit.form().evolved() && Unit.form().mobile() && !Unit.merging()
                && Unit.getTarget() == null && !Unit.isActing();
    }

    public static int evolvedAt(int Site) {
        int Count = 0;
        for (Mob Member : FactionLive.atSite(Scp610Faction.Id, Site)) {
            if (Member instanceof Scp610Creature Creature && Creature.form().evolved()) {
                Count++;
            }
        }
        return Count;
    }

    public static int evolvedLoaded() {
        int Count = 0;
        for (Entity Member : FactionLive.members(Scp610Faction.Id)) {
            if (Member instanceof Scp610Creature Creature && Creature.isAlive() && Creature.form().evolved()) {
                Count++;
            }
        }
        return Count;
    }

    public static void tick(ServerLevel World, FactionState State, Scp610Hive Hive) {
        long Now = World.getGameTime();
        advance(World, Now);
        if (State.phase() != FactionPhase.Apex || Now < Hive.NextMerge) {
            return;
        }
        Hive.NextMerge = Now + CheckTicks;
        begin(World, Hive, Now);
    }

    @Nullable
    public static Group begin(ServerLevel World, Scp610Hive Hive, long Now) {
        for (Group Each : Pending) {
            if (Each.Site() == Hive.id()) {
                return null;
            }
        }
        if (evolvedAt(Hive.id()) >= EvolvedPerHive || evolvedLoaded() >= EvolvedCap || Hive.biomass() < Scp610Biomass.MergeCost) {
            return null;
        }
        BlockPos Anchor = Hive.anchor();
        List<Scp610Unit> Near = new ArrayList<>();
        for (Mob Member : FactionLive.atSite(Scp610Faction.Id, Hive.id())) {
            if (Member.level() == World && plain(Member) && Member.blockPosition().distSqr(Anchor) <= Gather * Gather) {
                Near.add((Scp610Unit) Member);
            }
        }
        if (Near.size() < MinPlain) {
            return null;
        }
        List<Scp610Unit> Chosen = new ArrayList<>();
        Scp610Form Result;
        List<Scp610Unit> Quads = new ArrayList<>();
        List<Scp610Unit> Bipeds = new ArrayList<>();
        for (Scp610Unit Unit : Near) {
            if (Unit.form().frame() == Scp610Form.Body.Quadruped) {
                Quads.add(Unit);
            } else if (Unit.form() == Scp610Form.Shambler || Unit.form() == Scp610Form.Villager || Unit.form() == Scp610Form.Apostle) {
                Bipeds.add(Unit);
            }
        }
        if (Quads.size() >= AmalgamParts) {
            Result = Scp610Form.Amalgam;
            Chosen.addAll(Quads.subList(0, AmalgamParts));
        } else if (Bipeds.size() >= ColossusParts) {
            Result = bipedResult(Hive);
            Chosen.addAll(Bipeds.subList(0, ColossusParts));
        } else {
            Result = Scp610Form.Herald;
            Chosen.addAll(Near.subList(0, HeraldParts));
        }
        Scp610Biomass.spend(Hive, Scp610Biomass.MergeCost);
        if (Result == Scp610Form.SiegeBrute || Result == Scp610Form.Colossus) {
            Hive.nextMergeTurn();
        }
        Vec3 Point = Vec3.ZERO;
        for (Scp610Unit Unit : Chosen) {
            Point = Point.add(Unit.position());
        }
        Point = Point.scale(1.0 / Chosen.size());
        List<UUID> Parts = new ArrayList<>();
        for (Scp610Unit Unit : Chosen) {
            Unit.mergeAt(Point);
            Parts.add(Unit.getUUID());
        }
        Group Made = new Group(World.dimension(), Hive.id(), Result, Parts, Point, Now + Converge);
        Pending.add(Made);
        return Made;
    }

    public static Scp610Form bipedResult(Scp610Hive Hive) {
        boolean Brute = Hive.mergeTurn() % 2 == 0;
        if (Brute && Scp610Biomass.underCap(Hive, Scp610Form.SiegeBrute)) {
            return Scp610Form.SiegeBrute;
        }
        return Scp610Form.Colossus;
    }

    public static void advance(ServerLevel World, long Now) {
        Iterator<Group> Each = Pending.iterator();
        while (Each.hasNext()) {
            Group Current = Each.next();
            if (!Current.Dimension().equals(World.dimension())) {
                continue;
            }
            List<Scp610Unit> Members = members(World, Current);
            if (Members.size() < Current.Parts().size()) {
                for (Scp610Unit Unit : Members) {
                    Unit.mergeAt(null);
                }
                Each.remove();
                continue;
            }
            boolean Close = true;
            for (Scp610Unit Unit : Members) {
                if (Unit.position().distanceTo(Current.Point()) > Touching) {
                    Close = false;
                    break;
                }
            }
            if (Close || Now >= Current.Deadline()) {
                Each.remove();
                fuse(World, Current, Members);
            }
        }
    }

    private static List<Scp610Unit> members(ServerLevel World, Group Current) {
        List<Scp610Unit> Found = new ArrayList<>();
        for (UUID Id : Current.Parts()) {
            if (World.getEntity(Id) instanceof Scp610Unit Unit && Unit.isAlive()) {
                Found.add(Unit);
            }
        }
        return Found;
    }

    @Nullable
    public static Scp610Creature fuse(ServerLevel World, Group Current, List<Scp610Unit> Members) {
        Vec3 Point = Current.Point();
        for (Scp610Unit Unit : Members) {
            Scp610Infection.cloud(World, Unit.position().add(0.0, Unit.getBbHeight() * 0.5, 0.0), 1.0, 10);
            Unit.discard();
        }
        World.playSound(null, Point.x, Point.y, Point.z, Scp610Sounds.Merge.get(), SoundSource.HOSTILE, 1.6f, 0.8f);
        World.playSound(null, Point.x, Point.y, Point.z, Scp610Sounds.WetHeavy.get(), SoundSource.HOSTILE, 1.4f, 0.7f);
        Scp610Creature Made = Scp610Spawning.raise(World, Current.Result(), Point, World.random.nextFloat() * 360.0f, true);
        if (Made == null) {
            return null;
        }
        FactionState State = Scp610Hives.state(World.getServer());
        Scp610Hive Hive = Scp610Hives.hive(World.getServer(), Current.Site());
        if (Hive != null) {
            Made.setHome(Hive.anchor());
            FactionControl.enlist(Scp610Faction.Instance, State, Made, Current.Site(), Scp610Faction.RoleUnit);
        } else {
            FactionControl.stamp(Scp610Faction.Instance, State, Made);
        }
        return Made;
    }

    public static int fuseAll(MinecraftServer Server) {
        int Count = 0;
        for (Group Current : List.copyOf(Pending)) {
            ServerLevel World = Server.getLevel(Current.Dimension());
            if (World == null) {
                continue;
            }
            Pending.remove(Current);
            if (fuse(World, Current, members(World, Current)) != null) {
                Count++;
            }
        }
        return Count;
    }
}
