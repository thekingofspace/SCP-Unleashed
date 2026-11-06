package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.raid.RaidWorld;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class Scp610Lord {
    public static final String LordKey = "Lord";
    public static final int HalfWidth = 9;
    public static final int Height = 12;
    public static final long SlainSurgeTicks = 72000L;
    public static final double SlainSurge = 3.0;
    public static final long SlainRaid = 600L;

    private Scp610Lord() {
    }

    public static CompoundTag data(FactionState State) {
        CompoundTag Extra = State.extra();
        if (!Extra.contains(LordKey)) {
            Extra.put(LordKey, new CompoundTag());
        }
        return Extra.getCompound(LordKey);
    }

    public static boolean planned(FactionState State) {
        return State.extra().contains(LordKey) && State.extra().getCompound(LordKey).getBoolean("Planned");
    }

    public static boolean formed(FactionState State) {
        return State.extra().contains(LordKey) && State.extra().getCompound(LordKey).getBoolean("Formed");
    }

    public static boolean slain(FactionState State) {
        return State.extra().contains(LordKey) && State.extra().getCompound(LordKey).getBoolean("Slain");
    }

    @Nullable
    public static AABB bounds(FactionState State) {
        if (!planned(State)) {
            return null;
        }
        CompoundTag Lord = data(State);
        BlockPos Centre = BlockPos.of(Lord.getLong("Centre"));
        return new AABB(Centre.getX() - HalfWidth, Centre.getY() - 1, Centre.getZ() - HalfWidth, Centre.getX() + HalfWidth + 1, Centre.getY() + Height,
                Centre.getZ() + HalfWidth + 1);
    }

    public static boolean inChamber(ServerLevel World, BlockPos Pos) {
        FactionState State = Scp610Hives.state(World.getServer());
        AABB Room = bounds(State);
        if (Room == null || !formed(State) || slain(State)) {
            return false;
        }
        CompoundTag Lord = data(State);
        if (!World.dimension().location().toString().equals(Lord.getString("Dimension"))) {
            return false;
        }
        return Room.contains(Vec3.atCenterOf(Pos));
    }

    @Nullable
    public static Scp610HiveLord lord(MinecraftServer Server, FactionState State) {
        if (!formed(State) || slain(State)) {
            return null;
        }
        CompoundTag Lord = data(State);
        if (!Lord.hasUUID("Uuid")) {
            return null;
        }
        for (ServerLevel World : Server.getAllLevels()) {
            Entity Found = World.getEntity(Lord.getUUID("Uuid"));
            if (Found instanceof Scp610HiveLord Eye && Eye.isAlive()) {
                return Eye;
            }
        }
        return null;
    }

    public static boolean due(FactionState State) {
        FactionPhase Phase = State.phase();
        return (Phase == FactionPhase.Ascendant || Phase == FactionPhase.Apex) && !planned(State);
    }

    public static boolean plan(ServerLevel Overworld, FactionState State) {
        if (!due(State)) {
            return false;
        }
        MinecraftServer Server = Overworld.getServer();
        List<Scp610Hive> Order = Scp610Hives.list(Server);
        Order.sort(Comparator.comparingInt(Scp610Hive::id));
        for (Scp610Hive Hive : Order) {
            ServerLevel World = Server.getLevel(Hive.Site.dimension());
            if (World != null && planUnder(World, State, Hive)) {
                return true;
            }
        }
        return false;
    }

    public static boolean planUnder(ServerLevel World, FactionState State, Scp610Hive Hive) {
        RandomSource Random = RandomSource.create(Hive.Site.seed() ^ 0x610L);
        int Min = Scp610Config.lordDepthMin();
        int Depth = Min + Random.nextInt(Math.max(1, Scp610Config.lordDepthMax() - Min + 1));
        BlockPos Anchor = Hive.anchor();
        int Floor = Math.max(World.getMinBuildHeight() + 10, Anchor.getY() - Depth);
        if (Anchor.getY() - Floor < 12) {
            return false;
        }
        double Heading = Random.nextDouble() * Mth.TWO_PI;
        BlockPos Centre = new BlockPos(Anchor.getX() + Mth.floor(Math.cos(Heading) * 6.0), Floor, Anchor.getZ() + Mth.floor(Math.sin(Heading) * 6.0));
        double Back = Heading + Math.PI;
        BlockPos Entry = Centre.offset(Mth.floor(Math.cos(Back) * (Scp610Chambers.LordWide + 1.0)), 1, Mth.floor(Math.sin(Back) * (Scp610Chambers.LordWide + 1.0)));
        BlockPos Start = Anchor.below(3);
        Hive.Plan.add(0, new Scp610Chambers.Chamber(Scp610Chambers.Lord, Entry, Centre, 0));
        Hive.Plan.add(0, new Scp610Chambers.Chamber(Scp610Chambers.LordTunnel, Start, Entry, 0));
        Hive.forgetOps();
        Hive.Dirty = true;
        CompoundTag Lord = data(State);
        Lord.putBoolean("Planned", true);
        Lord.putInt("Site", Hive.id());
        Lord.putLong("Centre", Centre.asLong());
        Lord.putLong("Entry", Entry.asLong());
        Lord.putString("Dimension", World.dimension().location().toString());
        State.changed();
        return true;
    }

    public static void chamberDone(ServerLevel World, FactionState State, Scp610Hive Hive, Scp610Chambers.Chamber Room) {
        CompoundTag Lord = data(State);
        if (Lord.getBoolean("Formed") || Lord.getBoolean("Slain")) {
            return;
        }
        BlockPos Centre = Room.To;
        BlockPos Entry = Room.From;
        double Dx = Centre.getX() - Entry.getX();
        double Dz = Centre.getZ() - Entry.getZ();
        double Length = Math.max(0.001, Math.sqrt(Dx * Dx + Dz * Dz));
        double Far = Scp610Chambers.LordWide - 1.0;
        Vec3 Spot = new Vec3(Centre.getX() + 0.5 + Dx / Length * Far, Centre.getY() + 3.0, Centre.getZ() + 0.5 + Dz / Length * Far);
        float Yaw = (float) (Mth.atan2(-Dz, -Dx) * Mth.RAD_TO_DEG) - 90.0f;
        Scp610Creature Made = Scp610Spawning.raise(World, Scp610Form.HiveLord, Spot, Yaw, false);
        if (!(Made instanceof Scp610HiveLord Eye)) {
            return;
        }
        Eye.setHome(Centre);
        FactionControl.stamp(Scp610Faction.Instance, State, Eye);
        Lord.putBoolean("Formed", true);
        Lord.putUUID("Uuid", Eye.getUUID());
        State.changed();
        World.playSound(null, Eye.blockPosition(), Scp610Sounds.Evolve.get(), SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    public static int formNow(ServerLevel World, FactionState State) {
        if (!planned(State)) {
            plan(World, State);
        }
        if (!planned(State)) {
            return 0;
        }
        Scp610Hive Hive = Scp610Hives.hive(World.getServer(), data(State).getInt("Site"));
        if (Hive == null) {
            return 0;
        }
        int Changed = 0;
        int Guard = 0;
        while (!formed(State) && Hive.planningLord() && Guard++ < 200000) {
            Scp610Growth.Step Result = Scp610Growth.chamberStep(World, State, Hive);
            if (Result == Scp610Growth.Step.Changed) {
                Changed++;
            } else if (Result == Scp610Growth.Step.Idle) {
                break;
            }
        }
        return Changed;
    }

    public static void slainBy(ServerLevel World, Scp610HiveLord Eye, @Nullable Entity Killer) {
        MinecraftServer Server = World.getServer();
        FactionState State = Scp610Hives.state(Server);
        CompoundTag Lord = data(State);
        if (!Lord.hasUUID("Uuid") || !Lord.getUUID("Uuid").equals(Eye.getUUID())) {
            return;
        }
        Lord.putBoolean("Slain", true);
        Scp610Pact.forbid(State);
        if (State.phase().active()) {
            long Now = Server.overworld().getGameTime();
            Scp610Pact.surge(State, Now, SlainSurgeTicks, SlainSurge);
            RaidWorld.get(Server).ledger(Scp610Faction.Instance.id(), State.era()).setNextRaid(Now + SlainRaid);
            Scp610Pact.warn(Server, "dialogue.scp_unleashed.scp610.lord.slain");
        }
        State.changed();
    }

    @Nullable
    public static UUID uuid(FactionState State) {
        CompoundTag Lord = data(State);
        return Lord.hasUUID("Uuid") ? Lord.getUUID("Uuid") : null;
    }
}
