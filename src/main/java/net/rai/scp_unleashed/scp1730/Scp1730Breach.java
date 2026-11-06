package net.rai.scp_unleashed.scp1730;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp049.Scp049Cured;
import net.rai.scp_unleashed.scp049.Scp049Doctor;
import net.rai.scp_unleashed.scp049.Scp049Registry;
import net.rai.scp_unleashed.scp049.Scp049TrayBlockEntity;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Fixture;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730SetPieces;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Zone;
import net.rai.scp_unleashed.scp439.Scp439Creature;
import net.rai.scp_unleashed.scp457.Scp457;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Scp1730Breach {
    public static final String RoamingTag = "scp1730_roaming";
    public static final String SetPieceTag = "scp1730_setpiece";
    public static final int SpawnEvery = 60;
    public static final int DespawnEvery = 100;
    public static final double DespawnRange = 64.0;
    public static final double SpawnNear = 14.0;
    public static final double SpawnFar = 32.0;
    public static final int[] ZoneCaps = {6, 10, 12, 4};
    public static final int[] AreaCaps = {5, 8, 9, 3};
    public static final double AreaRadius = 32.0;
    public static final int MinBreachGap = 4800;
    public static final int MaxBreachGap = 8400;
    public static final int FirstBreach = 2400;
    public static final double BreachReach = 48.0;
    public static final Set<String> Excluded = Set.of("scp106_old_man", "scp2521", "scp2521_tendrils", "mtf_operative", "scp682", "scp001", "daevite_god",
            "scp4666_yule_man", "scp3008_staff", "scp303_doorman", "scp465_instance", "scp428_instance", "scp205_devil", "scp035_host", "scp016_skitter",
            "scp016_brute", "scp016_slinger", "scp016_duelist", "scp016_glutton", "scp016_flyer", "scp553_butterfly", "scp003_host", "scp3199");

    public record Threat(String Id, int Weight, int Pack) {
    }

    public static final Set<String> Biological = Scp1730SetPieces.Biological;
    public static final Set<String> Offspring = Set.of("scp049_2", "scp008_zombie", "scp457", "scp031", "scp031_polyp", "scp469_angel", "scp427_1");

    public static final List<List<Threat>> Roster = List.of(
            List.of(new Threat("scp008_zombie", 5, 1), new Threat("scp049_2", 3, 1), new Threat("scp538_shadow_spider", 2, 2), new Threat("scp372", 1, 1),
                    new Threat("scp031", 1, 1), new Threat("scp066", 1, 1), new Threat("scp087_hunter", 1, 1)),
            List.of(new Threat("scp008_zombie", 4, 2), new Threat("scp049_2", 2, 1), new Threat("scp548_ice_spider", 3, 3), new Threat("scp075_snail", 1, 1),
                    new Threat("scp019_drone", 2, 2), new Threat("theodore_amalgamation", 1, 1), new Threat("scp093_torso", 1, 1), new Threat("scp354_crawler", 2, 1),
                    new Threat("scp066", 2, 1), new Threat("scp439_brute", 2, 2), new Threat("scp337", 1, 1), new Threat("scp031", 1, 1),
                    new Threat("scp538_shadow_spider", 2, 2), new Threat("scp173", 1, 1), new Threat("scp352", 1, 1)),
            List.of(new Threat("scp939", 3, 2), new Threat("scp427_1", 2, 1), new Threat("scp352", 3, 1), new Threat("scp323", 1, 1), new Threat("scp058_heart", 1, 1),
                    new Threat("scp354_brute", 2, 1), new Threat("scp354_charger", 1, 1), new Threat("scp008_zombie", 2, 2), new Threat("scp049", 1, 1),
                    new Threat("scp457", 3, 1), new Threat("scp087_hunter", 2, 1), new Threat("scp354_lion", 1, 1), new Threat("scp354_lizard", 1, 1),
                    new Threat("scp407_titan", 1, 1), new Threat("scp469_angel", 1, 1), new Threat("scp337", 1, 1), new Threat("scp173", 2, 1),
                    new Threat("scp096_shy_guy", 1, 1)),
            List.of(new Threat("scp939", 2, 2), new Threat("scp058_heart", 1, 1), new Threat("scp427_1", 1, 1), new Threat("scp354_leaper", 1, 1),
                    new Threat("scp469_angel", 1, 1), new Threat("scp087_hunter", 2, 1), new Threat("scp457", 1, 1), new Threat("scp173", 1, 1),
                    new Threat("scp352", 1, 1)));

    public static final List<List<List<Threat>>> SetPieces = tableOf();

    private Scp1730Breach() {
    }

    public static boolean allowed(String Id) {
        return !Excluded.contains(Id);
    }

    public static boolean biological(String Id) {
        return Scp1730SetPieces.biological(Id);
    }

    public static boolean biological(List<Threat> Piece) {
        for (Threat Each : Piece) {
            if (biological(Each.Id())) {
                return true;
            }
        }
        return false;
    }

    public static List<Threat> halls(int Zone) {
        List<Threat> Found = new ArrayList<>();
        for (Threat Each : roster(Zone)) {
            if (!biological(Each.Id())) {
                Found.add(Each);
            }
        }
        return Found;
    }

    private static List<Threat> threatsOf(Scp1730SetPieces.Piece Piece) {
        List<Threat> Made = new ArrayList<>();
        for (Scp1730SetPieces.Group Each : Piece.Groups()) {
            Made.add(new Threat(Each.Id(), 1, Each.Pack()));
        }
        return List.copyOf(Made);
    }

    private static List<List<List<Threat>>> tableOf() {
        List<List<List<Threat>>> Zones = new ArrayList<>();
        for (List<Scp1730SetPieces.Piece> Options : Scp1730SetPieces.Pieces) {
            List<List<Threat>> Made = new ArrayList<>();
            for (Scp1730SetPieces.Piece Each : Options) {
                Made.add(threatsOf(Each));
            }
            Zones.add(List.copyOf(Made));
        }
        return List.copyOf(Zones);
    }

    @Nullable
    public static EntityType<?> typeOf(String Id) {
        ResourceLocation Key = Scp1730Registry.location(Id);
        return BuiltInRegistries.ENTITY_TYPE.containsKey(Key) ? BuiltInRegistries.ENTITY_TYPE.get(Key) : null;
    }

    public static List<Threat> roster(int Zone) {
        return Roster.get(Math.max(0, Math.min(Roster.size() - 1, Zone)));
    }

    @Nullable
    public static Threat pick(int Zone, RandomSource Random) {
        List<Threat> Table = roster(Zone);
        int Total = 0;
        for (Threat Each : Table) {
            Total += Each.Weight();
        }
        int Roll = Random.nextInt(Math.max(1, Total));
        for (Threat Each : Table) {
            Roll -= Each.Weight();
            if (Roll < 0) {
                return Each;
            }
        }
        return Table.isEmpty() ? null : Table.getFirst();
    }

    public static List<Threat> setPiece(int Zone, int Number, long Seed) {
        return threatsOf(Scp1730SetPieces.piece(Zone, Number, Seed));
    }

    public static String sceneOf(int Zone, int Number, long Seed) {
        return Scp1730SetPieces.scene(Zone, Number, Seed);
    }

    @Nullable
    public static Mob spawnOne(ServerLevel World, String Id, Vec3 At, float Yaw, String Tag) {
        if (!allowed(Id)) {
            return null;
        }
        EntityType<?> Type = typeOf(Id);
        if (Type == null) {
            return null;
        }
        Entity Made = Type.create(World);
        if (!(Made instanceof Mob Creature)) {
            if (Made != null) {
                Made.discard();
            }
            return null;
        }
        Creature.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(At)), MobSpawnType.EVENT, null);
        prepare(Creature);
        Creature.addTag(Tag);
        if (!World.noCollision(Creature)) {
            Creature.discard();
            return null;
        }
        World.addFreshEntity(Creature);
        return Creature;
    }

    public static void prepare(Mob Creature) {
        Creature.setPersistenceRequired();
        if (Creature instanceof Scp457 Burning) {
            Burning.setPrime(false);
        }
    }

    public static List<Mob> spawnSetPiece(ServerLevel World, List<Threat> Kinds, Vec3 At, float Yaw) {
        List<Mob> Made = new ArrayList<>();
        for (Threat Kind : Kinds) {
            Made.addAll(spawnGroup(World, Kind, At, Yaw, SetPieceTag));
        }
        link(Made, BlockPos.containing(At));
        return Made;
    }

    public static void link(List<Mob> Group, BlockPos Home) {
        Scp049Doctor Doctor = null;
        for (Mob Each : Group) {
            if (Each instanceof Scp049Doctor Found) {
                Doctor = Found;
                break;
            }
        }
        for (Mob Each : Group) {
            if (Doctor != null && Each instanceof Scp049Cured Attendant) {
                Attendant.bind(Doctor, null);
            }
            if (Each instanceof Scp439Creature Member) {
                Member.setHome(Home);
            }
        }
    }

    public static boolean adopt(Mob Creature) {
        if (Creature.getTags().contains(RoamingTag) || Creature.getTags().contains(SetPieceTag)) {
            return false;
        }
        ResourceLocation Key = BuiltInRegistries.ENTITY_TYPE.getKey(Creature.getType());
        if (!Key.getNamespace().equals(Scp_unleashed.ModId) || !Offspring.contains(Key.getPath()) || !allowed(Key.getPath())) {
            return false;
        }
        Creature.addTag(RoamingTag);
        return true;
    }

    public static void clearTray(ServerLevel World, Scp049Doctor Doctor) {
        BlockPos Tray = Doctor.trayPos();
        if (Tray == null || !World.isLoaded(Tray)) {
            return;
        }
        BlockState Here = World.getBlockState(Tray);
        if (!Here.is(Scp049Registry.Tray.get())) {
            return;
        }
        Scp1730State State = Scp1730State.get(World.getServer());
        if (State.run() >= 0 && Scp1730World.original(State.run(), Tray, Here)) {
            return;
        }
        if (World.getBlockEntity(Tray) instanceof Scp049TrayBlockEntity Found && Found.owner() != null && !Found.owner().equals(Doctor.getUUID())) {
            return;
        }
        World.removeBlock(Tray, false);
    }

    public static List<Mob> spawnGroup(ServerLevel World, Threat Kind, Vec3 At, float Yaw, String Tag) {
        List<Mob> Made = new ArrayList<>();
        for (int Index = 0; Index < Math.max(1, Kind.Pack()); Index++) {
            Vec3 Spot = Index == 0 ? At : At.add((World.random.nextDouble() - 0.5) * 2.0, 0.0, (World.random.nextDouble() - 0.5) * 2.0);
            Mob One = spawnOne(World, Kind.Id(), Spot, Yaw, Tag);
            if (One == null && Index > 0) {
                One = spawnOne(World, Kind.Id(), At, Yaw, Tag);
            }
            if (One != null) {
                Made.add(One);
            }
        }
        return Made;
    }

    public static int countIn(ServerLevel World, AABB Box, String Tag) {
        return World.getEntitiesOfClass(Mob.class, Box, Each -> Each.isAlive() && Each.getTags().contains(Tag)).size();
    }

    public static AABB boxOf(int Run, Scp1730Zone Zone) {
        BlockPos Low = Scp1730World.world(Run, Zone.MinX(), Zone.MinY(), Zone.MinZ());
        BlockPos High = Scp1730World.world(Run, Zone.MaxX() + 1, Zone.MaxY() + 1, Zone.MaxZ() + 1);
        return new AABB(Vec3.atLowerCornerOf(Low), Vec3.atLowerCornerOf(High));
    }

    public static void tickPlayer(ServerLevel World, ServerPlayer User, int Run, int Zone) {
        if (Zone < 0 || (World.getGameTime() + User.getId() * 7L) % SpawnEvery != 0 || User.isCreative() || User.isSpectator()) {
            return;
        }
        spawnRound(World, User, Run, Zone);
    }

    public static boolean spawnRound(ServerLevel World, ServerPlayer User, int Run, int Zone) {
        if (Zone < 0) {
            return false;
        }
        Scp1730Plan Plan = Scp1730World.plan(Run);
        AABB Box = boxOf(Run, Plan.zone(Zone));
        if (countIn(World, Box, RoamingTag) >= ZoneCaps[Zone] || countIn(World, User.getBoundingBox().inflate(AreaRadius), RoamingTag) >= AreaCaps[Zone]) {
            return false;
        }
        List<Scp1730Fixture> Spots = new ArrayList<>();
        boolean Inside = Scp1730Containment.inWing(Plan, Run, User.position());
        for (Scp1730Fixture Each : Plan.fixtures("spawn", Zone)) {
            Vec3 At = Vec3.atBottomCenterOf(Scp1730World.world(Run, Each));
            double Distance = At.distanceTo(User.position());
            if (Distance >= SpawnNear && Distance <= SpawnFar && World.isPositionEntityTicking(BlockPos.containing(At)) && (Inside || !Plan.inWing(Each.X(), Each.Y(), Each.Z()))) {
                Spots.add(Each);
            }
        }
        if (Spots.isEmpty()) {
            return false;
        }
        Scp1730Fixture Chosen = Spots.get(World.random.nextInt(Spots.size()));
        boolean Wing = Plan.inWing(Chosen.X(), Chosen.Y(), Chosen.Z());
        Threat Kind = Scp1730Containment.pick(Zone, Wing, World.random);
        if (Kind == null) {
            return false;
        }
        int Room = Math.max(0, Math.min(ZoneCaps[Zone] - countIn(World, Box, RoamingTag), AreaCaps[Zone] - countIn(World, User.getBoundingBox().inflate(AreaRadius), RoamingTag)));
        Threat Sized = new Threat(Kind.Id(), Kind.Weight(), Math.max(1, Math.min(Kind.Pack(), Room)));
        List<Mob> Made = spawnGroup(World, Sized, Vec3.atBottomCenterOf(Scp1730World.world(Run, Chosen)), World.random.nextFloat() * 360.0f, RoamingTag);
        if (Wing) {
            for (Mob Each : Made) {
                Scp1730Containment.settle(Run, Plan.wingAt(Chosen.X(), Chosen.Y(), Chosen.Z()), Each);
            }
        }
        return !Made.isEmpty();
    }

    public static void despawn(ServerLevel World) {
        if (World.getGameTime() % DespawnEvery != 0) {
            return;
        }
        Scp1730State State = Scp1730State.get(World.getServer());
        Scp1730Plan Plan = State.active() ? Scp1730World.plan(State.run()) : null;
        List<Mob> Far = new ArrayList<>();
        for (Entity Each : World.getAllEntities()) {
            if (!(Each instanceof Mob Creature)) {
                continue;
            }
            boolean Tracked = Creature.getTags().contains(RoamingTag) || Creature.getTags().contains(SetPieceTag);
            if (Creature.getTags().contains(RoamingTag) && World.getNearestPlayer(Creature, DespawnRange) == null
                    || Tracked && Plan != null && Scp1730Containment.leaked(World, Plan, State.run(), Creature)) {
                Far.add(Creature);
            }
        }
        for (Mob Creature : Far) {
            if (Creature instanceof Scp049Doctor Doctor) {
                clearTray(World, Doctor);
            }
            Creature.discard();
        }
    }

    public static void tickEvents(ServerLevel World, Scp1730State State, int Run, List<ServerPlayer> Inside, int[] ZoneOf) {
        long Now = World.getGameTime();
        Scp1730Plan Plan = Scp1730World.plan(Run);
        for (int Zone = 0; Zone < Scp1730State.DeepZone; Zone++) {
            boolean Occupied = false;
            for (int Index = 0; Index < Inside.size(); Index++) {
                if (ZoneOf[Index] == Zone) {
                    Occupied = true;
                    break;
                }
            }
            if (!Occupied) {
                continue;
            }
            if (State.nextBreach(Zone) == 0L) {
                State.setNextBreach(Zone, Now + FirstBreach + World.random.nextInt(FirstBreach));
                continue;
            }
            if (Now < State.nextBreach(Zone)) {
                continue;
            }
            State.setNextBreach(Zone, Now + MinBreachGap + World.random.nextInt(MaxBreachGap - MinBreachGap));
            breach(World, Run, Plan, Zone, Inside, ZoneOf);
        }
    }

    public static boolean breach(ServerLevel World, int Run, Scp1730Plan Plan, int Zone, List<ServerPlayer> Inside, int[] ZoneOf) {
        Scp1730Fixture Best = null;
        double BestDistance = BreachReach;
        for (Scp1730Fixture Each : Plan.fixtures("breach", Zone)) {
            Vec3 At = Vec3.atBottomCenterOf(Scp1730World.world(Run, Each));
            if (!World.isPositionEntityTicking(BlockPos.containing(At))) {
                continue;
            }
            for (ServerPlayer User : Inside) {
                double Distance = User.position().distanceTo(At);
                if (Distance < BestDistance && Distance > 10.0) {
                    Best = Each;
                    BestDistance = Distance;
                }
            }
        }
        if (Best == null) {
            return false;
        }
        for (int Index = 0; Index < Inside.size(); Index++) {
            if (ZoneOf[Index] == Zone) {
                ServerPlayer User = Inside.get(Index);
                World.playSound(null, User.getX(), User.getY() + 4.0, User.getZ(), Scp1730Registry.Alarm.get(), SoundSource.AMBIENT, 1.4f, 0.9f);
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.breach", Component.translatable("message.scp_unleashed.scp1730.zone." + Zone))
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            }
        }
        Vec3 At = Vec3.atBottomCenterOf(Scp1730World.world(Run, Best));
        int Waves = 2 + World.random.nextInt(2) + Zone;
        List<Mob> Flood = new ArrayList<>();
        for (int Wave = 0; Wave < Waves; Wave++) {
            Threat Kind = Scp1730Containment.pick(Zone, Plan.inWing(Best.X(), Best.Y(), Best.Z()), World.random);
            if (Kind != null) {
                Flood.addAll(spawnGroup(World, new Threat(Kind.Id(), 1, 1), At, Scp1730Plan.yawOf(Best.Facing()), RoamingTag));
            }
        }
        for (Mob Creature : Flood) {
            Player Nearest = World.getNearestPlayer(Creature, BreachReach);
            if (Nearest != null && !Nearest.isCreative()) {
                Creature.setTarget(Nearest);
            }
        }
        return !Flood.isEmpty();
    }
}
