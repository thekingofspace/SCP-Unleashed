package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletCourtHook;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ScarletCourt implements ScarletCourtHook {
    public static final ScarletCourt Instance = new ScarletCourt();
    public static final int CrackTick = 0;
    public static final int SnapTick = 20;
    public static final int StingTick = 40;
    public static final int LostGrace = 60;
    public static final int[] DaughterPillars = {0, 2, 4, 6};
    public static final int EntranceSteps = 6;
    public static final String[] DaughterIds = {"scarlet_daughter_thorns", "scarlet_daughter_bride", "scarlet_daughter_eyes", "scarlet_daughter_moon"};
    public static final ResourceLocation CultistId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scarlet_cultist");

    public enum Stage { Idle, Opening, Entrance, Fighting, Breather, King, Finished }

    private static boolean Fast;
    @Nullable
    private ServerLevel World;
    @Nullable
    private LivingEntity King;
    private BlockPos Dais = BlockPos.ZERO;
    private int Players = 1;
    private Stage Current = Stage.Idle;
    private int Timer;
    private int WaveTimer;
    private int Slain;
    private boolean Active;
    private int Turn;
    private int Lost;
    private long Driven = Long.MIN_VALUE / 2;
    private final Set<UUID> Minions = new LinkedHashSet<>();
    private final Set<UUID> Others = new LinkedHashSet<>();
    private final Set<UUID> Daughters = new LinkedHashSet<>();
    private final Set<Integer> KingWaves = new HashSet<>();

    private ScarletCourt() {
    }

    public static void fastForTests(boolean Value) {
        Fast = Value;
    }

    private static int opening() {
        return Fast ? 10 : ScarletCourtConfig.openingTicks();
    }

    private static int breather() {
        return Fast ? 20 : ScarletCourtConfig.breatherTicks();
    }

    private static int waveEvery() {
        return Fast ? 60 : ScarletCourtConfig.waveTicks();
    }

    private static int kingEvery() {
        return Fast ? 80 : ScarletCourtConfig.kingWaveTicks();
    }

    public static int cap(int Count) {
        int Solo = ScarletCourtConfig.capSolo();
        return Mth.clamp(Solo + ScarletCourtConfig.capPerPlayer() * Math.max(0, Count - 1), Solo, Math.max(Solo, ScarletCourtConfig.capMax()));
    }

    public static int scaled(int Count, int Players) {
        return (int) Math.round(Count * (1.0 + ScarletCourtConfig.extraPlayerShare() * Math.max(0, Players - 1)));
    }

    public Stage stage() {
        return Current;
    }

    public boolean active() {
        return Active;
    }

    public BlockPos dais() {
        return Dais;
    }

    public int slain() {
        return Slain;
    }

    public int timer() {
        return Timer;
    }

    @Override
    public void begin(ServerLevel Here, BlockPos Centre, LivingEntity Lord, int Count) {
        World = Here;
        King = Lord;
        Dais = Centre.immutable();
        Players = Math.max(1, Count);
        ScarletCourtState State = ScarletCourtState.get(Here.getServer());
        if (State.slain() >= ScarletCourtState.Daughters && !State.active() && Current == Stage.Finished) {
            State.slain(0);
        }
        Slain = State.slain();
        despawnAll(Here);
        Active = true;
        State.active(true);
        State.dais(Dais);
        State.players(Players);
        Turn = 0;
        Lost = 0;
        KingWaves.clear();
        if (Slain >= ScarletCourtState.Daughters) {
            Current = Stage.King;
            Timer = 0;
            WaveTimer = kingEvery() / 2;
        } else {
            Current = Stage.Opening;
            Timer = 0;
            WaveTimer = waveEvery();
        }
        wave(Here, "opening");
    }

    private void resume(ServerLevel Here, @Nullable LivingEntity Lord) {
        ScarletCourtState State = ScarletCourtState.get(Here.getServer());
        World = Here;
        King = Lord;
        Dais = State.dais();
        Players = State.players();
        Slain = State.slain();
        Active = true;
        Minions.clear();
        Others.clear();
        Daughters.clear();
        for (Entity Each : Here.getEntitiesOfClass(Entity.class, CourtArena.area(Dais), Found -> Found.getTags().contains(CourtFx.Tag))) {
            if (Each instanceof CourtDaughter Daughter && !Daughter.dying()) {
                Daughters.add(Each.getUUID());
            } else if (Each instanceof CourtMinion || Each.getType() == cultistType().orElse(null)) {
                Minions.add(Each.getUUID());
            } else {
                Others.add(Each.getUUID());
            }
        }
        if (Slain >= ScarletCourtState.Daughters) {
            Current = Stage.King;
            WaveTimer = kingEvery() / 2;
        } else if (!Daughters.isEmpty()) {
            Current = Stage.Fighting;
            WaveTimer = waveEvery() / 2;
        } else {
            Current = Stage.Breather;
            Timer = 0;
        }
    }

    @Override
    public void tick(ServerLevel Here, LivingEntity Lord) {
        Driven = Here.getGameTime();
        step(Here, Lord);
    }

    public void idle(ServerLevel Here) {
        if (Active && World == Here && Here.getGameTime() - Driven > 1) {
            step(Here, King);
        }
    }

    private void step(ServerLevel Here, @Nullable LivingEntity Lord) {
        if (!Active) {
            if (Current == Stage.Idle && ScarletCourtState.get(Here.getServer()).active()) {
                resume(Here, Lord);
            } else {
                return;
            }
        }
        World = Here;
        if (Lord != null) {
            King = Lord;
        }
        Timer++;
        prune(Here);
        switch (Current) {
            case Opening -> {
                if (Timer >= opening()) {
                    if (Slain < ScarletCourtState.Daughters) {
                        startEntrance();
                    } else {
                        toKing();
                    }
                }
            }
            case Entrance -> entrance(Here);
            case Fighting -> {
                if (--WaveTimer <= 0) {
                    WaveTimer = waveEvery();
                    wave(Here, CourtWaves.forDaughter(Slain, Turn++));
                }
                if (Daughters.isEmpty()) {
                    if (++Lost > LostGrace) {
                        startEntrance();
                    }
                } else {
                    Lost = 0;
                }
            }
            case Breather -> {
                if (Timer >= breather()) {
                    if (Slain < ScarletCourtState.Daughters) {
                        startEntrance();
                    } else {
                        toKing();
                    }
                }
            }
            case King -> {
                if (--WaveTimer <= 0) {
                    WaveTimer = kingEvery();
                    wave(Here, CourtWaves.forKing(Turn++));
                }
            }
            default -> {
            }
        }
    }

    private void toKing() {
        Current = Stage.King;
        Timer = 0;
        WaveTimer = kingEvery() / 2;
    }

    private void startEntrance() {
        Current = Stage.Entrance;
        Timer = 0;
        Lost = 0;
    }

    private void entrance(ServerLevel Here) {
        int Index = Math.min(Slain, ScarletCourtState.Daughters - 1);
        int Pillar = DaughterPillars[Index];
        BlockPos Base = CourtArena.pillarBase(Dais, Pillar);
        BlockPos Top = CourtArena.pillarTop(Dais, Pillar);
        if (Timer - 1 == CrackTick) {
            for (int Lift = 0; Lift <= CourtArena.pillarHeight(); Lift += 2) {
                BlockPos At = Base.above(Lift);
                CourtFx.crumble(Here, CourtFx.solidAt(Here, At), Vec3.atCenterOf(At), 10, 0.8, 0.8);
            }
            CourtFx.sound(Here, Vec3.atCenterOf(Base.above(CourtArena.pillarHeight() / 2)), "pillar_crack", 6.0f, 0.8f);
        }
        if (Timer - 1 == SnapTick) {
            Vec3 From = King != null ? King.position().add(0.0, King.getBbHeight() * 0.6, 0.0) : Vec3.atCenterOf(Dais).add(0.0, 10.0, 0.0);
            Vec3 To = CourtArena.overridden() ? Vec3.atCenterOf(Top) : ScarletThrone.anchorPoint(Here, Dais.below(ScarletThrone.DaisHeight), Pillar);
            CourtFx.line(Here, CourtFx.Binding, From, To, 0.6, 0.05);
            CourtFx.line(Here, ParticleTypes.END_ROD, From, To, 1.5, 0.1);
            CourtFx.sound(Here, From.add(To).scale(0.5), "band_snap", 6.0f, 1.0f);
            CourtFx.sound(Here, To, "band_snap", 4.0f, 0.8f);
        }
        if (Timer - 1 == StingTick) {
            BlockPos Spot = CourtArena.groundOr(Here, CourtArena.inward(Dais, Pillar, EntranceSteps), Dais);
            CourtDaughter Made = spawnDaughter(Here, Index, Spot);
            for (ServerPlayer Each : players(Here)) {
                Each.playNotifySound(ScarletCourtSounds.get("sting"), net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f);
                title(Each, Component.translatable("title.scp_unleashed.scarlet_court.rises." + Index).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            }
            CourtFx.sound(Here, Vec3.atCenterOf(Spot), "sting", 8.0f, 1.0f);
            Current = Stage.Fighting;
            Timer = 0;
            WaveTimer = waveEvery() / 2;
            Lost = Made == null ? LostGrace - 20 : 0;
        }
    }

    @Nullable
    public CourtDaughter spawnDaughter(ServerLevel Here, int Index, BlockPos Spot) {
        EntityType<? extends CourtDaughter> Type = ScarletCourtRegistry.daughterType(Index);
        CourtDaughter Made = Type.create(Here);
        if (Made == null) {
            return null;
        }
        if (!CourtKeeper.free(Here, Dais, Vec3.atBottomCenterOf(Spot), Type.getWidth(), Type.getHeight())) {
            Spot = CourtKeeper.clearBlock(Here, Dais, Spot, Type.getWidth(), Type.getHeight());
        }
        Vec3 Centre = Vec3.atBottomCenterOf(Dais);
        float Yaw = (float) (Mth.atan2(Centre.z - (Spot.getZ() + 0.5), Centre.x - (Spot.getX() + 0.5)) * Mth.RAD_TO_DEG) - 90.0f;
        Made.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, Yaw, 0.0f);
        Made.setYHeadRot(Yaw);
        Made.setYBodyRot(Yaw);
        Made.addTag(CourtFx.Tag);
        Made.setup(players(), Dais);
        Made.beginEntrance();
        Here.addFreshEntity(Made);
        Daughters.add(Made.getUUID());
        return Made;
    }

    public void addDaughter(CourtDaughter Daughter) {
        Daughters.add(Daughter.getUUID());
    }

    public void daughterFallen(CourtDaughter Daughter) {
        if (!(Daughter.level() instanceof ServerLevel Here)) {
            return;
        }
        boolean Tracked = Daughters.remove(Daughter.getUUID());
        for (UUID Id : Daughters) {
            if (Here.getEntity(Id) instanceof CourtDaughter Other && Other.isAlive() && !Other.dying()) {
                return;
            }
        }
        Vec3 At = Daughter.position().add(0.0, 1.0, 0.0);
        if (!Tracked || !Active) {
            return;
        }
        Daughters.clear();
        int Index = Daughter.index();
        Slain = Math.min(ScarletCourtState.Daughters, Math.max(Slain, Index) + 1);
        ScarletCourtState.get(Here.getServer()).slain(Slain);
        for (ServerPlayer Each : players(Here)) {
            title(Each, Component.translatable("title.scp_unleashed.scarlet_court.falls." + Index).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        CourtFx.sound(Here, At, "breather", 6.0f, 1.0f);
        if (Slain >= ScarletCourtState.Daughters) {
            toKing();
        } else {
            Current = Stage.Breather;
            Timer = 0;
            wave(Here, "light");
        }
    }

    @Override
    public int daughtersRemaining() {
        return Math.max(0, ScarletCourtState.Daughters - Slain);
    }

    @Override
    public boolean daughterActive() {
        return Active && (Current == Stage.Entrance || Current == Stage.Fighting);
    }

    @Override
    public void kingPhase(int Phase) {
        if (!Active || World == null || KingWaves.contains(Phase)) {
            return;
        }
        KingWaves.add(Phase);
        wave(World, CourtWaves.forKingPhase(Phase));
        WaveTimer = kingEvery();
    }

    @Override
    public void reset(ServerLevel Here) {
        despawnAll(Here);
        Active = false;
        Current = Stage.Idle;
        Timer = 0;
        ScarletCourtState State = ScarletCourtState.get(Here.getServer());
        State.active(false);
        Slain = State.slain();
    }

    @Override
    public void end(ServerLevel Here) {
        despawnAll(Here);
        Active = false;
        Current = Stage.Finished;
        Timer = 0;
        Slain = 0;
        ScarletCourtState State = ScarletCourtState.get(Here.getServer());
        State.active(false);
        State.slain(0);
    }

    public void forget() {
        World = null;
        King = null;
        Active = false;
        Current = Stage.Idle;
        Slain = 0;
        Minions.clear();
        Others.clear();
        Daughters.clear();
        KingWaves.clear();
    }

    public void adopt(Entity Each) {
        if (Each instanceof CourtMinion) {
            Minions.add(Each.getUUID());
        } else if (Each instanceof CourtDaughter Daughter && !Daughter.dying()) {
            Daughters.add(Each.getUUID());
        } else {
            Others.add(Each.getUUID());
        }
    }

    @Nullable
    public CourtDaughter currentDaughter(ServerLevel Here) {
        for (UUID Id : Daughters) {
            if (Here.getEntity(Id) instanceof CourtDaughter Found && Found.isAlive()) {
                return Found;
            }
        }
        return null;
    }

    public List<CourtDaughter> daughters(ServerLevel Here) {
        List<CourtDaughter> Found = new ArrayList<>();
        for (UUID Id : Daughters) {
            if (Here.getEntity(Id) instanceof CourtDaughter Each && Each.isAlive()) {
                Found.add(Each);
            }
        }
        return Found;
    }

    private void prune(ServerLevel Here) {
        Minions.removeIf(Id -> !(Here.getEntity(Id) instanceof LivingEntity Living) || !Living.isAlive());
        Others.removeIf(Id -> Here.getEntity(Id) == null);
        Daughters.removeIf(Id -> !(Here.getEntity(Id) instanceof CourtDaughter Each) || Each.isRemoved());
    }

    public int living(ServerLevel Here) {
        prune(Here);
        return Minions.size();
    }

    public int players() {
        if (World == null) {
            return Players;
        }
        int Count = players(World).size();
        return Math.max(1, Count > 0 ? Count : Players);
    }

    public List<ServerPlayer> players(ServerLevel Here) {
        double Range = CourtArena.playerRange();
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer Each : Here.players()) {
            if (Each.isAlive() && !Each.isSpectator() && Each.distanceToSqr(Vec3.atCenterOf(Dais)) <= Range * Range) {
                Found.add(Each);
            }
        }
        return Found;
    }

    private static void title(ServerPlayer Each, Component Line) {
        Each.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        Each.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
        Each.connection.send(new ClientboundSetSubtitleTextPacket(Line));
    }

    public static Optional<EntityType<?>> cultistType() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(CultistId);
    }

    public int wave(ServerLevel Here, String Name) {
        List<CourtWaves.Entry> Mix = CourtWaves.Mixes.get(Name);
        if (Mix == null) {
            return 0;
        }
        int Count = players();
        int Room = cap(Count) - living(Here);
        int Made = 0;
        Vec3 Horn = null;
        for (CourtWaves.Entry Each : Mix) {
            int Wanted = scaled(Each.Count(), Count);
            for (int Index = 0; Index < Wanted && Room > 0; Index++) {
                BlockPos Spot = spot(Here, Each.From());
                Entity Spawned = spawnOne(Here, Each.What(), Spot);
                if (Spawned != null) {
                    Room--;
                    Made++;
                    if (Horn == null) {
                        Horn = Spawned.position();
                    }
                }
            }
        }
        if (Made > 0) {
            CourtFx.sound(Here, Vec3.atCenterOf(Dais).add(0.0, 6.0, 0.0), "wave", 8.0f, 1.0f);
            if (Horn != null) {
                CourtFx.sound(Here, Horn, "wave", 3.0f, 0.8f);
            }
        }
        return Made;
    }

    private int nearestPillar(BlockPos Pos) {
        int Nearest = 0;
        double Best = Double.MAX_VALUE;
        for (int Index = 0; Index < CourtArena.Pillars; Index++) {
            double Distance = CourtArena.pillarBase(Dais, Index).distSqr(new BlockPos(Pos.getX(), Dais.getY(), Pos.getZ()));
            if (Distance < Best) {
                Best = Distance;
                Nearest = Index;
            }
        }
        return Nearest;
    }

    private BlockPos spot(ServerLevel Here, CourtWaves.Source From) {
        var Random = Here.random;
        return switch (From) {
            case Rim -> CourtArena.scatter(Here, Dais, CourtArena.rimRadius() - 2.0, CourtArena.rimRadius(), Random);
            case PillarBase -> CourtArena.groundOr(Here, CourtArena.inward(Dais, Random.nextInt(CourtArena.Pillars), EntranceSteps), Dais);
            case PillarTop -> {
                int Pillar = Random.nextInt(CourtArena.Pillars);
                BlockPos Top = CourtArena.ground(Here, CourtArena.pillarTop(Dais, Pillar), 2);
                yield Top != null ? Top : CourtArena.groundOr(Here, CourtArena.inward(Dais, Pillar, EntranceSteps), Dais);
            }
            case Near -> CourtArena.scatter(Here, Dais, Math.min(8.0, CourtArena.floorRadius() * 0.4), Math.max(9.0, CourtArena.floorRadius() * 0.55), Random);
        };
    }

    @Nullable
    public Entity spawnOne(ServerLevel Here, String What, BlockPos Spot) {
        if (CourtWaves.Cultist.equals(What) || CourtWaves.Leader.equals(What)) {
            return spawnCultist(Here, Spot, CourtWaves.Leader.equals(What));
        }
        CourtKind Kind = CourtKind.valueOf(What);
        return spawnMinion(Here, Kind, Spot);
    }

    @Nullable
    public CourtMinion summon(ServerLevel Here, CourtKind Kind, BlockPos Spot, boolean RespectCap) {
        if (RespectCap && Active && living(Here) >= cap(players())) {
            return null;
        }
        return spawnMinion(Here, Kind, Spot);
    }

    @Nullable
    public CourtMinion spawnMinion(ServerLevel Here, CourtKind Kind, BlockPos Spot) {
        CourtMinion Made = ScarletCourtRegistry.minionType(Kind).create(Here);
        if (Made == null) {
            return null;
        }
        Vec3 Feet = Vec3.atBottomCenterOf(Spot);
        boolean Perched = Spot.getY() > Dais.getY() + 3;
        if (Perched ? !CourtKeeper.fits(Here, Feet, Kind.width(), Kind.height()) : !CourtKeeper.free(Here, Dais, Feet, Kind.width(), Kind.height())) {
            Spot = CourtKeeper.clearBlock(Here, Dais, Perched ? CourtArena.inward(Dais, nearestPillar(Spot), EntranceSteps) : Spot, Kind.width(), Kind.height());
        }
        Made.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, Here.random.nextFloat() * 360.0f, 0.0f);
        Made.addTag(CourtFx.Tag);
        Made.setPersistenceRequired();
        Made.rise(CourtMinion.SpawnTicks);
        Player Nearest = Here.getNearestPlayer(Made, 64.0);
        if (Nearest != null && CourtFx.validTarget(Nearest)) {
            Made.setTarget(Nearest);
        }
        Here.addFreshEntity(Made);
        Minions.add(Made.getUUID());
        puff(Here, Made.position(), Made.getBbHeight());
        return Made;
    }

    @Nullable
    public Entity spawnCultist(ServerLevel Here, BlockPos Spot, boolean Lead) {
        Optional<EntityType<?>> Type = cultistType();
        if (Type.isEmpty()) {
            return null;
        }
        Entity Made;
        if (Lead) {
            CompoundTag Tag = new CompoundTag();
            Tag.putString("id", CultistId.toString());
            Tag.putBoolean("Leader", true);
            Made = EntityType.loadEntityRecursive(Tag, Here, Loaded -> Loaded);
        } else {
            Made = Type.get().create(Here);
        }
        if (Made == null) {
            return null;
        }
        Made.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, Here.random.nextFloat() * 360.0f, 0.0f);
        Made.addTag(CourtFx.Tag);
        if (Made instanceof Mob Cultist) {
            Cultist.setPersistenceRequired();
            Player Nearest = Here.getNearestPlayer(Made, 64.0);
            if (Nearest != null && CourtFx.validTarget(Nearest)) {
                Cultist.setTarget(Nearest);
            }
        }
        Here.addFreshEntity(Made);
        Minions.add(Made.getUUID());
        puff(Here, Made.position(), Made.getBbHeight());
        return Made;
    }

    private static void puff(ServerLevel Here, Vec3 At, double Height) {
        Here.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + Height * 0.5, At.z, 10, 0.3, Height * 0.3, 0.3, 0.02);
        Here.sendParticles(CourtFx.Crimson, At.x, At.y + Height * 0.5, At.z, 12, 0.35, Height * 0.35, 0.35, 0.0);
        Here.sendParticles(ParticleTypes.SMALL_FLAME, At.x, At.y + 0.1, At.z, 6, 0.4, 0.05, 0.4, 0.01);
    }

    public void despawnAll(ServerLevel Here) {
        for (Set<UUID> Group : List.of(Minions, Others, Daughters)) {
            for (UUID Id : Group) {
                Entity Found = Here.getEntity(Id);
                if (Found != null && !(Found instanceof Player)) {
                    Found.discard();
                }
            }
            Group.clear();
        }
        AABB Area = CourtArena.area(Dais).inflate(16.0);
        for (Entity Found : Here.getEntitiesOfClass(Entity.class, Area, Each -> !(Each instanceof Player) && (Each.getTags().contains(CourtFx.Tag) || Each instanceof CourtBolt))) {
            Found.discard();
        }
    }
}
