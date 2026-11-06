package net.rai.scp_unleashed.daevite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.entity.DaeviteKnight;
import net.rai.scp_unleashed.daevite.entity.DaeviteMob;
import net.rai.scp_unleashed.faction.FactionBuilder;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionGround;
import net.rai.scp_unleashed.faction.FactionLive;
import net.rai.scp_unleashed.faction.FactionPlan;
import net.rai.scp_unleashed.faction.FactionPlans;
import net.rai.scp_unleashed.faction.FactionRules;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.scp140.api.DaeviteTags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class DaeviteSites {
    public static final ResourceLocation TemplePlan = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_temple");
    public static final ResourceLocation[] CampPlans = {ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_camp"),
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_camp_ridge")};
    public static final int TempleMin = 260;
    public static final int TempleMax = 420;
    public static final int CampMin = 110;
    public static final int CampMax = 220;
    public static final int SiteSpacing = 96;
    public static final int StrictTries = 4;
    public static final int LenientTries = 10;
    public static final int SpreadBase = 12;
    public static final int SpreadStep = 8;
    public static final int MaxSpread = 72;
    public static final int CampSearch = 72;
    public static final int RaidMin = 26;
    public static final int RaidMax = 42;
    public static final long RaidPatrol = 3000L;
    public static final String FaceX = "FaceX";
    public static final String FaceZ = "FaceZ";

    private DaeviteSites() {
    }

    public static BlockPos overworldOrigin(ServerLevel Book, BlockPos Origin) {
        if (Book.dimension() == Level.OVERWORLD) {
            return Origin;
        }
        if (Book.dimension() == Level.NETHER) {
            return new BlockPos(Origin.getX() * 8, Book.getServer().overworld().getSeaLevel(), Origin.getZ() * 8);
        }
        return Book.getServer().overworld().getSharedSpawnPos();
    }

    @Nullable
    public static FactionSite templeSite(FactionState State) {
        return State.firstSite(Site -> Site.is(DaeviteFaction.Temple)).orElse(null);
    }

    public static boolean farFromSites(FactionState State, BlockPos Pos, int Spacing) {
        for (FactionSite Site : State.sites()) {
            BlockPos At = Site.anchor();
            double Dx = At.getX() - Pos.getX();
            double Dz = At.getZ() - Pos.getZ();
            if (Dx * Dx + Dz * Dz < (double) Spacing * Spacing) {
                return false;
            }
        }
        return true;
    }

    private static void face(FactionSite Site, BlockPos Toward) {
        Site.extra().putInt(FaceX, Toward.getX());
        Site.extra().putInt(FaceZ, Toward.getZ());
    }

    public static FactionSite planTemple(ServerLevel Overworld, FactionState State, BlockPos Origin) {
        FactionSite Existing = templeSite(State);
        if (Existing != null) {
            return Existing;
        }
        RandomSource Random = Overworld.random;
        BlockPos Target = FactionGround.preset(Overworld, Origin, TempleMin, TempleMax, Random, Pos -> farFromSites(State, Pos, SiteSpacing));
        FactionSite Site = State.addSite(DaeviteFaction.Temple, Level.OVERWORLD, Target, Random.nextLong());
        face(Site, Origin);
        State.changed();
        return Site;
    }

    @Nullable
    public static FactionSite planCamp(MinecraftServer Server, FactionState State, @Nullable BlockPos Toward) {
        if (State.sites().size() >= DaeviteFaction.Instance.rules().SiteCap()) {
            return null;
        }
        ServerLevel World = Server.overworld();
        RandomSource Random = World.random;
        FactionSite Temple = templeSite(State);
        BlockPos Base;
        if (Temple != null && Toward != null && Temple.anchor().distSqr(Toward) > 64.0 * 64.0) {
            double Share = 0.4 + Random.nextDouble() * 0.25;
            double Dx = Toward.getX() - Temple.anchor().getX();
            double Dz = Toward.getZ() - Temple.anchor().getZ();
            double Length = Math.sqrt(Dx * Dx + Dz * Dz);
            double Side = (Random.nextBoolean() ? 1.0 : -1.0) * (24.0 + Random.nextDouble() * 56.0);
            int X = Mth.floor(Temple.anchor().getX() + Dx * Share - Dz / Length * Side);
            int Z = Mth.floor(Temple.anchor().getZ() + Dz * Share + Dx / Length * Side);
            Base = new BlockPos(X, World.getSeaLevel(), Z);
        } else {
            BlockPos Around = Temple != null ? Temple.anchor() : Toward != null ? Toward : World.getSharedSpawnPos();
            List<FactionSite> Built = State.sites().stream().filter(FactionSite::built).toList();
            if (!Built.isEmpty()) {
                Around = Built.get(Random.nextInt(Built.size())).anchor();
            }
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Distance = CampMin + Random.nextDouble() * (CampMax - CampMin);
            Base = new BlockPos(Around.getX() + Mth.floor(Math.cos(Angle) * Distance), World.getSeaLevel(), Around.getZ() + Mth.floor(Math.sin(Angle) * Distance));
        }
        BlockPos Target = unloadedNear(World, Base, Random, Pos -> farFromSites(State, Pos, SiteSpacing));
        FactionSite Site = State.addSite(DaeviteFaction.Camp, Level.OVERWORLD, Target, Random.nextLong());
        face(Site, Toward != null ? Toward : Temple != null ? Temple.anchor() : Base);
        State.changed();
        return Site;
    }

    private static BlockPos unloadedNear(ServerLevel World, BlockPos Base, RandomSource Random, Predicate<BlockPos> Accept) {
        WorldBorder Border = World.getWorldBorder();
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < 64; Attempt++) {
            double Radius = Attempt == 0 ? 0.0 : 8.0 + Attempt * (CampSearch / 32.0);
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            int X = Base.getX() + Mth.floor(Math.cos(Angle) * Radius);
            int Z = Base.getZ() + Mth.floor(Math.sin(Angle) * Radius);
            if (!Border.isWithinBounds(X, Z) || Border.getDistanceToBorder(X, Z) < FactionGround.BorderMargin) {
                continue;
            }
            BlockPos Candidate = new BlockPos(X, World.getSeaLevel(), Z);
            if (World.getChunkSource().hasChunk(X >> 4, Z >> 4) || !Accept.test(Candidate)) {
                continue;
            }
            if (Fallback == null) {
                Fallback = Candidate;
            }
            if (FactionGround.isDryLand(World, X, Z)) {
                return Candidate;
            }
        }
        return Fallback != null ? Fallback : Base;
    }

    public static ResourceLocation planFor(FactionSite Site) {
        if (Site.is(DaeviteFaction.Temple)) {
            return TemplePlan;
        }
        return CampPlans[Math.floorMod(Site.seed(), CampPlans.length)];
    }

    public static boolean build(ServerLevel World, FactionState State, FactionSite Site) {
        FactionPlan Plan = FactionPlans.get(World.getServer(), planFor(Site));
        if (Plan == null) {
            return false;
        }
        boolean Temple = Site.is(DaeviteFaction.Temple);
        int Tries = Site.tries();
        int Mode = Tries < StrictTries ? FactionGround.Strict : Tries < LenientTries ? FactionGround.Lenient : FactionGround.Forced;
        int Spread = Math.min(MaxSpread, SpreadBase + Tries * SpreadStep);
        FactionGround.Footprint Print = Plan.footprint(Temple ? 6 : 4, Temple ? 12 : 9);
        Vec3 Face = Site.extra().contains(FaceX) ? new Vec3(Site.extra().getInt(FaceX), 0.0, Site.extra().getInt(FaceZ)) : null;
        Optional<FactionGround.Placement> Found = FactionGround.find(World, Site.anchor(), Spread, Print, Mode, RandomSource.create(Site.seed() + Tries), Face,
                State0 -> State0.is(DaeviteTags.Decor), Pos -> farFromOtherBuilt(State, Site, Pos));
        if (Found.isEmpty()) {
            return false;
        }
        FactionGround.Placement Place = Found.get();
        Map<String, List<BlockPos>> Marks = FactionBuilder.place(World, Plan, Place.Origin(), Place.Front(), Site.seed());
        Site.markBuilt(Place.Origin(), Place.Front(), Marks);
        return true;
    }

    public static boolean buildNow(ServerLevel World, FactionSite Site, BlockPos Near, Vec3 Face) {
        FactionPlan Plan = FactionPlans.get(World.getServer(), planFor(Site));
        if (Plan == null) {
            return false;
        }
        FactionGround.Footprint Print = Plan.footprint(64, 64);
        Optional<FactionGround.Placement> Found = FactionGround.find(World, Near, 12, Print, FactionGround.Forced, RandomSource.create(Site.seed()), Face,
                State0 -> State0.is(DaeviteTags.Decor), Pos -> true);
        FactionGround.Placement Place = Found.orElseGet(() -> new FactionGround.Placement(
                new BlockPos(Near.getX(), FactionGround.ground(World, Near.getX(), Near.getZ()), Near.getZ()), FactionGround.facing(Near, Face), 0));
        Map<String, List<BlockPos>> Marks = FactionBuilder.place(World, Plan, Place.Origin(), Place.Front(), Site.seed());
        Site.markBuilt(Place.Origin(), Place.Front(), Marks);
        return true;
    }

    private static boolean farFromOtherBuilt(FactionState State, FactionSite Self, BlockPos Pos) {
        for (FactionSite Site : State.sites()) {
            if (Site == Self || !Site.built()) {
                continue;
            }
            if (Site.anchor().distSqr(Pos) < 64.0 * 64.0) {
                return false;
            }
        }
        return true;
    }

    public static int searchRadius(FactionSite Site) {
        int Reach = Site.is(DaeviteFaction.Temple) ? 24 : 14;
        return Reach + Math.min(MaxSpread, SpreadBase + Site.tries() * SpreadStep);
    }

    public static int knightCap(FactionSite Site) {
        return Site.is(DaeviteFaction.Temple) ? DaeviteFaction.TempleKnights : DaeviteFaction.CampKnights;
    }

    public static int overseerCap(FactionSite Site) {
        return Site.is(DaeviteFaction.Temple) ? DaeviteFaction.TempleOverseers : DaeviteFaction.CampOverseers;
    }

    public static void garrison(ServerLevel World, FactionState State, FactionSite Site) {
        boolean Temple = Site.is(DaeviteFaction.Temple);
        int Knights = Temple ? DaeviteFaction.TempleStartKnights : DaeviteFaction.CampStartKnights;
        int Overseers = Temple ? DaeviteFaction.TempleStartOverseers : DaeviteFaction.CampStartOverseers;
        if (World.getDifficulty() == Difficulty.HARD) {
            Knights++;
        }
        int Room = Math.max(0, FactionRules.GlobalLoadedCap - FactionLive.total());
        int Owed = 0;
        for (int Index = 0; Index < Knights + Overseers; Index++) {
            String Role = Index < Knights ? DaeviteFaction.RoleKnight : DaeviteFaction.RoleOverseer;
            if (Room <= 0) {
                Owed++;
                continue;
            }
            BlockPos At = spawnPoint(World, Site, Role);
            if (At != null && spawnMember(World, State, Site, Role, At, false) != null) {
                Room--;
            } else {
                Owed++;
            }
        }
        if (Owed > 0) {
            Site.setReserve(Site.reserve() + Owed);
            State.changed();
        }
    }

    @Nullable
    public static DaeviteMob recruit(ServerLevel World, FactionState State, FactionSite Site) {
        int Knights = State.rosterAt(Site.id(), DaeviteFaction.RoleKnight);
        int Overseers = State.rosterAt(Site.id(), DaeviteFaction.RoleOverseer);
        boolean KnightRoom = Knights < knightCap(Site);
        boolean OverseerRoom = Overseers < overseerCap(Site);
        if (!KnightRoom && !OverseerRoom) {
            return null;
        }
        String Role = OverseerRoom && (!KnightRoom || World.random.nextFloat() < 0.35f) ? DaeviteFaction.RoleOverseer : DaeviteFaction.RoleKnight;
        BlockPos At = spawnPoint(World, Site, Role);
        return At == null ? null : spawnMember(World, State, Site, Role, At, true);
    }

    @Nullable
    public static BlockPos spawnPoint(ServerLevel World, FactionSite Site, String Role) {
        List<BlockPos> Options = new ArrayList<>(Site.marks(Role));
        if (Options.isEmpty()) {
            Options.addAll(Site.marks(DaeviteFaction.RoleKnight));
        }
        if (Options.isEmpty()) {
            Options.add(Site.anchor().above());
        }
        for (int Attempt = 0; Attempt < Options.size() * 2; Attempt++) {
            BlockPos At = Options.get(World.random.nextInt(Options.size()));
            if (World.isLoaded(At) && free(World, At)) {
                return At;
            }
        }
        return null;
    }

    private static boolean free(ServerLevel World, BlockPos At) {
        if (!World.getBlockState(At).getCollisionShape(World, At).isEmpty() || !World.getBlockState(At.above()).getCollisionShape(World, At.above()).isEmpty()) {
            return false;
        }
        return World.getEntitiesOfClass(DaeviteMob.class, new AABB(At).inflate(0.6, 1.0, 0.6)).isEmpty();
    }

    @Nullable
    public static DaeviteMob spawnMember(ServerLevel World, FactionState State, @Nullable FactionSite Site, String Role, BlockPos At, boolean Arrival) {
        boolean Seer = DaeviteFaction.RoleOverseer.equals(Role);
        DaeviteMob Member = Seer ? DaeviteRegistry.Overseer.get().create(World) : DaeviteRegistry.Knight.get().create(World);
        if (Member == null) {
            return null;
        }
        Member.moveTo(At.getX() + 0.5, At.getY(), At.getZ() + 0.5, World.random.nextFloat() * 360.0f, 0.0f);
        Member.finalizeSpawn(World, World.getCurrentDifficultyAt(At), MobSpawnType.EVENT, null);
        Member.setHome(At);
        FactionControl.enlist(DaeviteFaction.Instance, State, Member, Site == null ? -1 : Site.id(), Role);
        World.addFreshEntity(Member);
        if (Arrival) {
            arrival(World, Vec3.atBottomCenterOf(At));
        }
        return Member;
    }

    public static DaeviteKnight knightAt(ServerLevel World, FactionState State, @Nullable FactionSite Site, BlockPos At, byte Loadout) {
        DaeviteKnight Knight = DaeviteRegistry.Knight.get().create(World);
        if (Knight == null) {
            return null;
        }
        Knight.setLoadout(Loadout);
        Knight.moveTo(At.getX() + 0.5, At.getY(), At.getZ() + 0.5, World.random.nextFloat() * 360.0f, 0.0f);
        Knight.finalizeSpawn(World, World.getCurrentDifficultyAt(At), MobSpawnType.EVENT, null);
        Knight.setHome(At);
        FactionControl.enlist(DaeviteFaction.Instance, State, Knight, Site == null ? -1 : Site.id(), DaeviteFaction.RoleKnight);
        World.addFreshEntity(Knight);
        return Knight;
    }

    public static void arrival(ServerLevel World, Vec3 At) {
        World.sendParticles(DaeviteCollapse.Cinder, At.x, At.y + 1.0, At.z, 30, 0.35, 0.8, 0.35, 0.04);
        World.sendParticles(ParticleTypes.FLAME, At.x, At.y + 0.1, At.z, 12, 0.5, 0.05, 0.5, 0.02);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + 0.4, At.z, 8, 0.35, 0.3, 0.35, 0.01);
        World.playSound(null, At.x, At.y, At.z, DaeviteRegistry.Arrive.get(), SoundSource.HOSTILE, 1.0f, 0.85f + World.random.nextFloat() * 0.2f);
    }

    public static boolean warParty(ServerLevel World, FactionState State, ServerPlayer Target, FactionSite From) {
        if (FactionLive.total() > FactionRules.GlobalLoadedCap - 3 || State.roster().size() >= DaeviteFaction.Instance.rules().RosterCap() - 3) {
            return false;
        }
        BlockPos Spot = null;
        for (int Attempt = 0; Attempt < 6 && Spot == null; Attempt++) {
            BlockPos Candidate = FactionGround.standable(World, Target.blockPosition(), RaidMin, RaidMax, World.random, 12);
            if (Candidate == null) {
                continue;
            }
            Vec3 Eye = Target.getEyePosition();
            Vec3 Top = Vec3.atCenterOf(Candidate.above());
            boolean Hidden = World.clip(new ClipContext(Eye, Top, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Target)).getType() != HitResult.Type.MISS;
            if (Hidden || Attempt >= 4) {
                Spot = Candidate;
            }
        }
        if (Spot == null) {
            return false;
        }
        int Knights = World.getDifficulty() == Difficulty.HARD ? 3 : 2;
        boolean Seer = World.random.nextBoolean();
        long Until = World.getGameTime() + RaidPatrol;
        for (int Index = 0; Index < Knights + (Seer ? 1 : 0); Index++) {
            BlockPos At = FactionGround.standable(World, Spot, 0, 3, World.random, 8);
            if (At == null) {
                At = Spot;
            }
            DaeviteMob Member = spawnMember(World, State, From, Index < Knights ? DaeviteFaction.RoleKnight : DaeviteFaction.RoleOverseer, At, true);
            if (Member != null) {
                Member.setPatrol(Target.position(), Until);
                BlockPos Gate = From.mark("gate");
                Member.setHome(Gate != null ? Gate : From.anchor());
            }
        }
        World.playSound(null, Spot.getX(), Spot.getY(), Spot.getZ(), DaeviteRegistry.Horn.get(), SoundSource.HOSTILE, 4.0f, 0.9f);
        return true;
    }
}
