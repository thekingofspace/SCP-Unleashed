package net.rai.scp_unleashed.scp205;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp205LampBlockEntity extends OutletBlockEntity implements WireDevice {
    public static final int PowerDraw = 6;
    public static final int PowerGrace = 10;
    public static final int LoadGrace = 40;
    public static final int TraceInterval = 10;
    public static final int IntruderInterval = 10;
    public static final int RegatherTicks = 160;
    public static final int BlinkTicks = 10;
    public static final int BlinkMin = 160;
    public static final int BlinkMax = 420;
    public static final int HumMin = 90;
    public static final int HumMax = 130;
    public static final double HumReach = 14.0;
    public static final float HumVolume = 0.45f;
    public static final float FlutterVolume = 0.55f;
    public static final byte ModeNone = 0;
    public static final byte ModeShadow = 1;
    public static final byte ModeDevil = 2;
    public static final byte ModeOut = 3;
    public static final String PlacedKey = "Placed";

    private final Set<BlockPos> Placed = new LinkedHashSet<>();
    private int Received;
    private int LastReceived;
    private long LastFed = Long.MIN_VALUE / 2;
    private boolean Loaded;
    private int TraceTimer;
    private int IntruderTimer;
    private int HumTimer = HumMin;
    private int BlinkTimer = BlinkMin;
    private long RegatherAt;
    @Nullable
    private UUID DevilId;
    @Nullable
    private Scp205Beam.Trace LastTrace;
    private byte Mode = ModeNone;
    private byte PoseIndex;
    private byte PrevPose;
    private long BlinkAt = Long.MIN_VALUE / 2;
    private float Length = (float) Scp205Beam.MaxReach;
    private byte HitFace = -1;
    private boolean Wall;

    public Scp205LampBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp205Registry.LampEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp205LampBlockEntity Lamp) {
        if (World instanceof ServerLevel Server) {
            Lamp.tickServer(Server, Pos, State);
        }
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        boolean WasLit = State.getValue(Scp205LampBlock.Lit);
        if (!Loaded) {
            Loaded = true;
            TraceTimer = 0;
            if (WasLit) {
                LastFed = Now + LoadGrace;
            }
        }
        LastReceived = Received;
        if (Received >= PowerDraw) {
            LastFed = Math.max(LastFed, Now);
        }
        Received = 0;
        boolean Active = Now - LastFed <= PowerGrace;
        if (Active != WasLit) {
            Scp205LampBlock.setLit(World, Pos, Active);
            play(World, Pos.above(), Active ? Scp205Registry.WarmLayers : Scp205Registry.CoolLayers, 0.8f, 1.0f);
            TraceTimer = 0;
            HumTimer = Mth.nextInt(World.getRandom(), HumMin / 2, HumMax / 2);
            if (!Active) {
                darken(World);
            }
        }
        if (!Active) {
            return;
        }
        if (--TraceTimer <= 0) {
            TraceTimer = TraceInterval;
            retrace(World, Pos, World.getBlockState(Pos));
        }
        project(World, Pos, Now);
        if (--HumTimer <= 0) {
            HumTimer = Mth.nextInt(World.getRandom(), HumMin, HumMax);
            Vec3 Head = Vec3.atCenterOf(Pos.above());
            if (World.hasNearbyAlivePlayer(Head.x, Head.y, Head.z, HumReach)) {
                play(World, Pos.above(), Scp205Registry.HumLayers, HumVolume, 1.0f);
            }
        }
    }

    private void darken(ServerLevel World) {
        clearGlows(World);
        recall(World);
        LastTrace = null;
        setProjection(ModeNone);
    }

    public void shutDown() {
        if (level instanceof ServerLevel World) {
            clearGlows(World);
            recall(World);
        }
    }

    public void aimChanged() {
        if (level instanceof ServerLevel World && isPowered()) {
            TraceTimer = TraceInterval;
            retrace(World, worldPosition, getBlockState());
        }
    }

    public boolean isPowered() {
        BlockState State = getBlockState();
        return State.hasProperty(Scp205LampBlock.Lit) && State.getValue(Scp205LampBlock.Lit);
    }

    public boolean claims(BlockPos Pos) {
        return Placed.contains(Pos);
    }

    public Set<BlockPos> placedGlows() {
        return Set.copyOf(Placed);
    }

    @Nullable
    public Scp205Beam.Trace lastTrace() {
        return LastTrace;
    }

    private void retrace(ServerLevel World, BlockPos Pos, BlockState State) {
        Scp205Beam.Trace Beam = Scp205Beam.trace(World, Pos, State);
        LastTrace = Beam;
        Map<BlockPos, Integer> Wanted = Scp205Beam.cells(Beam, Pos);
        for (BlockPos Old : List.copyOf(Placed)) {
            if (!Wanted.containsKey(Old)) {
                removeGlow(World, Old);
                Placed.remove(Old);
            }
        }
        for (Map.Entry<BlockPos, Integer> Entry : Wanted.entrySet()) {
            BlockPos Cell = Entry.getKey();
            if (!World.isLoaded(Cell)) {
                continue;
            }
            BlockState Current = World.getBlockState(Cell);
            boolean Ours = Current.getBlock() instanceof Scp205GlowBlock;
            if (!Ours && !Current.isAir()) {
                Placed.remove(Cell);
                continue;
            }
            BlockState Glow = Scp205Registry.Glow.get().defaultBlockState().setValue(Scp205GlowBlock.Brightness, Entry.getValue());
            if (!Ours || Current.getValue(Scp205GlowBlock.Brightness) < Entry.getValue()) {
                World.setBlock(Cell, Glow, Block.UPDATE_CLIENTS);
            }
            Placed.add(Cell);
        }
        float NewLength = (float) Beam.Length();
        byte NewFace = Beam.Face() == null ? -1 : (byte) Beam.Face().get3DDataValue();
        if (Math.abs(NewLength - Length) > 0.01f || NewFace != HitFace || Beam.Wall() != Wall) {
            Length = NewLength;
            HitFace = NewFace;
            Wall = Beam.Wall();
            sync();
        }
        setChanged();
    }

    private void clearGlows(ServerLevel World) {
        for (BlockPos Cell : Placed) {
            removeGlow(World, Cell);
        }
        if (!Placed.isEmpty()) {
            Placed.clear();
            setChanged();
        }
    }

    private static void removeGlow(ServerLevel World, BlockPos Cell) {
        if (World.isLoaded(Cell) && World.getBlockState(Cell).getBlock() instanceof Scp205GlowBlock) {
            World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void project(ServerLevel World, BlockPos Pos, long Now) {
        Scp205Beam.Trace Beam = LastTrace;
        if (Beam == null || !Beam.Wall()) {
            setProjection(ModeNone);
            return;
        }
        boolean Night = World.isNight();
        if (!Night) {
            if (DevilId != null && !(World.getEntity(DevilId) instanceof Scp205Devil)) {
                DevilId = null;
            }
            setProjection(ModeShadow);
        } else if (DevilId != null) {
            if (!(World.getEntity(DevilId) instanceof Scp205Devil Devil) || !Devil.isAlive()) {
                DevilId = null;
                RegatherAt = Now + RegatherTicks;
            }
            setProjection(ModeOut);
        } else if (Now < RegatherAt) {
            setProjection(ModeOut);
        } else {
            setProjection(ModeDevil);
            if (--IntruderTimer <= 0) {
                IntruderTimer = IntruderInterval;
                LivingEntity Prey = intruder(World, Beam);
                if (Prey != null) {
                    release(World, Pos, Beam, Prey);
                }
            }
        }
        if ((Mode == ModeShadow || Mode == ModeDevil) && --BlinkTimer <= 0) {
            BlinkTimer = Mth.nextInt(World.getRandom(), BlinkMin, BlinkMax);
            blink(World, Pos, Now);
        }
    }

    @Nullable
    private LivingEntity intruder(ServerLevel World, Scp205Beam.Trace Beam) {
        if (World.getDifficulty() == Difficulty.PEACEFUL) {
            return null;
        }
        Vec3 End = Beam.End();
        return World.getEntitiesOfClass(LivingEntity.class, Scp205Beam.litArea(Beam), Scp205Devil::prey).stream()
                .min(Comparator.comparingDouble(Each -> Each.distanceToSqr(End))).orElse(null);
    }

    private void release(ServerLevel World, BlockPos Pos, Scp205Beam.Trace Beam, LivingEntity Prey) {
        Vec3 Home = Scp205Beam.home(World, Beam, Pos);
        if (Home == null) {
            return;
        }
        Scp205Devil Devil = Scp205Registry.Devil.get().create(World);
        if (Devil == null) {
            return;
        }
        Vec3 Toward = Beam.Axis().reverse();
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        Devil.moveTo(Home.x, Home.y, Home.z, Yaw, 0.0f);
        Devil.setYHeadRot(Yaw);
        Devil.yBodyRot = Yaw;
        Devil.bind(Pos, Home);
        Devil.setTarget(Prey);
        Devil.beginEmerging();
        if (!World.addFreshEntity(Devil)) {
            return;
        }
        DevilId = Devil.getUUID();
        setProjection(ModeOut);
        play(World, BlockPos.containing(Home), Scp205Registry.EmergeLayers, 1.0f, 1.0f);
    }

    private void recall(ServerLevel World) {
        if (DevilId != null && World.getEntity(DevilId) instanceof Scp205Devil Devil) {
            Devil.fadeOut();
        }
        DevilId = null;
    }

    public boolean anchors(Scp205Devil Devil) {
        return isPowered() && Wall && Devil.getUUID().equals(DevilId) && level != null && level.isNight();
    }

    @Nullable
    public UUID devilId() {
        return DevilId;
    }

    private void blink(ServerLevel World, BlockPos Pos, long Now) {
        int Count = Mode == ModeDevil ? Scp205Poses.DevilCount : Scp205Poses.DayCount;
        PrevPose = PoseIndex;
        PoseIndex = (byte) Scp205Poses.another(World.getRandom(), PoseIndex, Count);
        BlinkAt = Now;
        play(World, Pos.above(), Scp205Registry.FlutterLayers, FlutterVolume, 1.0f);
        sync();
    }

    private void setProjection(byte Next) {
        if (Next == Mode) {
            return;
        }
        boolean Kind = (Next == ModeDevil) != (Mode == ModeDevil);
        Mode = Next;
        if (Kind && level != null) {
            int Count = Next == ModeDevil ? Scp205Poses.DevilCount : Scp205Poses.DayCount;
            PoseIndex = (byte) level.getRandom().nextInt(Count);
            PrevPose = PoseIndex;
        }
        sync();
    }

    public static void play(ServerLevel World, BlockPos Pos, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Varied = Pitch * (0.95f + World.getRandom().nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, Volume, Varied);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public byte mode() {
        return Mode;
    }

    public int pose() {
        return PoseIndex;
    }

    public int previousPose() {
        return PrevPose;
    }

    public long blinkAt() {
        return BlinkAt;
    }

    public float beamLength() {
        return Length;
    }

    @Nullable
    public Direction hitFace() {
        return HitFace < 0 ? null : Direction.from3DDataValue(HitFace);
    }

    public boolean hitsWall() {
        return Wall;
    }

    public int getLastReceived() {
        return LastReceived;
    }

    @Override
    public int getPowerDemand() {
        return PowerDraw;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putByte("Mode", Mode);
        Tag.putByte("Pose", PoseIndex);
        Tag.putByte("PrevPose", PrevPose);
        Tag.putLong("BlinkAt", BlinkAt);
        Tag.putFloat("Length", Length);
        Tag.putByte("HitFace", HitFace);
        Tag.putBoolean("Wall", Wall);
        Tag.putLong("RegatherAt", RegatherAt);
        if (DevilId != null) {
            Tag.putUUID("Devil", DevilId);
        }
        Tag.putLongArray(PlacedKey, Placed.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Mode = Tag.getByte("Mode");
        PoseIndex = Tag.getByte("Pose");
        PrevPose = Tag.getByte("PrevPose");
        BlinkAt = Tag.contains("BlinkAt") ? Tag.getLong("BlinkAt") : Long.MIN_VALUE / 2;
        Length = Tag.contains("Length") ? Tag.getFloat("Length") : (float) Scp205Beam.MaxReach;
        HitFace = Tag.contains("HitFace") ? Tag.getByte("HitFace") : -1;
        Wall = Tag.getBoolean("Wall");
        RegatherAt = Tag.getLong("RegatherAt");
        DevilId = Tag.hasUUID("Devil") ? Tag.getUUID("Devil") : null;
        if (Tag.contains(PlacedKey, CompoundTag.TAG_LONG_ARRAY)) {
            Placed.clear();
            for (long Packed : Tag.getLongArray(PlacedKey)) {
                Placed.add(BlockPos.of(Packed));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Synced = super.getUpdateTag(Registries);
        Synced.remove(PlacedKey);
        Synced.remove("RegatherAt");
        Synced.putBoolean("Scp205", true);
        return Synced;
    }
}
