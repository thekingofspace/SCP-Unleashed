package net.rai.scp_unleashed.tesla;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.wire.WireDevice;

import java.util.function.Consumer;

public class TeslaGateBlockEntity extends BlockEntity implements WireDevice {
    public static final int ChargedDraw = 40;
    public static final int BurstDraw = 400;
    public static final int WindUpTicks = 12;
    public static final int DischargeTicks = 8;
    public static final int CooldownTicks = 60;
    public static final int RelaxTicks = 40;
    public static final int StarveTicks = 60;
    public static final int ScanInterval = 4;
    public static final int PowerGrace = 2;
    public static final int SyncTicks = 5;
    public static final float StrikeDamage = 30.0f;
    public static final double ChargeReach = 6.0;
    public static final double TriggerReach = 2.5;

    private static Consumer<TeslaGateBlockEntity> ClientEffects = Gate -> {
    };
    private static LivingEntity Shocked;

    private final IntSet Struck = new IntOpenHashSet();
    private TeslaPhase Phase = TeslaPhase.Idle;
    private TeslaStatus Status = TeslaStatus.Off;
    private int PhaseTicks;
    private int Demand;
    private int Received;
    private long LastFed = Long.MIN_VALUE / 2;
    private long StarvedUntil = Long.MIN_VALUE / 2;
    private boolean Overridden;
    private boolean RedstoneDirty = true;
    private int Calm;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private float Heat;
    private float PreviousHeat;

    public TeslaGateBlockEntity(BlockPos Pos, BlockState State) {
        super(TeslaRegistry.TeslaGateEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<TeslaGateBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, TeslaGateBlockEntity Gate) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof TeslaGateBlock) {
            Gate.tickServer(ServerWorld, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, TeslaGateBlockEntity Gate) {
        Gate.tickClient();
        ClientEffects.accept(Gate);
    }

    public static boolean isShocked(LivingEntity Target) {
        return Target == Shocked;
    }

    public static boolean isTarget(LivingEntity Target) {
        return Target.isAlive() && !Target.isSpectator() && !(Target instanceof Player User && User.isCreative()) && !(Target instanceof ArmorStand);
    }

    private void tickServer(ServerLevel World, BlockState State) {
        long Now = World.getGameTime();
        boolean Fed = Demand > 0 && Received >= Demand;
        Received = 0;
        if (Fed) {
            LastFed = Now;
        }
        if (RedstoneDirty) {
            RedstoneDirty = false;
            Overridden = redstoneOverride(World, State);
        }
        boolean Powered = Now - LastFed <= PowerGrace;
        TeslaPhase Before = Phase;
        switch (Phase) {
            case Idle -> tickIdle(World, State, Now, Powered);
            case Charged -> tickCharged(World, State, Now, Fed);
            case WindUp -> tickWindUp(World, State, Fed);
            case Discharge -> tickDischarge(World, State, Fed);
            case Cooldown -> tickCooldown(World, State, Powered);
        }
        if (Phase == Before) {
            PhaseTicks++;
        }
        TeslaStatus Next = statusFor(Now, Powered);
        if (Next != Status) {
            Status = Next;
            NeedsSync = true;
        }
        updateGlow(World);
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void tickIdle(ServerLevel World, BlockState State, long Now, boolean Powered) {
        if (!Overridden && Powered && Now >= StarvedUntil && PhaseTicks % ScanInterval == 0 && anyone(World, chargeZone(State))) {
            enter(TeslaPhase.Charged);
        }
    }

    private void tickCharged(ServerLevel World, BlockState State, long Now, boolean Fed) {
        if (Overridden) {
            enter(TeslaPhase.Idle);
            return;
        }
        if (!Fed) {
            StarvedUntil = Now + StarveTicks;
            enter(TeslaPhase.Idle);
            return;
        }
        if (anyone(World, triggerZone(State))) {
            enter(TeslaPhase.WindUp);
            play(World, State, TeslaRegistry.Charge.get(), 1.6f, 1.0f);
            return;
        }
        if (PhaseTicks % ScanInterval == 0) {
            Calm = anyone(World, chargeZone(State)) ? 0 : Calm + ScanInterval;
            if (Calm >= RelaxTicks) {
                enter(TeslaPhase.Idle);
            }
        }
    }

    private void tickWindUp(ServerLevel World, BlockState State, boolean Fed) {
        if (Overridden) {
            enter(TeslaPhase.Idle);
            return;
        }
        if (!Fed) {
            fizzle(World, State);
            enter(TeslaPhase.Cooldown);
            return;
        }
        if (PhaseTicks >= WindUpTicks - 1) {
            enter(TeslaPhase.Discharge);
        }
    }

    private void tickDischarge(ServerLevel World, BlockState State, boolean Fed) {
        if (!Fed) {
            if (PhaseTicks == 0) {
                fizzle(World, State);
            }
            enter(TeslaPhase.Cooldown);
            return;
        }
        if (PhaseTicks == 0) {
            play(World, State, TeslaRegistry.Discharge.get(), 3.0f, 0.92f + World.getRandom().nextFloat() * 0.16f);
        }
        strike(World, State);
        if (PhaseTicks >= DischargeTicks - 1) {
            enter(TeslaPhase.Cooldown);
        }
    }

    private void tickCooldown(ServerLevel World, BlockState State, boolean Powered) {
        if (PhaseTicks >= CooldownTicks - 1) {
            enter(!Overridden && Powered && anyone(World, chargeZone(State)) ? TeslaPhase.Charged : TeslaPhase.Idle);
        }
    }

    private void enter(TeslaPhase Next) {
        Phase = Next;
        PhaseTicks = 0;
        Calm = 0;
        Demand = switch (Next) {
            case Charged, WindUp -> ChargedDraw;
            case Discharge -> BurstDraw;
            default -> 0;
        };
        if (Next == TeslaPhase.Discharge) {
            Struck.clear();
        }
        NeedsSync = true;
        UrgentSync = true;
    }

    private TeslaStatus statusFor(long Now, boolean Powered) {
        if (Phase.isActive()) {
            return TeslaStatus.Armed;
        }
        if (!Powered) {
            return TeslaStatus.Off;
        }
        if (Overridden) {
            return TeslaStatus.Maintenance;
        }
        return Now < StarvedUntil ? TeslaStatus.Fault : TeslaStatus.Ready;
    }

    private void updateGlow(ServerLevel World) {
        BlockState Current = getBlockState();
        if (!(Current.getBlock() instanceof TeslaGateBlock)) {
            return;
        }
        int Wanted = switch (Phase) {
            case Discharge -> 3;
            case Charged, WindUp, Cooldown -> 2;
            default -> Status == TeslaStatus.Off ? 0 : 1;
        };
        if (Current.getValue(TeslaGateBlock.Glow) != Wanted) {
            World.setBlock(worldPosition, Current.setValue(TeslaGateBlock.Glow, Wanted), Block.UPDATE_CLIENTS);
        }
    }

    private boolean redstoneOverride(Level World, BlockState State) {
        BlockPos Origin = TeslaGateBlock.originOf(worldPosition, State);
        Direction Front = State.getValue(TeslaGateBlock.Facing);
        for (int Index = 0; Index < TeslaGateBlock.CellCount; Index++) {
            BlockPos Cell = TeslaGateBlock.cellPos(Origin, Front, Index);
            if (World.isLoaded(Cell) && World.hasNeighborSignal(Cell)) {
                return true;
            }
        }
        return false;
    }

    public void markRedstoneDirty() {
        RedstoneDirty = true;
    }

    private static boolean anyone(ServerLevel World, AABB Zone) {
        return !World.getEntitiesOfClass(LivingEntity.class, Zone, TeslaGateBlockEntity::isTarget).isEmpty();
    }

    public AABB volume(BlockState State) {
        Direction Front = State.getValue(TeslaGateBlock.Facing);
        BlockPos Origin = TeslaGateBlock.originOf(worldPosition, State);
        BlockPos First = TeslaGateBlock.cellPos(Origin, Front, TeslaGateBlock.index(0, 0, 0));
        BlockPos Last = TeslaGateBlock.cellPos(Origin, Front, TeslaGateBlock.index(TeslaGateBlock.Width - 1, TeslaGateBlock.Height - 1, TeslaGateBlock.Depth - 1));
        return new AABB(First).minmax(new AABB(Last));
    }

    public AABB chargeZone(BlockState State) {
        return TeslaGeometry.corridorZone(TeslaGateBlock.originOf(worldPosition, State), State.getValue(TeslaGateBlock.Facing), 0.5, ChargeReach);
    }

    public AABB triggerZone(BlockState State) {
        return TeslaGeometry.corridorZone(TeslaGateBlock.originOf(worldPosition, State), State.getValue(TeslaGateBlock.Facing), 0.25, TriggerReach);
    }

    public AABB strikeZone(BlockState State) {
        return TeslaGeometry.strikeZone(TeslaGateBlock.originOf(worldPosition, State), State.getValue(TeslaGateBlock.Facing));
    }

    private Vec3 centre(BlockState State) {
        return volume(State).getCenter();
    }

    private void strike(ServerLevel World, BlockState State) {
        Direction Away = TeslaGeometry.away(State.getValue(TeslaGateBlock.Facing));
        Vec3 Middle = centre(State);
        AABB Zone = strikeZone(State);
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, Zone, TeslaGateBlockEntity::isTarget)) {
            if (!TeslaGeometry.struck(Zone, Target.getBoundingBox()) || !Struck.add(Target.getId())) {
                continue;
            }
            double Offset = (Target.getX() - Middle.x) * Away.getStepX() + (Target.getZ() - Middle.z) * Away.getStepZ();
            Vec3 Plane = new Vec3(Target.getX() - Offset * Away.getStepX(), Target.getY(), Target.getZ() - Offset * Away.getStepZ());
            DamageSource Source = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TeslaRegistry.TeslaDamage), Plane);
            Shocked = Target;
            try {
                Target.hurt(Source, StrikeDamage);
            } finally {
                Shocked = null;
            }
            World.sendParticles(TeslaRegistry.Spark.get(), Target.getX(), Target.getY(0.5), Target.getZ(), 14, Target.getBbWidth() * 0.4, Target.getBbHeight() * 0.35, Target.getBbWidth() * 0.4, 0.25);
        }
    }

    private void fizzle(ServerLevel World, BlockState State) {
        Vec3 Middle = centre(State);
        World.playSound(null, Middle.x, Middle.y, Middle.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.7f);
        World.sendParticles(ParticleTypes.SMOKE, Middle.x, Middle.y + 0.6, Middle.z, 8, 0.8, 0.6, 0.6, 0.01);
        World.sendParticles(TeslaRegistry.Spark.get(), Middle.x, Middle.y + 0.6, Middle.z, 10, 0.9, 0.7, 0.7, 0.1);
    }

    private void play(ServerLevel World, BlockState State, SoundEvent Sound, float Volume, float Pitch) {
        Vec3 Middle = centre(State);
        World.playSound(null, Middle.x, Middle.y, Middle.z, Sound, SoundSource.BLOCKS, Volume, Pitch);
    }

    private void tickClient() {
        PhaseTicks++;
        PreviousHeat = Heat;
        float Target = switch (Phase) {
            case Charged -> 0.5f;
            case WindUp -> 0.55f + 0.45f * windUpProgress(0.0f);
            case Discharge -> 1.0f;
            default -> 0.0f;
        };
        float Rate = Target > Heat ? 0.3f : Phase == TeslaPhase.Cooldown ? 0.06f : 0.12f;
        Heat += (Target - Heat) * Rate;
    }

    public TeslaPhase getPhase() {
        return Phase;
    }

    public TeslaStatus getStatus() {
        return Status;
    }

    public int getPhaseTicks() {
        return PhaseTicks;
    }

    public float getHeat(float PartialTick) {
        return PreviousHeat + (Heat - PreviousHeat) * PartialTick;
    }

    public float windUpProgress(float PartialTick) {
        if (Phase == TeslaPhase.Discharge) {
            return 1.0f;
        }
        return Phase == TeslaPhase.WindUp ? Math.min(1.0f, (PhaseTicks + PartialTick) / WindUpTicks) : 0.0f;
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.hasProperty(TeslaGateBlock.Facing) ? State.getValue(TeslaGateBlock.Facing) : Direction.NORTH;
    }

    public BlockPos getOrigin() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof TeslaGateBlock ? TeslaGateBlock.originOf(worldPosition, State) : worldPosition;
    }

    public AABB getRenderBounds() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof TeslaGateBlock ? volume(State).inflate(0.5) : new AABB(worldPosition).inflate(2.0);
    }

    public int getDemand() {
        return Demand;
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
    public void observeNetwork(long Available) {
        if (Demand <= 0 && Available > 0 && level != null) {
            LastFed = level.getGameTime();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Phase")) {
            TeslaPhase Next = TeslaPhase.byIndex(Tag.getByte("Phase"));
            if (Next != Phase && Next == TeslaPhase.Discharge) {
                Heat = 1.0f;
            }
            Phase = Next;
            Status = TeslaStatus.byIndex(Tag.getByte("Status"));
            PhaseTicks = Tag.getInt("PhaseTicks");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Phase", (byte) Phase.ordinal());
        Tag.putByte("Status", (byte) Status.ordinal());
        Tag.putInt("PhaseTicks", PhaseTicks);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
