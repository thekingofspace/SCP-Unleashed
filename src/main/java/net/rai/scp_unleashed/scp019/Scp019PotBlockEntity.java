package net.rai.scp_unleashed.scp019;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Scp019PotBlockEntity extends BlockEntity {
    public static final int MaxDrones = 25;
    public static final int MaxBrutes = 6;
    public static final int SearchRadius = 25;
    public static final int BruteLeash = 32;
    public static final int FirstEmergeDelay = 30;
    public static final int BruteEmergeGap = 44;
    public static final int DroneEmergeGap = 16;
    public static final int BruteReformTicks = 900;
    public static final int LostReformTicks = 100;
    public static final int UpkeepTicks = 10;
    public static final int RaysPerTick = 24;
    public static final int PathsPerTick = 2;
    public static final int SyncTicks = 20;
    public static final double MouthHeight = 29.0 / 16.0;
    public static final double BruteReach = 1.5;
    public static final double DiagonalBruteReach = 1.9;
    public static final float LidStep = 1.0f / 12.0f;
    public static final int MinAlarmTicks = 100;
    public static final int MaxAlarmTicks = 2400;
    public static final int ChirpGap = 6;
    public static final int MaxChirps = 6;
    public static final double OpenTop = 1.0 + 13.0 / 16.0;
    public static final int SpotRetryTicks = 4;
    public static final int MaxSpotWaits = 8;

    private int Session = RandomSource.create().nextInt(1 << 20);
    private boolean Live;
    private boolean Unloading;
    private final List<UUID> Drones = new ArrayList<>();
    private final List<UUID> Brutes = new ArrayList<>();
    private final IntArrayList BruteTimers = new IntArrayList();
    private int BrutesFormed;
    private int EmergeCooldown;
    private int Upkeep;
    private int Rays;
    private int Paths;
    @Nullable
    private Scp019SearchMap Survey;
    private int SyncedDrones;
    private int SyncedBrutes;
    private int SyncedReforming;
    private int SyncedProgress;
    private int SyncedSweeps;
    private boolean SyncedAlarm;
    private boolean Alarm;
    private long AlarmSince;
    @Nullable
    private UUID AlarmTarget;
    private long LastChirp;
    private int Chirps;
    private int SpotWaits;
    private long LastSync;
    private float Lid;
    private float LidO;
    private boolean LidReady;

    public Scp019PotBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp019Registry.PotEntity.get(), Pos, State);
    }

    public boolean isOpen() {
        return getBlockState().getValue(Scp019PotBlock.Open);
    }

    public int session() {
        return Session;
    }

    public int droneCount() {
        return level != null && level.isClientSide ? SyncedDrones : Drones.size();
    }

    public int bruteCount() {
        return level != null && level.isClientSide ? SyncedBrutes : Brutes.size();
    }

    public int reforming() {
        return level != null && level.isClientSide ? SyncedReforming : BruteTimers.size();
    }

    public boolean alarmed() {
        return level != null && level.isClientSide ? SyncedAlarm : Alarm;
    }

    public int alarmSeconds() {
        return Alarm && level != null ? (int) ((level.getGameTime() - AlarmSince) / 20L) : 0;
    }

    public boolean tryChirp() {
        if (level == null || level.getGameTime() - LastChirp < ChirpGap || Chirps >= MaxChirps) {
            return false;
        }
        LastChirp = level.getGameTime();
        Chirps++;
        return true;
    }

    public void raiseAlarm(@Nullable LivingEntity Target, Scp019Drone Spotter) {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        if (!Alarm) {
            Alarm = true;
            AlarmSince = World.getGameTime();
            Chirps = 0;
        }
        if (Target != null) {
            AlarmTarget = Target.getUUID();
            dispatch(Target, Target.position(), true);
        }
        for (UUID Id : new ArrayList<>(Drones)) {
            if (World.getEntity(Id) instanceof Scp019Drone Drone && Drone != Spotter && Drone.isAlive()) {
                Drone.hide();
            }
        }
        sync(true);
    }

    private void endAlarm(ServerLevel World) {
        Alarm = false;
        AlarmTarget = null;
        for (UUID Id : new ArrayList<>(Drones)) {
            if (World.getEntity(Id) instanceof Scp019Drone Drone) {
                Drone.calm();
            }
        }
        sync(true);
    }

    private boolean alarmOver(ServerLevel World) {
        long Age = World.getGameTime() - AlarmSince;
        if (Age >= MaxAlarmTicks) {
            return true;
        }
        if (Age < MinAlarmTicks) {
            return false;
        }
        for (UUID Id : Drones) {
            if (World.getEntity(Id) instanceof Scp019Drone Drone && Drone.isReporting()) {
                return false;
            }
        }
        Entity Found = AlarmTarget != null ? World.getEntity(AlarmTarget) : null;
        if (!Scp019Creature.isValidTarget(Found, World.getDifficulty() == Difficulty.PEACEFUL) || !inLeash(centre(), Found.position(), BruteLeash)) {
            return true;
        }
        for (UUID Id : Brutes) {
            if (World.getEntity(Id) instanceof Scp019Brute Brute && Brute.isAlive()
                    && (Brute.getTarget() != null || Brute.hasOrder() || Brute.distanceToPost() > Scp019Brute.PostReach * 2.0)) {
                return false;
            }
        }
        return true;
    }

    public int searchPercent() {
        if (level != null && level.isClientSide) {
            return SyncedProgress;
        }
        return Survey != null ? Math.round(Survey.progress() * 100.0f) : 0;
    }

    public int sweeps() {
        if (level != null && level.isClientSide) {
            return SyncedSweeps;
        }
        return Survey != null ? Survey.sweeps() : 0;
    }

    @Nullable
    public Scp019SearchMap map() {
        return Survey;
    }

    public boolean owns(UUID Id) {
        return Drones.contains(Id) || Brutes.contains(Id);
    }

    public Vec3 centre() {
        return Vec3.atBottomCenterOf(worldPosition);
    }

    public Vec3 mouth() {
        return centre().add(0.0, MouthHeight, 0.0);
    }

    public boolean takeRay() {
        if (Rays <= 0) {
            return false;
        }
        Rays--;
        return true;
    }

    public boolean takePath() {
        if (Paths <= 0) {
            return false;
        }
        Paths--;
        return true;
    }

    public float lid(float Partial) {
        return Mth.lerp(Partial, LidO, Lid);
    }

    public void toggle(@Nullable Player User) {
        if (isOpen()) {
            close();
        } else {
            open();
        }
    }

    public void open() {
        if (!(level instanceof ServerLevel World) || isOpen()) {
            return;
        }
        Scp019PotBlock.setOpen(World, worldPosition, true);
        Vec3 Mouth = mouth();
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp019Registry.PotOpen.get(), SoundSource.BLOCKS, 1.0f, 0.95f + World.random.nextFloat() * 0.1f);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp019Registry.PotOpenScrape.get(), SoundSource.BLOCKS, 0.7f, 0.9f + World.random.nextFloat() * 0.1f);
        begin();
    }

    public void close() {
        if (!(level instanceof ServerLevel World) || !isOpen()) {
            return;
        }
        Scp019PotBlock.setOpen(World, worldPosition, false);
        Vec3 Mouth = mouth();
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp019Registry.PotClose.get(), SoundSource.BLOCKS, 1.0f, 0.95f + World.random.nextFloat() * 0.1f);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp019Registry.PotCloseSeat.get(), SoundSource.BLOCKS, 0.8f, 0.9f + World.random.nextFloat() * 0.1f);
        dismissAll(World);
        Live = false;
        Survey = null;
        Alarm = false;
        AlarmTarget = null;
        Session++;
        sync(true);
    }

    private void begin() {
        Session++;
        Live = true;
        Alarm = false;
        AlarmTarget = null;
        Drones.clear();
        Brutes.clear();
        BruteTimers.clear();
        for (int Index = 0; Index < MaxBrutes; Index++) {
            BruteTimers.add(FirstEmergeDelay + Index * BruteEmergeGap);
        }
        BrutesFormed = 0;
        EmergeCooldown = FirstEmergeDelay;
        Survey = new Scp019SearchMap(worldPosition);
        setChanged();
        sync(true);
    }

    private void dismissAll(ServerLevel World) {
        for (UUID Id : new ArrayList<>(Drones)) {
            if (World.getEntity(Id) instanceof Scp019Creature Creature) {
                Creature.puffAway();
            }
        }
        for (UUID Id : new ArrayList<>(Brutes)) {
            if (World.getEntity(Id) instanceof Scp019Creature Creature) {
                Creature.puffAway();
            }
        }
        Drones.clear();
        Brutes.clear();
        BruteTimers.clear();
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp019PotBlockEntity Pot) {
        if (!(World instanceof ServerLevel Server) || !State.getValue(Scp019PotBlock.Open)) {
            return;
        }
        if (!Pot.Live) {
            Pot.begin();
        }
        Pot.Rays = RaysPerTick;
        Pot.Paths = PathsPerTick;
        if (Pot.Survey != null) {
            Pot.Survey.tick(World);
            if (Pot.Survey.finished()) {
                Pot.Survey.reset();
            }
        }
        if (++Pot.Upkeep >= UpkeepTicks) {
            Pot.Upkeep = 0;
            Pot.upkeep(Server);
        }
        for (int Index = 0; Index < Pot.BruteTimers.size(); Index++) {
            Pot.BruteTimers.set(Index, Pot.BruteTimers.getInt(Index) - 1);
        }
        if (Pot.EmergeCooldown > 0) {
            Pot.EmergeCooldown--;
        } else {
            Pot.emergeNext(Server);
        }
        Pot.sync(false);
    }

    private void emergeNext(ServerLevel World) {
        int Ready = -1;
        for (int Index = 0; Index < BruteTimers.size(); Index++) {
            if (BruteTimers.getInt(Index) <= 0) {
                Ready = Index;
                break;
            }
        }
        if (Ready >= 0 && Brutes.size() < MaxBrutes) {
            if (spawn(World, true)) {
                BruteTimers.removeInt(Ready);
                BrutesFormed++;
                EmergeCooldown = DroneEmergeGap;
            } else {
                EmergeCooldown = SpotRetryTicks;
            }
            return;
        }
        if (BrutesFormed >= MaxBrutes && Drones.size() < MaxDrones && !Alarm) {
            EmergeCooldown = spawn(World, false) ? DroneEmergeGap + World.random.nextInt(6) : SpotRetryTicks;
        }
    }

    private void upkeep(ServerLevel World) {
        Drones.removeIf(Id -> !(World.getEntity(Id) instanceof Scp019Drone Drone) || !Drone.isAlive());
        Brutes.removeIf(Id -> !(World.getEntity(Id) instanceof Scp019Brute Brute) || !Brute.isAlive());
        while (Brutes.size() + BruteTimers.size() < MaxBrutes) {
            BruteTimers.add(LostReformTicks);
        }
        if (Alarm && alarmOver(World)) {
            endAlarm(World);
        }
    }

    public void bruteFell(Scp019Brute Brute) {
        if (Brutes.remove(Brute.getUUID())) {
            BruteTimers.add(BruteReformTicks);
        }
    }

    public void droneGone(Scp019Drone Drone) {
        Drones.remove(Drone.getUUID());
    }

    public void report(@Nullable LivingEntity Target, Vec3 LastSeen) {
        dispatch(Target, LastSeen, false);
    }

    public int dispatch(@Nullable LivingEntity Target, Vec3 LastSeen, boolean Roar) {
        if (!(level instanceof ServerLevel World)) {
            return 0;
        }
        int Sent = 0;
        for (UUID Id : Brutes) {
            if (World.getEntity(Id) instanceof Scp019Brute Brute && Brute.isAlive() && Brute.order(Target, LastSeen)) {
                if (Roar && Sent == 0) {
                    Brute.roar();
                }
                Sent++;
            }
        }
        return Sent;
    }

    private boolean spawn(ServerLevel World, boolean Heavy) {
        EntityType<? extends Scp019Creature> Type = Heavy ? Scp019Registry.Brute.get() : Scp019Registry.Drone.get();
        Spot Found = findSpot(World, Type, Heavy, SpotWaits >= MaxSpotWaits);
        if (Found == null) {
            SpotWaits++;
            return false;
        }
        Scp019Creature Creature = Type.create(World);
        if (Creature == null) {
            return false;
        }
        Creature.moveTo(Found.Position().x, Found.Position().y, Found.Position().z, Found.Yaw(), 0.0f);
        Creature.setYHeadRot(Found.Yaw());
        Creature.setYBodyRot(Found.Yaw());
        Creature.setDeltaMovement(Vec3.ZERO);
        Creature.bind(this);
        Creature.beginEmerging(Found.Mouth());
        Creature.applyLockedYaw();
        if (!Creature.isFreshlyForming() || !World.addFreshEntity(Creature)) {
            return false;
        }
        SpotWaits = 0;
        (Heavy ? Brutes : Drones).add(Creature.getUUID());
        Vec3 Mouth = mouth();
        float Pitch = 0.9f + World.random.nextFloat() * 0.2f;
        if (Heavy) {
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp019Registry.BruteEmerge.get(), SoundSource.HOSTILE, 1.0f, Pitch);
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp019Registry.BruteEmergeLow.get(), SoundSource.HOSTILE, 0.8f, Pitch);
        } else {
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp019Registry.Emerge.get(), SoundSource.HOSTILE, 0.7f, Pitch);
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp019Registry.EmergeScrabble.get(), SoundSource.HOSTILE, 0.5f, Pitch);
        }
        World.sendParticles(ParticleTypes.SMOKE, Mouth.x, Mouth.y + 0.05, Mouth.z, Heavy ? 8 : 3, 0.12, 0.05, 0.12, 0.01);
        return true;
    }

    public record Spot(Vec3 Position, float Yaw, boolean Mouth) {
    }

    @Nullable
    private Spot findSpot(ServerLevel World, EntityType<?> Type, boolean Heavy, boolean Desperate) {
        Vec3 Centre = centre();
        List<int[]> Sides = new ArrayList<>(List.of(new int[]{1, 0}, new int[]{-1, 0}, new int[]{0, 1}, new int[]{0, -1}));
        List<int[]> Corners = new ArrayList<>(List.of(new int[]{1, 1}, new int[]{-1, 1}, new int[]{1, -1}, new int[]{-1, -1}));
        Collections.shuffle(Sides, new Random(World.random.nextLong()));
        Collections.shuffle(Corners, new Random(World.random.nextLong()));
        Direction Lid = lidSide();
        Sides.sort((First, Second) -> Boolean.compare(First[0] == Lid.getStepX() && First[1] == Lid.getStepZ(), Second[0] == Lid.getStepX() && Second[1] == Lid.getStepZ()));
        List<int[]> Order = new ArrayList<>(Sides);
        Order.addAll(Corners);
        for (int Pass = 0; Pass < (Desperate ? 3 : 1); Pass++) {
            boolean NeedGround = Pass < 2;
            boolean NeedRoom = Pass == 0;
            for (int[] Side : Order) {
                boolean Diagonal = Side[0] != 0 && Side[1] != 0;
                double Reach = Heavy ? (Diagonal ? DiagonalBruteReach : BruteReach) : 1.0;
                double Length = Math.sqrt(Side[0] * Side[0] + Side[1] * Side[1]);
                double X = Centre.x + Side[0] / Length * Reach;
                double Z = Centre.z + Side[1] / Length * Reach;
                float Yaw = (float) (Mth.atan2(-Side[0], Side[1]) * Mth.RAD_TO_DEG);
                for (int Dy : new int[]{0, -1, 1}) {
                    double Y = worldPosition.getY() + Dy;
                    AABB Box = Type.getDimensions().makeBoundingBox(X, Y, Z);
                    if (!World.noCollision(Box)) {
                        continue;
                    }
                    boolean Grounded = !World.noCollision(Box.move(0.0, -0.25, 0.0));
                    if (NeedGround && !Grounded) {
                        continue;
                    }
                    if (NeedRoom && !World.getEntitiesOfClass(Scp019Creature.class, Box.inflate(0.15)).isEmpty()) {
                        continue;
                    }
                    return new Spot(new Vec3(X, Y, Z), Yaw, Dy == 0 && Grounded && !(Heavy && Diagonal));
                }
            }
        }
        if (!Desperate) {
            return null;
        }
        double Top = worldPosition.getY() + OpenTop;
        AABB Box = Type.getDimensions().makeBoundingBox(Centre.x, Top, Centre.z);
        if (World.noCollision(Box) && World.getEntitiesOfClass(Scp019Creature.class, Box.inflate(0.15)).isEmpty()) {
            return new Spot(new Vec3(Centre.x, Top, Centre.z), World.random.nextFloat() * 360.0f, false);
        }
        return null;
    }

    public Direction lidSide() {
        return getBlockState().getValue(Scp019PotBlock.Facing).getCounterClockWise();
    }

    public List<Vec3> claimedSpots(Scp019Drone Asking) {
        List<Vec3> Found = new ArrayList<>();
        if (!(level instanceof ServerLevel World)) {
            return Found;
        }
        for (UUID Id : Drones) {
            if (World.getEntity(Id) instanceof Scp019Drone Drone && Drone != Asking) {
                Vec3 Goal = Drone.waypointCentre();
                Found.add(Goal != null ? Goal : Drone.position());
            }
        }
        return Found;
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp019PotBlockEntity Pot) {
        float Target = State.getValue(Scp019PotBlock.Open) ? 1.0f : 0.0f;
        if (!Pot.LidReady) {
            Pot.Lid = Target;
            Pot.LidO = Target;
            Pot.LidReady = true;
        }
        Pot.LidO = Pot.Lid;
        if (Pot.Lid < Target) {
            Pot.Lid = Math.min(Target, Pot.Lid + LidStep);
        } else if (Pot.Lid > Target) {
            Pot.Lid = Math.max(Target, Pot.Lid - LidStep);
        }
        if (Target > 0.5f && World.random.nextInt(14) == 0) {
            Vec3 Mouth = Pot.mouth();
            World.addParticle(ParticleTypes.SMOKE, Mouth.x + (World.random.nextDouble() - 0.5) * 0.25, Mouth.y - 0.05, Mouth.z + (World.random.nextDouble() - 0.5) * 0.25,
                    0.0, 0.015, 0.0);
        }
    }

    private void sync(boolean Force) {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean Changed = SyncedDrones != Drones.size() || SyncedBrutes != Brutes.size() || SyncedReforming != BruteTimers.size() || SyncedProgress != searchPercent()
                || SyncedSweeps != sweeps() || SyncedAlarm != Alarm;
        if (Force || Changed && level.getGameTime() - LastSync >= SyncTicks) {
            SyncedDrones = Drones.size();
            SyncedBrutes = Brutes.size();
            SyncedReforming = BruteTimers.size();
            SyncedProgress = searchPercent();
            SyncedSweeps = sweeps();
            SyncedAlarm = Alarm;
            LastSync = level.getGameTime();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        Unloading = true;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!Unloading && level instanceof ServerLevel World) {
            dismissAll(World);
        }
        Live = false;
        Survey = null;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        Unloading = false;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Session", Session);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Drones")) {
            SyncedDrones = Tag.getInt("Drones");
            SyncedBrutes = Tag.getInt("Brutes");
            SyncedReforming = Tag.getInt("Reforming");
            SyncedProgress = Tag.getInt("Progress");
            SyncedSweeps = Tag.getInt("Sweeps");
            SyncedAlarm = Tag.getBoolean("Alarm");
        }
        if (Tag.contains("Session") && level == null) {
            Session = Tag.getInt("Session") + 1;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Drones", Drones.size());
        Tag.putInt("Brutes", Brutes.size());
        Tag.putInt("Reforming", BruteTimers.size());
        Tag.putInt("Progress", searchPercent());
        Tag.putInt("Sweeps", sweeps());
        Tag.putBoolean("Alarm", Alarm);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static boolean inLeash(Vec3 Centre, Vec3 Point, double Reach) {
        double Dx = Point.x - Centre.x;
        double Dz = Point.z - Centre.z;
        return Dx * Dx + Dz * Dz <= Reach * Reach && Math.abs(Point.y - Centre.y) <= 16.0;
    }
}
