package net.rai.scp_unleashed.instability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class InstabilityReactorBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int Capacity = 240000;
    public static final int StoredScale = 10;
    public static final int ActiveHold = 40;
    public static final int SyncTicks = 10;
    public static final int SurgeEvent = 1;
    public static final int DataCount = 7;
    public static final int DataBurnSeconds = 0;
    public static final int DataTotalSeconds = 1;
    public static final int DataOutput = 2;
    public static final int DataStability = 3;
    public static final int DataGrade = 4;
    public static final int DataStored = 5;
    public static final int DataStatus = 6;
    public static final float DriftPull = 0.012f;
    public static final float DriftNoise = 0.06f;
    public static final float SurgeDecay = 0.93f;
    public static final float SurgeKick = 0.6f;
    public static final float FlickerDecay = 0.86f;
    public static final float OpenRate = 0.022f;
    public static final float CloseRate = 0.012f;
    public static final float IdleOpen = 0.72f;
    public static final float CollapseTicks = 34.0f;
    public static final float RingSpeed = 4.2f;
    public static final float SpinRate = 0.02f;
    public static final float IdleSpin = 0.3f;
    public static final float TintRate = 0.06f;
    public static final float CalmRate = 0.1f;
    public static final double CoreHeight = 22.5 / 16.0;

    private static Consumer<InstabilityReactorBlockEntity> ClientEffects = Reactor -> {
    };

    private final ItemStackHandler Fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return ReactorFuel.isFuel(Stack);
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
        }
    };
    private final ReactorFuelHandler Feed = new ReactorFuelHandler(this);

    @Nullable
    private ReactorFuel Burning;
    private ReactorFuel LastFuel = ReactorFuel.Faint;
    private int BurnTime;
    private int BurnTotal;
    private int Stored;
    private float Carry;
    private int Output;
    private float Drift;
    private float Surge;
    private float SurgeSign = 1.0f;
    private float Stability = 1.0f;
    private long LastBurned = Long.MIN_VALUE / 2;
    private float BurnDebt;
    private int SyncedGrade = -1;
    private int SyncedStability = -1;
    private long LastSync = Long.MIN_VALUE / 2;

    private int ShownGrade;
    private float ShownStability = 1.0f;
    private boolean Primed;
    private ReactorStatus SeenStatus = ReactorStatus.Off;
    private float Open;
    private float PreviousOpen;
    private float Collapse;
    private float PreviousCollapse;
    private boolean Collapsing;
    private float Spin;
    private double RingPhase;
    private double PreviousRingPhase;
    private float TintRed = 1.0f;
    private float TintGreen = 1.0f;
    private float TintBlue = 1.0f;
    private float EasedGrade = 1.0f;
    private float EasedCalm = 1.0f;
    private float Flicker;
    private int FlickerTier;
    private int PendingSurge;
    private int PendingChange;
    private long Clock;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataBurnSeconds -> (BurnTime + 19) / 20;
                case DataTotalSeconds -> (BurnTotal + 19) / 20;
                case DataOutput -> Output;
                case DataStability -> Math.round(Stability * 1000.0f);
                case DataGrade -> Burning == null ? 0 : Burning.getTier();
                case DataStored -> Stored / StoredScale;
                case DataStatus -> getBlockState().getValue(InstabilityReactorBlock.Status).ordinal();
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

    public InstabilityReactorBlockEntity(BlockPos Pos, BlockState State) {
        super(InstabilityRegistry.ReactorEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<InstabilityReactorBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, InstabilityReactorBlockEntity Reactor) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof InstabilityReactorBlock && State.getValue(InstabilityReactorBlock.Status) != ReactorStatus.Off) {
            InstabilityReactorBlock.setStatus(ServerWorld, InstabilityReactorBlock.originOf(Pos, State), State, ReactorStatus.Off);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, InstabilityReactorBlockEntity Reactor) {
        if (State.getBlock() instanceof InstabilityReactorBlock) {
            Reactor.tickClient(State);
            ClientEffects.accept(Reactor);
        }
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        RandomSource Random = World.getRandom();
        if (BurnTime <= 0) {
            Burning = null;
            ReactorFuel Next = ReactorFuel.of(Fuel.getStackInSlot(0));
            if (Next != null && Stored < Capacity) {
                Fuel.extractItem(0, 1, false);
                Burning = Next;
                LastFuel = Next;
                BurnTime = Next.getBurnTicks();
                BurnTotal = BurnTime;
                setChanged();
            }
        }
        Output = 0;
        if (Burning != null) {
            float Factor = 1.0f + Burning.getSwing() * (Drift + Surge * SurgeSign * SurgeKick);
            float Gain = Burning.getOutput() * Factor + Carry;
            int Whole = (int) Gain;
            int Taken = Math.min(Whole, Capacity - Stored);
            if (Taken > 0) {
                Carry = Taken == Whole ? Gain - Whole : 0.0f;
                Stored += Taken;
                Output = Taken;
                BurnDebt += (float) Taken / Whole;
                while (BurnDebt >= 1.0f && BurnTime > 0) {
                    BurnTime--;
                    BurnDebt -= 1.0f;
                }
                LastBurned = Now;
                evolve(World, Pos, State, Random, Burning);
                setChanged();
            } else {
                settle(Burning.getCalm());
            }
        } else {
            settle(1.0f);
        }
        ReactorStatus Next = Now - LastBurned <= ActiveHold ? ReactorStatus.Running
                : Burning != null || ReactorFuel.isFuel(Fuel.getStackInSlot(0)) ? ReactorStatus.Idle : ReactorStatus.Off;
        if (State.getValue(InstabilityReactorBlock.Status) != Next) {
            InstabilityReactorBlock.setStatus(World, InstabilityReactorBlock.originOf(Pos, State), State, Next);
        }
        sync(World, Now);
    }

    private void evolve(ServerLevel World, BlockPos Pos, BlockState State, RandomSource Random, ReactorFuel Source) {
        Drift = Mth.clamp(Drift - Drift * DriftPull + (Random.nextFloat() * 2.0f - 1.0f) * DriftNoise, -1.0f, 1.0f);
        Surge *= SurgeDecay;
        if (Random.nextInt(Source.getSurgeInterval()) == 0) {
            Surge = 1.0f;
            SurgeSign = Random.nextBoolean() ? 1.0f : -1.0f;
            World.blockEvent(Pos, State.getBlock(), SurgeEvent, Source.getTier());
        }
        float SurgeShake = 0.08f + 0.03f * Source.getTier();
        float Target = Source.getCalm() - Source.getSwing() * 2.0f * Math.abs(Drift) - SurgeShake * Surge;
        Stability = Mth.clamp(Stability + (Target - Stability) * 0.25f, 0.0f, 1.0f);
    }

    private void settle(float Calm) {
        Drift *= 0.97f;
        Surge *= SurgeDecay;
        Stability += (Calm - Stability) * 0.05f;
    }

    private void sync(ServerLevel World, long Now) {
        int Grade = Burning == null ? 0 : Burning.getTier();
        int Percent = Math.round(Stability * 100.0f);
        boolean Urgent = Grade != SyncedGrade;
        boolean Drifted = Math.abs(Percent - SyncedStability) >= 2 && Now - LastSync >= SyncTicks;
        if (Urgent || Drifted) {
            SyncedGrade = Grade;
            SyncedStability = Percent;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean triggerEvent(int Id, int Param) {
        if (Id == SurgeEvent) {
            if (level != null && level.isClientSide) {
                Flicker = 1.0f;
                FlickerTier = Mth.clamp(Param, 1, 5);
                PendingSurge = FlickerTier;
            }
            return true;
        }
        return super.triggerEvent(Id, Param);
    }

    private void tickClient(BlockState State) {
        ReactorStatus Shown = State.getValue(InstabilityReactorBlock.Status);
        float Target = Shown == ReactorStatus.Running ? 1.0f : Shown == ReactorStatus.Idle ? IdleOpen : 0.0f;
        float SpinTarget = Shown == ReactorStatus.Running ? 1.0f : Shown == ReactorStatus.Idle ? IdleSpin : 0.0f;
        int Rgb = FragmentRarity.byTier(getVisualGrade()).getColour();
        float Red = ((Rgb >> 16) & 255) / 255.0f;
        float Green = ((Rgb >> 8) & 255) / 255.0f;
        float Blue = (Rgb & 255) / 255.0f;
        if (!Primed) {
            Primed = true;
            long Now = level == null ? 0L : level.getGameTime();
            SeenStatus = Shown;
            Open = Target;
            PreviousOpen = Target;
            Spin = SpinTarget;
            Clock = Now;
            RingPhase = Now * (double) SpinTarget;
            PreviousRingPhase = RingPhase;
            TintRed = Red;
            TintGreen = Green;
            TintBlue = Blue;
            EasedGrade = getVisualGrade();
            EasedCalm = ShownStability;
            return;
        }
        Clock++;
        PreviousOpen = Open;
        PreviousCollapse = Collapse;
        PreviousRingPhase = RingPhase;
        if (Shown != SeenStatus) {
            if (!Shown.isLit() && SeenStatus.isLit()) {
                Collapsing = true;
                Collapse = 0.0f;
                PendingChange = -1;
            } else if (Shown.isLit() && !SeenStatus.isLit()) {
                if (Collapsing) {
                    Open *= 1.0f - Collapse;
                    PreviousOpen = Open;
                }
                Collapsing = false;
                Collapse = 0.0f;
                PreviousCollapse = 0.0f;
                PendingChange = 1;
            }
            SeenStatus = Shown;
        }
        if (Collapsing) {
            Collapse = Math.min(1.0f, Collapse + 1.0f / CollapseTicks);
            if (Collapse >= 1.0f) {
                Collapsing = false;
                Collapse = 0.0f;
                PreviousCollapse = 0.0f;
                Open = 0.0f;
                PreviousOpen = 0.0f;
            }
        } else if (Open < Target) {
            Open = Math.min(Target, Open + OpenRate);
        } else if (Open > Target) {
            Open = Math.max(Target, Open - CloseRate);
        }
        Spin += Mth.clamp(SpinTarget - Spin, -SpinRate, SpinRate);
        RingPhase += Spin;
        TintRed += (Red - TintRed) * TintRate;
        TintGreen += (Green - TintGreen) * TintRate;
        TintBlue += (Blue - TintBlue) * TintRate;
        EasedGrade += (getVisualGrade() - EasedGrade) * TintRate;
        EasedCalm += (ShownStability - EasedCalm) * CalmRate;
        Flicker *= FlickerDecay;
    }

    public int takePendingSurge() {
        int Taken = PendingSurge;
        PendingSurge = 0;
        return Taken;
    }

    public int takePendingChange() {
        int Taken = PendingChange;
        PendingChange = 0;
        return Taken;
    }

    public ReactorStatus getStatus() {
        return getBlockState().getValue(InstabilityReactorBlock.Status);
    }

    public Direction getFront() {
        return getBlockState().getValue(InstabilityReactorBlock.Facing);
    }

    public BlockPos getOrigin() {
        return InstabilityReactorBlock.originOf(worldPosition, getBlockState());
    }

    public Vec3 getCoreCentre() {
        Direction Front = getFront();
        BlockPos Origin = getOrigin();
        BlockPos Far = InstabilityReactorBlock.cellPos(Origin, Front, InstabilityReactorBlock.index(0, 1, 1));
        return new Vec3((Math.min(Origin.getX(), Far.getX()) + Math.max(Origin.getX(), Far.getX()) + 1) * 0.5,
                Origin.getY() + CoreHeight,
                (Math.min(Origin.getZ(), Far.getZ()) + Math.max(Origin.getZ(), Far.getZ()) + 1) * 0.5);
    }

    public AABB getRenderBounds() {
        AABB Box = new AABB(worldPosition);
        for (BlockPos Cell : InstabilityReactorBlock.cells(getOrigin(), getFront())) {
            Box = Box.minmax(new AABB(Cell));
        }
        return Box.inflate(0.5);
    }

    public int getShownGrade() {
        return ShownGrade;
    }

    public int getVisualGrade() {
        return ShownGrade > 0 ? ShownGrade : FlickerTier > 0 ? FlickerTier : 1;
    }

    public float getShownStability() {
        return ShownStability;
    }

    public float getOpen(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousOpen, Open);
    }

    public float getCollapse(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousCollapse, Collapse);
    }

    public double getRingPhase(float PartialTick) {
        return PreviousRingPhase + (RingPhase - PreviousRingPhase) * PartialTick;
    }

    public double getTime(float PartialTick) {
        return Clock + (double) PartialTick;
    }

    public int getTint() {
        return (Math.round(Mth.clamp(TintRed, 0.0f, 1.0f) * 255.0f) << 16) | (Math.round(Mth.clamp(TintGreen, 0.0f, 1.0f) * 255.0f) << 8)
                | Math.round(Mth.clamp(TintBlue, 0.0f, 1.0f) * 255.0f);
    }

    public float getEasedGrade() {
        return EasedGrade;
    }

    public float getEasedCalm() {
        return EasedCalm;
    }

    public float getSpin() {
        return Spin;
    }

    public float getFlicker() {
        return Flicker;
    }

    public long getClock() {
        return Clock;
    }

    public ItemStackHandler getFuel() {
        return Fuel;
    }

    public ReactorFuelHandler getFeed() {
        return Feed;
    }

    @Nullable
    public ReactorFuel getBurning() {
        return Burning;
    }

    public int getBurnTime() {
        return BurnTime;
    }

    public int getBurnTotal() {
        return BurnTotal;
    }

    public float getStability() {
        return Stability;
    }

    public int getOutput() {
        return Output;
    }

    public void dropContents(Level World, BlockPos Pos) {
        Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Fuel.getStackInSlot(0).copy());
        Fuel.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Fuel", Fuel.serializeNBT(Registries));
        Tag.putInt("Burning", Burning == null ? 0 : Burning.getTier());
        Tag.putInt("LastFuel", LastFuel.getTier());
        Tag.putInt("BurnTime", BurnTime);
        Tag.putInt("BurnTotal", BurnTotal);
        Tag.putInt("Stored", Stored);
        Tag.putFloat("Carry", Carry);
        Tag.putFloat("Drift", Drift);
        Tag.putFloat("Stability", Stability);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Fuel")) {
            Fuel.deserializeNBT(Registries, Tag.getCompound("Fuel"));
        }
        if (Tag.contains("BurnTime")) {
            Burning = ReactorFuel.byTier(Tag.getInt("Burning"));
            ReactorFuel Previous = ReactorFuel.byTier(Tag.getInt("LastFuel"));
            LastFuel = Previous == null ? ReactorFuel.Faint : Previous;
            BurnTime = Burning == null ? 0 : Mth.clamp(Tag.getInt("BurnTime"), 0, Burning.getBurnTicks());
            BurnTotal = Burning == null ? 0 : Mth.clamp(Tag.getInt("BurnTotal"), BurnTime, Burning.getBurnTicks());
            Stored = 0;
            Carry = Mth.clamp(Tag.getFloat("Carry"), 0.0f, 1.0f);
            Drift = Mth.clamp(Tag.getFloat("Drift"), -1.0f, 1.0f);
            Stability = Mth.clamp(Tag.getFloat("Stability"), 0.0f, 1.0f);
        }
        if (Tag.contains("Grade")) {
            ShownGrade = Mth.clamp(Tag.getByte("Grade"), 0, 5);
            ShownStability = Mth.clamp(Tag.getByte("Calm") / 100.0f, 0.0f, 1.0f);
            if (ShownGrade > 0) {
                FlickerTier = ShownGrade;
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Grade", (byte) (Burning == null ? 0 : Burning.getTier()));
        Tag.putByte("Calm", (byte) Math.round(Stability * 100.0f));
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.instability_reactor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new InstabilityMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        Vec3 Centre = getCoreCentre();
        return User.distanceToSqr(Centre.x, Centre.y, Centre.z) <= 81.0;
    }
}
