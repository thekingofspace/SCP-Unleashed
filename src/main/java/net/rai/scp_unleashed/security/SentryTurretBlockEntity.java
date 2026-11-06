package net.rai.scp_unleashed.security;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.guns.GunFiring;
import net.rai.scp_unleashed.guns.GunGlass;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSoundMixer;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.guns.network.GunImpact;
import net.rai.scp_unleashed.guns.network.GunShotPayload;
import net.rai.scp_unleashed.space.SpaceAir;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SentryTurretBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int AmmoSlots = 3;
    public static final int IdleDraw = 4;
    public static final int TrackDraw = 12;
    public static final int FireDraw = 24;
    public static final int PowerGrace = 2;
    public static final double Range = 24.0;
    public static final double ShotReach = 32.0;
    public static final int ScanInterval = 5;
    public static final float YawSpeed = 10.0f;
    public static final float PitchSpeed = 7.0f;
    public static final float AimTolerance = 3.5f;
    public static final int FireHold = 12;
    public static final int DryEvery = 16;
    public static final int SyncTicks = 4;
    public static final int FlashTicks = 3;
    public static final double PickPadding = 0.05;
    public static final double ClearanceSlack = 0.35;
    public static final int DataStatus = 0;
    public static final int DataRounds = 1;
    public static final int DataRound = 2;
    public static final int DataDemand = 3;
    public static final int DataCount = 4;
    public static final String DefaultShooterName = "[Sentry Turret]";
    public static final UUID DefaultShooterId = UUID.fromString("5e7a1c2e-50c0-4e11-9b1e-7a5c0a7f1001");

    private final ItemStackHandler Ammo = new ItemStackHandler(AmmoSlots) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return TurretRound.of(Stack) != null;
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
        }
    };
    private final IItemHandler Automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return AmmoSlots;
        }

        @Override
        public ItemStack getStackInSlot(int Slot) {
            return Ammo.getStackInSlot(Slot);
        }

        @Override
        public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
            return Ammo.insertItem(Slot, Stack, Simulate);
        }

        @Override
        public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int Slot) {
            return Ammo.getSlotLimit(Slot);
        }

        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Ammo.isItemValid(Slot, Stack);
        }
    };
    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataStatus -> Status.ordinal();
                case DataRounds -> rounds();
                case DataRound -> {
                    TurretRound Round = loadedRound();
                    yield Round == null ? -1 : Round.ordinal();
                }
                case DataDemand -> Demand;
                default -> 0;
            };
        }

        @Override
        public void set(int Index, int Value) {
        }

        @Override
        public int getCount() {
            return DataCount;
        }
    };
    private final TurretRules Rules = new TurretRules();
    @Nullable
    private UUID Owner;
    private String OwnerName = "";
    private TurretStatus Status = TurretStatus.Offline;
    private int TargetId = -1;
    @Nullable
    private LivingEntity Target;
    private Vec3 AimOffset = Vec3.ZERO;
    private float Yaw = Float.NaN;
    private float Pitch;
    private float PreviousYaw;
    private float PreviousPitch;
    private int Demand = IdleDraw;
    private int Received;
    private long LastFed = Long.MIN_VALUE / 2;
    private long NextShot;
    private long LastShot = Long.MIN_VALUE / 2;
    private long LastDry = Long.MIN_VALUE / 2;
    private long ChirpAt = -1;
    private int Streak;
    private int Barrel;
    private boolean NeedsSync = true;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private long FlashTick = Long.MIN_VALUE / 2;
    private int FlashBarrel;
    private float Recoil;
    private float PreviousRecoil;

    public SentryTurretBlockEntity(BlockPos Pos, BlockState State) {
        super(SecurityRegistry.SentryTurretEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, SentryTurretBlockEntity Turret) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof SentryTurretBlock) {
            Turret.tickServer(ServerWorld, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, SentryTurretBlockEntity Turret) {
        if (State.getBlock() instanceof SentryTurretBlock) {
            Turret.tickClient(World, State);
        }
    }

    private boolean hanging(BlockState State) {
        return State.getValue(SentryTurretBlock.Hanging);
    }

    private Direction front(BlockState State) {
        return State.getValue(SentryTurretBlock.Facing);
    }

    public Vec3 pivot() {
        BlockState State = getBlockState();
        return TurretGeometry.pivot(worldPosition, State.hasProperty(SentryTurretBlock.Hanging) && State.getValue(SentryTurretBlock.Hanging));
    }

    private long seed() {
        return Math.floorMod(worldPosition.asLong() * 31L, 997L);
    }

    private void settleAngles(BlockState State) {
        if (Float.isNaN(Yaw)) {
            Yaw = TurretGeometry.restYaw(front(State));
            Pitch = TurretGeometry.droop(hanging(State));
            PreviousYaw = Yaw;
            PreviousPitch = Pitch;
        }
    }

    private void tickServer(ServerLevel World, BlockState State) {
        settleAngles(State);
        long Now = World.getGameTime();
        boolean Fed = Demand > 0 && Received >= Demand;
        Received = 0;
        if (Fed) {
            LastFed = Now;
        }
        boolean Powered = Now - LastFed <= PowerGrace;
        boolean Hanging = hanging(State);
        TurretStatus Next;
        if (!Powered) {
            dropTarget();
            Demand = IdleDraw;
            Next = TurretStatus.Offline;
            turnToward(TurretGeometry.restYaw(front(State)), TurretGeometry.droop(Hanging), 0.5f);
        } else if (SpaceAir.isAirless(World)) {
            dropTarget();
            Demand = IdleDraw;
            Next = TurretStatus.Jammed;
            turnToward(TurretGeometry.restYaw(front(State)), TurretGeometry.restPitch(Hanging), 0.5f);
        } else {
            Next = tickArmed(World, State, Now);
        }
        if (ChirpAt >= 0 && Now >= ChirpAt) {
            ChirpAt = -1;
            play(World, SecurityRegistry.TurretAlert.get(), 0.45f, 1.5f);
        }
        setStatus(World, State, Next);
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
        }
    }

    private TurretStatus tickArmed(ServerLevel World, BlockState State, long Now) {
        boolean Hanging = hanging(State);
        TurretRound Round = loadedRound();
        boolean Had = Target != null;
        if (Target != null && !stillTarget(Target)) {
            dropTarget();
        }
        if (Target == null || Math.floorMod(Now + seed(), ScanInterval) == 0) {
            acquire(World, Hanging);
        }
        if (Had && Target == null) {
            play(World, SecurityRegistry.TurretServo.get(), 0.35f, 0.95f);
        }
        if (Target == null) {
            Demand = IdleDraw;
            Streak = 0;
            turnToward(TurretGeometry.sweepYaw(front(State), Now, seed()), TurretGeometry.restPitch(Hanging), 1.0f);
            if (TurretGeometry.sweepTurning(Now, seed())) {
                play(World, SecurityRegistry.TurretServoTick.get(), 0.12f, 1.4f);
            }
            return Round == null ? TurretStatus.NoAmmo : TurretStatus.Ready;
        }
        Vec3 Pivot = pivot();
        Vec3 AimPoint = Target.position().add(AimOffset);
        float WantYaw = TurretGeometry.yawToward(Pivot, AimPoint);
        float WantPitch = Mth.clamp(TurretGeometry.pitchToward(Pivot, AimPoint), TurretGeometry.minPitch(Hanging), TurretGeometry.maxPitch(Hanging));
        turnToward(WantYaw, WantPitch, 1.0f);
        boolean Aligned = Math.abs(Mth.wrapDegrees(Yaw - WantYaw)) <= AimTolerance && Math.abs(Pitch - WantPitch) <= AimTolerance;
        TurretStatus Next = TurretStatus.Tracking;
        Demand = TrackDraw;
        if (Aligned && Now >= NextShot) {
            if (Round == null) {
                if (Now - LastDry >= DryEvery) {
                    LastDry = Now;
                    dryFire(World);
                }
                return TurretStatus.NoAmmo;
            }
            if (clearShot(World, Pivot, AimPoint, Target)) {
                fire(World, Round, Pivot, AimPoint, Hanging, Now);
                consumeRound();
                NextShot = Now + Round.getInterval();
            } else {
                Next = TurretStatus.Blocked;
            }
        }
        if (Now - LastShot <= FireHold && Round != null) {
            Demand = FireDraw;
            return TurretStatus.Firing;
        }
        return Round == null ? TurretStatus.NoAmmo : Next;
    }

    private void turnToward(float WantYaw, float WantPitch, float Rate) {
        Yaw = Mth.approachDegrees(Yaw, WantYaw, YawSpeed * Rate);
        Pitch = Mth.approach(Pitch, WantPitch, PitchSpeed * Rate);
    }

    private boolean stillTarget(LivingEntity Candidate) {
        return Candidate.level() == level && !Candidate.isRemoved() && TurretTargeting.isTarget(Rules, Owner, Candidate)
                && Candidate.position().add(AimOffset).distanceToSqr(pivot()) <= Range * Range;
    }

    private void dropTarget() {
        if (Target != null || TargetId != -1) {
            Target = null;
            TargetId = -1;
            NeedsSync = true;
            UrgentSync = true;
        }
    }

    private void acquire(ServerLevel World, boolean Hanging) {
        Vec3 Pivot = pivot();
        if (Target != null) {
            Vec3 Seen = visiblePoint(World, Pivot, Target, Hanging);
            if (Seen != null) {
                AimOffset = Seen.subtract(Target.position());
                return;
            }
        }
        List<LivingEntity> Candidates = World.getEntitiesOfClass(LivingEntity.class, new AABB(Pivot, Pivot).inflate(Range),
                Candidate -> Candidate.distanceToSqr(Pivot) <= Range * Range && TurretTargeting.isTarget(Rules, Owner, Candidate));
        Candidates.sort(Comparator.comparingDouble(Candidate -> Candidate.distanceToSqr(Pivot)));
        for (LivingEntity Candidate : Candidates) {
            Vec3 Seen = visiblePoint(World, Pivot, Candidate, Hanging);
            if (Seen != null) {
                boolean Fresh = Target == null;
                Target = Candidate;
                TargetId = Candidate.getId();
                AimOffset = Seen.subtract(Candidate.position());
                NeedsSync = true;
                UrgentSync = true;
                if (Fresh) {
                    play(World, SecurityRegistry.TurretServo.get(), 0.5f, 1.25f);
                    play(World, SecurityRegistry.TurretServoHum.get(), 0.3f, 1.6f);
                    play(World, SecurityRegistry.TurretAlert.get(), 0.45f, 1.3f);
                    ChirpAt = World.getGameTime() + 3;
                }
                return;
            }
        }
        dropTarget();
    }

    @Nullable
    private Vec3 visiblePoint(ServerLevel World, Vec3 Pivot, LivingEntity Candidate, boolean Hanging) {
        Vec3[] Points = {
                Candidate.position().add(0.0, Candidate.getBbHeight() * 0.5, 0.0),
                Candidate.position().add(0.0, Candidate.getBbHeight() * 0.85, 0.0),
                Candidate.position().add(0.0, Candidate.getBbHeight() * 0.2, 0.0)
        };
        for (Vec3 Point : Points) {
            float WantPitch = TurretGeometry.pitchToward(Pivot, Point);
            if (WantPitch < TurretGeometry.minPitch(Hanging) || WantPitch > TurretGeometry.maxPitch(Hanging)) {
                continue;
            }
            if (clearShot(World, Pivot, Point, Candidate)) {
                return Point;
            }
        }
        return null;
    }

    private boolean clearShot(ServerLevel World, Vec3 Pivot, Vec3 Point, LivingEntity Intended) {
        Vec3 Heading = Point.subtract(Pivot);
        double Length = Heading.length();
        if (Length < 1.0E-3) {
            return true;
        }
        Vec3 Toward = Heading.scale(1.0 / Length);
        Vec3 Muzzle = Pivot.add(Toward.scale(Math.min(TurretGeometry.MuzzleReach, Length * 0.5)));
        double Distance = Muzzle.distanceTo(Point);
        GunGlass.Route Path = GunGlass.trace(World, shooter(World), Muzzle, Point);
        if (Path.lodged() || Path.solid() && Muzzle.distanceTo(Path.limit()) < Distance - ClearanceSlack) {
            return false;
        }
        for (Hit Struck : livingAlong(World, Muzzle, Point)) {
            if (Struck.Target() == Intended) {
                return true;
            }
            if (!TurretTargeting.isTarget(Rules, Owner, Struck.Target())) {
                return false;
            }
        }
        return true;
    }

    private void fire(ServerLevel World, TurretRound Round, Vec3 Pivot, Vec3 AimPoint, boolean Hanging, long Now) {
        FakePlayer Shooter = shooter(World);
        GunKind Kind = Round.getKind();
        GunStats Stats = Kind.getStats();
        RandomSource Random = World.getRandom();
        Barrel = 1 - Barrel;
        Vec3 Muzzle = TurretGeometry.muzzle(Pivot, Yaw, Pitch, Hanging, Barrel);
        Vec3 Centre = GunFiring.scatter(AimPoint.subtract(Muzzle).normalize(), Round.getSpread(), Random);
        List<GunImpact> Impacts = new ArrayList<>();
        Map<LivingEntity, Float> Wounds = new LinkedHashMap<>();
        Map<LivingEntity, List<Integer>> Marks = new HashMap<>();
        Set<BlockPos> Shattered = new LinkedHashSet<>();
        Map<BlockPos, Boolean> Verdicts = new HashMap<>();
        List<BlockHitResult> Surfaces = new ArrayList<>();
        Map<BlockPos, Float> Blows = new LinkedHashMap<>();
        Map<BlockPos, BlockHitResult> BlowSurfaces = new HashMap<>();
        for (int Pellet = 0; Pellet < Round.getPellets(); Pellet++) {
            Vec3 Heading = Round.getPellets() > 1 ? GunFiring.scatter(Centre, Round.getPelletSpread(), Random) : Centre;
            Vec3 End = Muzzle.add(Heading.scale(ShotReach));
            GunGlass.Route Path = GunGlass.trace(World, Shooter, Muzzle, End);
            List<Hit> Hits = livingAlong(World, Muzzle, Path.limit());
            Hit First = Hits.isEmpty() ? null : Hits.get(0);
            Path = Path.settle(World, Shooter, First != null ? First.Distance() : Double.MAX_VALUE, Verdicts);
            Vec3 Limit = Path.limit();
            Shattered.addAll(Path.positions());
            Direction Back = Direction.getNearest(-Heading.x, -Heading.y, -Heading.z);
            if (First != null && First.Distance() <= Muzzle.distanceTo(Limit) + PickPadding) {
                if (TurretTargeting.isTarget(Rules, Owner, First.Target())) {
                    float Amount = Round.getDamage() * Stats.falloff(First.Distance()) * Path.carryBefore(First.Distance());
                    Wounds.merge(First.Target(), Amount, Float::sum);
                    Marks.computeIfAbsent(First.Target(), Key -> new ArrayList<>()).add(Impacts.size());
                    Impacts.add(GunImpact.at(Muzzle, First.Point(), GunImpact.Wound, Back));
                } else {
                    Impacts.add(GunImpact.at(Muzzle, First.Point(), GunImpact.Miss, Back));
                }
                continue;
            }
            if (Path.solid()) {
                BlockHitResult Surface = Path.surface();
                Impacts.add(GunImpact.at(Muzzle, Limit, GunImpact.Surface, Surface.getDirection()));
                Surfaces.add(Surface);
                BlockPos Struck = Surface.getBlockPos().immutable();
                Blows.merge(Struck, Round.getDamage() * Stats.falloff(Muzzle.distanceTo(Limit)) * Path.carry(), Float::sum);
                BlowSurfaces.putIfAbsent(Struck, Surface);
            } else {
                Impacts.add(GunImpact.at(Muzzle, Limit, GunImpact.Miss, Direction.UP));
            }
        }
        DamageSource Source = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SecurityRegistry.TurretDamage), Muzzle);
        for (Map.Entry<LivingEntity, Float> Entry : Wounds.entrySet()) {
            LivingEntity Struck = Entry.getKey();
            Struck.invulnerableTime = 0;
            boolean Landed = Struck.hurt(Source, Entry.getValue());
            if (Landed && (!Struck.isAlive() || Struck.isDeadOrDying())) {
                for (int Index : Marks.get(Struck)) {
                    Impacts.set(Index, Impacts.get(Index).withType(GunImpact.Kill));
                }
            }
        }
        if (Impacts.size() > GunShotPayload.MaxImpacts) {
            Impacts = new ArrayList<>(Impacts.subList(0, GunShotPayload.MaxImpacts));
        }
        PacketDistributor.sendToPlayersTrackingChunk(World, new ChunkPos(worldPosition), new GunShotPayload(-1, Kind.ordinal(), Muzzle, Impacts));
        Streak = Round.getInterval() <= 6 && Now - LastShot <= Round.getInterval() * 2L ? Streak + 1 : 0;
        LastShot = Now;
        GunSounds.play(World, null, Muzzle.x, Muzzle.y, Muzzle.z, GunSounds.burst(GunSounds.shot(Kind), Streak), Round.getVolume(), GunSounds.jitter(Streak));
        World.blockEvent(worldPosition, getBlockState().getBlock(), SentryTurretBlock.EventFire, Barrel);
        World.gameEvent(GameEvent.PROJECTILE_SHOOT, Muzzle, GameEvent.Context.of(getBlockState()));
        GunGlass.shatter(World, Shooter, Shattered);
        Blows.forEach((Pos, Amount) -> GunGlass.strike(World, Shooter, BlowSurfaces.get(Pos), Amount));
        for (int Index = 0; Index < Surfaces.size(); Index++) {
            BlockHitResult Surface = Surfaces.get(Index);
            BlockPos Pos = Surface.getBlockPos();
            World.gameEvent(GameEvent.PROJECTILE_LAND, Surface.getLocation(), GameEvent.Context.of(getBlockState()));
            if (Index < GunFiring.MaxImpactSounds) {
                BlockState Struck = World.getBlockState(Pos);
                SoundType Sounds = Struck.getSoundType(World, Pos, Shooter);
                GunSoundMixer.emitAt(World, null, Pos, Sounds.getHitSound(), SoundSource.BLOCKS, Sounds.getVolume() * 0.7f, Sounds.getPitch() * 1.25f);
            }
        }
    }

    private void dryFire(ServerLevel World) {
        Vec3 Pivot = pivot();
        GunSounds.play(World, null, Pivot.x, Pivot.y, Pivot.z, GunSounds.dryFire(), 0.9f);
        play(World, SecurityRegistry.TurretEmpty.get(), 0.5f, 1.35f);
        World.blockEvent(worldPosition, getBlockState().getBlock(), SentryTurretBlock.EventDry, 0);
    }

    private List<Hit> livingAlong(ServerLevel World, Vec3 Start, Vec3 End) {
        AABB Area = new AABB(Start, End).inflate(1.0);
        List<Hit> Hits = new ArrayList<>();
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> Found.isPickable() && !Found.isSpectator() && Found.isAlive())) {
            AABB Box = Candidate.getBoundingBox().inflate(Candidate.getPickRadius() + PickPadding);
            if (Box.contains(Start)) {
                Hits.add(new Hit(Candidate, Start, 0.0));
                continue;
            }
            Optional<Vec3> Clip = Box.clip(Start, End);
            Clip.ifPresent(Point -> Hits.add(new Hit(Candidate, Point, Start.distanceTo(Point))));
        }
        Hits.sort(Comparator.comparingDouble(Hit::Distance));
        return Hits;
    }

    private FakePlayer shooter(ServerLevel World) {
        GameProfile Profile = Owner != null ? new GameProfile(Owner, OwnerName.isEmpty() ? DefaultShooterName : OwnerName) : new GameProfile(DefaultShooterId, DefaultShooterName);
        FakePlayer Shooter = FakePlayerFactory.get(World, Profile);
        Vec3 Pivot = pivot();
        Shooter.setPos(Pivot.x, Pivot.y, Pivot.z);
        return Shooter;
    }

    private void play(ServerLevel World, SoundEvent Sound, float Volume, float Pitch) {
        Vec3 Pivot = pivot();
        World.playSound(null, Pivot.x, Pivot.y, Pivot.z, Sound, SoundSource.BLOCKS, Volume, Pitch * (0.96f + World.getRandom().nextFloat() * 0.08f));
    }

    private void setStatus(ServerLevel World, BlockState State, TurretStatus Next) {
        if (Next == Status) {
            return;
        }
        TurretStatus Before = Status;
        Status = Next;
        NeedsSync = true;
        if (Before == TurretStatus.Offline && Next != TurretStatus.Offline) {
            play(World, SecurityRegistry.TurretPowerUp.get(), 0.4f, 1.6f);
            play(World, SecurityRegistry.TurretServo.get(), 0.3f, 1.1f);
        } else if (Next == TurretStatus.Offline) {
            play(World, SecurityRegistry.TurretServo.get(), 0.3f, 0.7f);
        }
        TurretLight Light = Next.getLight();
        if (State.getValue(SentryTurretBlock.Light) != Light) {
            World.setBlock(worldPosition, State.setValue(SentryTurretBlock.Light, Light), Block.UPDATE_CLIENTS);
        }
    }

    private void tickClient(Level World, BlockState State) {
        settleAngles(State);
        PreviousYaw = Yaw;
        PreviousPitch = Pitch;
        PreviousRecoil = Recoil;
        Recoil = Math.max(0.0f, Recoil - 0.25f);
        boolean Hanging = hanging(State);
        Entity Seen = TargetId >= 0 ? World.getEntity(TargetId) : null;
        if (Status == TurretStatus.Offline) {
            turnToward(TurretGeometry.restYaw(front(State)), TurretGeometry.droop(Hanging), 0.5f);
        } else if (Seen != null) {
            Vec3 Pivot = pivot();
            Vec3 Point = Seen.position().add(0.0, Seen.getBbHeight() * 0.5, 0.0);
            turnToward(TurretGeometry.yawToward(Pivot, Point),
                    Mth.clamp(TurretGeometry.pitchToward(Pivot, Point), TurretGeometry.minPitch(Hanging), TurretGeometry.maxPitch(Hanging)), 1.0f);
        } else if (Status == TurretStatus.Jammed) {
            turnToward(TurretGeometry.restYaw(front(State)), TurretGeometry.restPitch(Hanging), 0.5f);
        } else {
            turnToward(TurretGeometry.sweepYaw(front(State), World.getGameTime(), seed()), TurretGeometry.restPitch(Hanging), 1.0f);
        }
    }

    @Override
    public boolean triggerEvent(int Id, int Param) {
        if (Id == SentryTurretBlock.EventFire) {
            if (level != null && level.isClientSide) {
                FlashTick = level.getGameTime();
                FlashBarrel = Param;
                Recoil = 1.0f;
                BlockState State = getBlockState();
                if (State.getBlock() instanceof SentryTurretBlock) {
                    Vec3 Muzzle = TurretGeometry.muzzle(pivot(), Yaw, Pitch, hanging(State), Param);
                    level.addParticle(ParticleTypes.SMOKE, Muzzle.x, Muzzle.y, Muzzle.z, 0.0, 0.02, 0.0);
                }
            }
            return true;
        }
        if (Id == SentryTurretBlock.EventDry) {
            if (level != null && level.isClientSide) {
                Recoil = 0.35f;
            }
            return true;
        }
        return super.triggerEvent(Id, Param);
    }

    public float getYaw(float Partial) {
        return Float.isNaN(Yaw) ? 0.0f : Mth.rotLerp(Partial, PreviousYaw, Yaw);
    }

    public float getPitch(float Partial) {
        return Float.isNaN(Yaw) ? 0.0f : Mth.lerp(Partial, PreviousPitch, Pitch);
    }

    public float getRecoil(float Partial) {
        return Mth.lerp(Partial, PreviousRecoil, Recoil);
    }

    public float flash(float Partial) {
        if (level == null) {
            return 0.0f;
        }
        float Age = level.getGameTime() + Partial - FlashTick;
        return Age < 0.0f || Age > FlashTicks ? 0.0f : 1.0f - Age / FlashTicks;
    }

    public int getFlashBarrel() {
        return FlashBarrel;
    }

    public TurretStatus getStatus() {
        return Status;
    }

    public int getTargetId() {
        return TargetId;
    }

    @Nullable
    public LivingEntity getTarget() {
        return Target;
    }

    public TurretRules getRules() {
        return Rules;
    }

    @Nullable
    public UUID getOwner() {
        return Owner;
    }

    public String getOwnerName() {
        return OwnerName;
    }

    public int getDemand() {
        return Demand;
    }

    public ItemStackHandler getAmmo() {
        return Ammo;
    }

    public IItemHandler getAutomation() {
        return Automation;
    }

    public void setOwner(Player User) {
        Owner = User.getUUID();
        OwnerName = User.getGameProfile().getName();
        changed();
    }

    public boolean isOwner(Player User) {
        return Owner != null && Owner.equals(User.getUUID());
    }

    public boolean canEdit(Player User) {
        return Owner == null || isOwner(User) || User.isCreative() && User.hasPermissions(2);
    }

    public boolean canConfigure(Player User) {
        return canEdit(User) || Rules.allowsName(User.getGameProfile().getName());
    }

    public void refuse(Player User) {
        User.displayClientMessage(Component.translatable("message.scp_unleashed.sentry_turret.locked", OwnerName).withStyle(ChatFormatting.RED), true);
    }

    public void loadFrom(Player User, ItemStack Stack) {
        ItemStack Left = Stack.copy();
        for (int Slot = 0; Slot < AmmoSlots && !Left.isEmpty(); Slot++) {
            Left = Ammo.insertItem(Slot, Left, false);
        }
        int Moved = Stack.getCount() - Left.getCount();
        if (Moved <= 0) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.sentry_turret.full").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!User.getAbilities().instabuild) {
            Stack.shrink(Moved);
        }
        if (level instanceof ServerLevel World) {
            Vec3 Pivot = pivot();
            GunSounds.play(World, null, Pivot.x, Pivot.y, Pivot.z, GunSounds.reloadFinish(GunKind.AssaultRifle), 0.8f);
        }
    }

    public boolean applyConfig(int Action, int Index, String Text) {
        boolean Done = switch (Action) {
            case TurretConfigPayload.ActionToggle -> {
                if (Index < 0 || Index >= TurretCategory.values().length) {
                    yield false;
                }
                Rules.toggle(TurretCategory.byIndex(Index));
                yield true;
            }
            case TurretConfigPayload.ActionAllow -> Rules.add(Text, false);
            case TurretConfigPayload.ActionDeny -> Rules.add(Text, true);
            case TurretConfigPayload.ActionRemove -> Rules.remove(Index);
            case TurretConfigPayload.ActionFlip -> Rules.flip(Index);
            default -> false;
        };
        if (Done) {
            if (Target != null && !TurretTargeting.isTarget(Rules, Owner, Target)) {
                dropTarget();
            }
            changed();
        }
        return Done;
    }

    private void changed() {
        setChanged();
        NeedsSync = true;
        UrgentSync = true;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            NeedsSync = false;
        }
    }

    @Nullable
    public TurretRound loadedRound() {
        for (int Slot = 0; Slot < AmmoSlots; Slot++) {
            TurretRound Round = TurretRound.of(Ammo.getStackInSlot(Slot));
            if (Round != null) {
                return Round;
            }
        }
        return null;
    }

    public int rounds() {
        int Total = 0;
        for (int Slot = 0; Slot < AmmoSlots; Slot++) {
            ItemStack Stack = Ammo.getStackInSlot(Slot);
            if (TurretRound.of(Stack) != null) {
                Total += Stack.getCount();
            }
        }
        return Total;
    }

    private void consumeRound() {
        for (int Slot = 0; Slot < AmmoSlots; Slot++) {
            ItemStack Stack = Ammo.getStackInSlot(Slot);
            if (TurretRound.of(Stack) != null) {
                Ammo.extractItem(Slot, 1, false);
                return;
            }
        }
    }

    public int comparatorSignal() {
        int Total = rounds();
        if (Total <= 0) {
            return 0;
        }
        return 1 + Math.min(14, Total * 14 / (AmmoSlots * 64));
    }

    public List<ItemStack> drops() {
        List<ItemStack> Found = new ArrayList<>();
        for (int Slot = 0; Slot < AmmoSlots; Slot++) {
            ItemStack Stack = Ammo.getStackInSlot(Slot);
            if (!Stack.isEmpty()) {
                Found.add(Stack.copy());
                Ammo.setStackInSlot(Slot, ItemStack.EMPTY);
            }
        }
        return Found;
    }

    public ContainerData getData() {
        return Data;
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.scp_unleashed.sentry_turret");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new SentryTurretMenu(ContainerId, PlayerInventory, this, Data);
    }

    @Override
    public int getPowerDemand() {
        return Demand;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Ammo", Ammo.serializeNBT(Registries));
        Tag.put("Rules", Rules.save());
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
            Tag.putString("OwnerName", OwnerName);
        }
        if (!Float.isNaN(Yaw)) {
            Tag.putFloat("Yaw", Yaw);
            Tag.putFloat("Pitch", Pitch);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Ammo")) {
            Ammo.deserializeNBT(Registries, Tag.getCompound("Ammo"));
        }
        if (Tag.contains("Rules")) {
            Rules.load(Tag.getCompound("Rules"));
        }
        if (Tag.hasUUID("Owner")) {
            Owner = Tag.getUUID("Owner");
            OwnerName = Tag.getString("OwnerName");
        }
        if (Tag.contains("Yaw") && Float.isNaN(Yaw)) {
            Yaw = Tag.getFloat("Yaw");
            Pitch = Tag.getFloat("Pitch");
            PreviousYaw = Yaw;
            PreviousPitch = Pitch;
        }
        if (Tag.contains("Status")) {
            Status = TurretStatus.byIndex(Tag.getByte("Status"));
            TargetId = Tag.getInt("Target");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Status", (byte) Status.ordinal());
        Tag.putInt("Target", TargetId);
        Tag.put("Rules", Rules.save());
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
            Tag.putString("OwnerName", OwnerName);
        }
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).inflate(1.0);
    }

    private record Hit(LivingEntity Target, Vec3 Point, double Distance) {
    }
}
