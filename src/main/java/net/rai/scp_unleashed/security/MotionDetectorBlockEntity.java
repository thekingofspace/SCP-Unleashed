package net.rai.scp_unleashed.security;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.block.entity.PortHolder;
import net.rai.scp_unleashed.block.entity.RedstonePortBlockEntity;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.wire.WireChannels;
import net.rai.scp_unleashed.wire.WireDevice;
import net.rai.scp_unleashed.wire.WireNetworkManager;

import java.util.HashMap;
import java.util.Map;

public class MotionDetectorBlockEntity extends OutletBlockEntity implements WireDevice, PortHolder {
    public static final String KindName = "motion_detector";
    public static final int MinRange = 4;
    public static final int MaxRange = 16;
    public static final int DefaultRange = 12;
    public static final int ScanInterval = 2;
    public static final double SpeedThreshold = 0.04;
    public static final double SneakSpeedThreshold = 0.08;
    public static final double SneakRangeShare = 0.4;
    public static final double ConeCos = Math.cos(Math.toRadians(50.0));
    public static final double LensDepth = 5.0 / 16.0;
    public static final int PulseTicks = 10;
    public static final int RestTicks = 10;
    public static final int HoldTicks = 10;
    public static final int ModePulse = 0;
    public static final int ModeHold = 1;
    public static final int FilterAll = 0;
    public static final int FilterPlayers = 1;
    public static final int FilterMobs = 2;
    public static final int SettingRange = 0;
    public static final int SettingMode = 1;
    public static final int SettingFilter = 2;

    private final Map<Integer, Seen> Tracked = new HashMap<>();
    private int Port;
    private int Range = DefaultRange;
    private int Mode = ModePulse;
    private int Filter = FilterAll;
    private long LastMotion = Long.MIN_VALUE / 2;
    private long PulseStart = Long.MIN_VALUE / 2;
    private long PulseEnd = Long.MIN_VALUE / 2;
    private int LastSent = -1;
    private boolean Signalling;

    public MotionDetectorBlockEntity(BlockPos Pos, BlockState State) {
        super(SecurityRegistry.MotionDetectorEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, MotionDetectorBlockEntity Detector) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof MotionDetectorBlock) {
            Detector.tickServer(ServerWorld, State);
        }
    }

    private void tickServer(ServerLevel World, BlockState State) {
        long Now = World.getGameTime();
        if (Math.floorMod(Now + worldPosition.asLong(), ScanInterval) == 0 && scan(World, State, Now)) {
            LastMotion = Now;
        }
        boolean Motion = Now - LastMotion <= ScanInterval;
        boolean Wanted;
        if (Mode == ModeHold) {
            Wanted = Now - LastMotion <= HoldTicks;
        } else {
            if (Motion && Now >= PulseEnd + RestTicks) {
                PulseStart = Now;
                PulseEnd = Now + PulseTicks;
            }
            Wanted = Now < PulseEnd;
        }
        if (Wanted && !Signalling && (Mode == ModeHold || PulseStart == Now)) {
            beep(World);
        }
        Signalling = Wanted;
        int Signal = Wanted ? MotionDetectorBlock.Signal : 0;
        if (Signal != LastSent || Signal > 0 && Math.floorMod(Now + worldPosition.asLong(), RedstonePortBlockEntity.Heartbeat) == 0) {
            send(World, Port, Signal);
            LastSent = Signal;
        }
        if (State.getValue(MotionDetectorBlock.Tripped) != Wanted) {
            BlockState Next = State.setValue(MotionDetectorBlock.Tripped, Wanted);
            World.setBlock(worldPosition, Next, Block.UPDATE_ALL);
            MotionDetectorBlock.updateNeighbours(World, worldPosition, Next);
        }
    }

    public boolean scan(ServerLevel World, BlockState State, long Now) {
        Direction Front = State.getValue(MotionDetectorBlock.Facing);
        Vec3 Axis = Vec3.atLowerCornerOf(Front.getNormal());
        Vec3 Lens = Vec3.atCenterOf(worldPosition).add(Axis.scale(LensDepth));
        AABB Area = new AABB(Lens, Lens).inflate(Range);
        boolean Found = false;
        Map<Integer, Seen> Fresh = new HashMap<>();
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, Area, this::watches)) {
            Vec3 Centre = Candidate.position().add(0.0, Candidate.getBbHeight() * 0.5, 0.0);
            Seen Before = Tracked.get(Candidate.getId());
            Fresh.put(Candidate.getId(), new Seen(Candidate.position(), Now));
            if (Before == null || Now <= Before.Tick()) {
                continue;
            }
            if (!inCone(Lens, Axis, Centre, rangeFor(Candidate)) || !visible(World, Lens, Candidate)) {
                continue;
            }
            double Speed = Candidate.position().distanceTo(Before.Position()) / (Now - Before.Tick());
            if (Speed > thresholdFor(Candidate)) {
                Found = true;
            }
        }
        Tracked.clear();
        Tracked.putAll(Fresh);
        return Found;
    }

    private boolean watches(LivingEntity Candidate) {
        if (!Candidate.isAlive() || Candidate.isSpectator() || Candidate instanceof ArmorStand || Scp268.isNeglected(Candidate)) {
            return false;
        }
        boolean IsPlayer = Candidate instanceof Player;
        return Filter == FilterAll || Filter == FilterPlayers && IsPlayer || Filter == FilterMobs && !IsPlayer;
    }

    public static boolean sneaking(LivingEntity Candidate) {
        return Candidate.isShiftKeyDown() || Candidate.isCrouching();
    }

    public double rangeFor(LivingEntity Candidate) {
        return sneaking(Candidate) ? Range * SneakRangeShare : Range;
    }

    public static double thresholdFor(LivingEntity Candidate) {
        return sneaking(Candidate) ? SneakSpeedThreshold : SpeedThreshold;
    }

    public static boolean inCone(Vec3 Lens, Vec3 Axis, Vec3 Point, double Reach) {
        Vec3 Offset = Point.subtract(Lens);
        double Distance = Offset.length();
        if (Distance > Reach) {
            return false;
        }
        return Distance < 0.5 || Offset.dot(Axis) / Distance >= ConeCos;
    }

    private boolean visible(ServerLevel World, Vec3 Lens, LivingEntity Candidate) {
        Vec3[] Points = {Candidate.getEyePosition(), Candidate.position().add(0.0, Candidate.getBbHeight() * 0.5, 0.0)};
        for (Vec3 Point : Points) {
            HitResult Hit = World.clip(new ClipContext(Lens, Point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Candidate));
            if (Hit.getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    private void beep(ServerLevel World) {
        Vec3 Centre = Vec3.atCenterOf(worldPosition);
        play(World, Centre, SecurityRegistry.DetectorBeep.get(), 0.4f, 1.0f);
        play(World, Centre, SecurityRegistry.DetectorClick.get(), 0.25f, 1.0f);
    }

    private static void play(ServerLevel World, Vec3 At, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, Sound, SoundSource.BLOCKS, Volume, Pitch);
    }

    private void send(ServerLevel World, int OnPort, int Signal) {
        CompoundTag Payload = new CompoundTag();
        Payload.putInt(RedstonePortBlockEntity.SignalKey, Signal);
        WireNetworkManager.get(World).sendFromDevice(World, worldPosition, WireChannels.redstonePort(OnPort), Payload);
    }

    public void silence(ServerLevel World) {
        if (LastSent > 0) {
            send(World, Port, 0);
            LastSent = 0;
        }
    }

    public int getPort() {
        return Port;
    }

    public int getRange() {
        return Range;
    }

    public int getMode() {
        return Mode;
    }

    public int getFilter() {
        return Filter;
    }

    public boolean isSignalling() {
        return Signalling || getBlockState().hasProperty(MotionDetectorBlock.Tripped) && getBlockState().getValue(MotionDetectorBlock.Tripped);
    }

    public void applySetting(int Setting, int Value) {
        switch (Setting) {
            case SettingRange -> Range = Mth.clamp(Value, MinRange, MaxRange);
            case SettingMode -> Mode = Value == ModeHold ? ModeHold : ModePulse;
            case SettingFilter -> Filter = Mth.clamp(Value, FilterAll, FilterMobs);
            default -> {
                return;
            }
        }
        sync();
    }

    @Override
    public String getKindName() {
        return KindName;
    }

    @Override
    public int getSlotCount() {
        return 1;
    }

    @Override
    public int getSlot(int Slot) {
        return Port;
    }

    @Override
    public void setSlot(int Slot, int Requested) {
        int Clamped = WireChannels.clampPort(Requested);
        if (Slot != 0 || Clamped == Port) {
            return;
        }
        if (level instanceof ServerLevel World && LastSent > 0) {
            send(World, Port, 0);
        }
        Port = Clamped;
        LastSent = -1;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Port", Port);
        Tag.putInt("Range", Range);
        Tag.putInt("Mode", Mode);
        Tag.putInt("Filter", Filter);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Port = WireChannels.clampPort(Tag.getInt("Port"));
        Range = Tag.contains("Range") ? Mth.clamp(Tag.getInt("Range"), MinRange, MaxRange) : DefaultRange;
        Mode = Tag.getInt("Mode") == ModeHold ? ModeHold : ModePulse;
        Filter = Mth.clamp(Tag.getInt("Filter"), FilterAll, FilterMobs);
    }

    private record Seen(Vec3 Position, long Tick) {
    }
}
