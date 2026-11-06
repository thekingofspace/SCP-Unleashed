package net.rai.scp_unleashed.fission;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.wire.WireDevice;
import net.rai.scp_unleashed.discord.DiscordNotices;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class FissionReactorBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int SlotCount = 9;
    public static final int FirstFuel = 0;
    public static final int FuelSlots = 4;
    public static final int FirstSpent = 4;
    public static final int SpentSlots = 2;
    public static final int IrradiateInput = 6;
    public static final int IrradiateOutput = 7;
    public static final int ActiveRod = 8;
    public static final int WasteCapacity = 8000;
    public static final float WastePerTick = 6.0f;
    public static final int GasCapacity = 1200;
    public static final float GasPerTick = 2.0f;
    public static final float GasLeakStart = 0.85f;
    public static final float GasLeakShare = 0.6f;
    public static final int VentPerCycle = 60;
    public static final double LeakReach = 5.3;
    public static final double LeakHeight = 2.2;
    public static final int BufferCapacity = 100000;
    public static final int MaxOffer = 4000;
    public static final float ThrottleBand = 0.1f;
    public static final float RampUp = 0.0025f;
    public static final float RampDown = 0.01f;
    public static final float ScramDrop = 0.08f;
    public static final float PassiveCooling = 0.00022f;
    public static final float MeltdownPower = 15.0f;
    public static final int GasInterval = 10;
    public static final int SyncTicks = 10;
    public static final int SaveTicks = 40;
    public static final int MinLimit = 10;
    public static final int MaxLimit = 100;
    public static final int LimitStep = 10;
    public static final int ButtonToggle = 0;
    public static final int ButtonLower = 1;
    public static final int ButtonRaise = 2;
    public static final int VentSealed = 0;
    public static final int VentOpen = 1;
    public static final int VentLeaking = 2;
    public static final int VentDissolved = 3;
    public static final int MaxSettle = 9999;
    public static final int ScramClunks = 4;
    public static final int ClunkGap = 3;
    public static final int HissOdds = 10;
    public static final int DataPower = 0;
    public static final int DataLimit = 1;
    public static final int DataTemperature = 2;
    public static final int DataRunning = 3;
    public static final int DataStatus = 4;
    public static final int DataRod = 5;
    public static final int DataOutput = 6;
    public static final int DataWaste = 7;
    public static final int DataGas = 8;
    public static final int DataExchangers = 9;
    public static final int DataFlowing = 10;
    public static final int DataIce = 11;
    public static final int DataCooling = 12;
    public static final int DataHeating = 13;
    public static final int DataSettle = 14;
    public static final int DataIrradiation = 15;
    public static final int DataVenting = 16;
    public static final int DataMaxOutput = 17;
    public static final int DataRodMinutes = 18;
    public static final int DataFlow = 19;
    public static final int DataCount = 20;

    private static Consumer<FissionReactorBlockEntity> ClientEffects = Reactor -> {
    };

    private final ItemStackHandler Contents = new ItemStackHandler(SlotCount) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            if (Slot >= FirstFuel && Slot < FirstFuel + FuelSlots) {
                return ReactorFuel.isFuel(Stack);
            }
            return Slot == IrradiateInput && isIrradiatable(level, Stack);
        }

        @Override
        public int getSlotLimit(int Slot) {
            return Slot == ActiveRod ? 1 : super.getSlotLimit(Slot);
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
            if (Slot == IrradiateInput) {
                RecipeDirty = true;
            }
        }
    };
    private final FluidTank Waste = new FluidTank(WasteCapacity, Stack -> Stack.is(FissionTags.WasteWater)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private boolean Running;
    private int PowerLimit = MaxLimit;
    private float Power;
    private float Temperature = ReactorThermal.Ambient;
    private float BurnDone;
    private int BurnTotal;
    private float Gas;
    private float WasteCarry;
    private int Stored;
    private int Generation;
    private float IrradiationDone;
    @Nullable
    private RecipeManager KnownRecipes;
    @Nullable
    private RecipeHolder<IrradiatingRecipe> CurrentRecipe;
    @Nullable
    private ResourceLocation CurrentRecipeId;
    private boolean RecipeDirty = true;
    private FissionStatus Status = FissionStatus.Offline;
    private boolean Interlocked;
    private boolean RedstoneDirty = true;
    private ReactorCooling.Survey Cooling = ReactorCooling.Survey.Empty;
    private float LastHeating;
    private float LastCooling;
    private float TargetPower;
    private int Venting = VentSealed;
    private float Leaked;
    private int Comparator = -1;
    private int PendingClunks;
    private long NextClunk;
    private boolean NeedsSync = true;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private int LastSyncedPower = -1;
    private int LastSyncedHeat = -1;
    private int LastSyncedLeak = -1;
    private int LastSyncedWaste = -1;
    private int LastSyncedGas = -1;
    private int ClientWaste;
    private int ClientGas;
    private int ClientLeak;
    private float ShownPower;
    private float PreviousShownPower;
    private float ShownHeat = ReactorThermal.Ambient;
    private float PreviousShownHeat = ReactorThermal.Ambient;
    private boolean Primed;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataPower -> Math.round(Power * 1000.0f);
                case DataLimit -> PowerLimit;
                case DataTemperature -> Math.round(Temperature);
                case DataRunning -> Running ? 1 : 0;
                case DataStatus -> Status.ordinal();
                case DataRod -> rodRemaining();
                case DataOutput -> Generation;
                case DataWaste -> Waste.getFluidAmount();
                case DataGas -> Math.round(Gas);
                case DataExchangers -> Cooling.Exchangers();
                case DataFlowing -> Cooling.Flowing();
                case DataIce -> Cooling.Ice();
                case DataCooling -> Math.round(LastCooling * 200.0f);
                case DataHeating -> Math.round(LastHeating * 200.0f);
                case DataSettle -> settleTemperature();
                case DataIrradiation -> irradiationPermille();
                case DataVenting -> Venting;
                case DataMaxOutput -> activeFuel() == null ? 0 : activeFuel().Power();
                case DataRodMinutes -> rodMinutes();
                case DataFlow -> Math.round(Cooling.Flow() * 100.0f);
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

    public FissionReactorBlockEntity(BlockPos Pos, BlockState State) {
        super(FissionRegistry.ReactorEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<FissionReactorBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static boolean isIrradiatable(@Nullable Level World, ItemStack Stack) {
        if (World == null || Stack.isEmpty()) {
            return false;
        }
        return World.getRecipeManager().getRecipeFor(FissionRegistry.Irradiating.get(), new SingleRecipeInput(Stack), World).isPresent();
    }

    public ItemStackHandler getContents() {
        return Contents;
    }

    public FluidTank getWasteTank() {
        return Waste;
    }

    public Direction getFacing() {
        return getBlockState().getValue(FissionReactorBlock.Facing);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, FissionReactorBlockEntity Reactor) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof FissionReactorBlock) {
            Reactor.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, FissionReactorBlockEntity Reactor) {
        Reactor.tickClient();
        ClientEffects.accept(Reactor);
    }

    private static boolean footprintLoaded(Level World, BlockPos Origin) {
        for (int Dx = 0; Dx <= ReactorLayout.Last; Dx += ReactorLayout.Last) {
            for (int Dz = 0; Dz <= ReactorLayout.Last; Dz += ReactorLayout.Last) {
                if (!World.isLoaded(Origin.offset(Dx, 0, Dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private void tickServer(ServerLevel World, BlockPos Origin, BlockState State) {
        if (!footprintLoaded(World, Origin)) {
            return;
        }
        long Now = World.getGameTime();
        Direction Facing = State.getValue(FissionReactorBlock.Facing);
        if (RedstoneDirty) {
            RedstoneDirty = false;
            refreshRedstone(World, Origin);
        }
        if (Interlocked && Running) {
            scram(World, Origin);
        }
        if (PendingClunks > 0 && Now >= NextClunk) {
            Vec3 Centre = ReactorLayout.centre(Origin);
            World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorClunk.get(), SoundSource.BLOCKS, 1.4f, 0.85f + 0.06f * (ScramClunks - PendingClunks));
            PendingClunks--;
            NextClunk = Now + ClunkGap;
        }
        if (Now % ReactorCooling.Interval == 0) {
            Cooling = ReactorCooling.survey(World, Origin, Temperature);
        }
        if (Running && !Interlocked && Contents.getStackInSlot(ActiveRod).isEmpty()) {
            loadRod();
        }
        ReactorFuel Fuel = activeFuel();
        boolean Spent = Fuel != null && BurnDone >= BurnTotal;
        if (Spent) {
            finishRod();
            Fuel = activeFuel();
            Spent = Fuel != null && BurnDone >= BurnTotal;
        }
        float WasteThrottle = throttle(Waste.getFluidAmount(), WasteCapacity);
        float LoadThrottle = throttle(Stored, BufferCapacity);
        boolean CanRun = Running && !Interlocked && Fuel != null && !Spent;
        TargetPower = CanRun ? PowerLimit / 100.0f * Math.min(WasteThrottle, LoadThrottle) : 0.0f;
        FissionStatus Previous = Status;
        Status = statusFor(Fuel, Spent, WasteThrottle, LoadThrottle);
        if (TargetPower > Power) {
            Power = Math.min(TargetPower, Power + RampUp);
        } else if (TargetPower < Power) {
            Power = Math.max(TargetPower, Power - (CanRun ? RampDown : ScramDrop));
        }
        float Heating = 0.0f;
        Generation = 0;
        if (Fuel != null && !Spent && Power > 0.0f) {
            BurnDone = Math.min(BurnTotal, BurnDone + Power);
            Generation = Math.round(Fuel.Power() * Power);
            Stored = Math.min(BufferCapacity, Stored + Generation);
            Heating = Fuel.Heat() * Power;
            produceWaste();
            Gas = Math.min(GasCapacity, Gas + GasPerTick * Power);
        }
        float Removed = coolingConstant() * (Temperature - ReactorThermal.Ambient);
        Temperature = Math.max(ReactorThermal.Ambient, Temperature + Heating - Removed);
        LastHeating = Heating;
        LastCooling = Removed;
        if (Now % GasInterval == 0) {
            handleGas(World, Origin, Facing);
        }
        tickIrradiation(World);
        if (Temperature >= ReactorThermal.MeltdownLevel) {
            meltdown(World, Origin);
            return;
        }
        updateComparator(World, Origin);
        if (Previous != Status) {
            UrgentSync = true;
            NeedsSync = true;
        }
        if (Now % SaveTicks == 0) {
            setChanged();
        }
        sync(World, Now);
    }

    private static float throttle(int Amount, int Capacity) {
        float Band = Capacity * ThrottleBand;
        return Mth.clamp((Capacity - Amount) / Band, 0.0f, 1.0f);
    }

    private FissionStatus statusFor(@Nullable ReactorFuel Fuel, boolean Spent, float WasteThrottle, float LoadThrottle) {
        if (Interlocked) {
            return FissionStatus.Interlocked;
        }
        if (!Running) {
            return FissionStatus.Offline;
        }
        if (Fuel == null) {
            return FissionStatus.NoFuel;
        }
        if (Spent) {
            return FissionStatus.SpentFull;
        }
        if (WasteThrottle < 1.0f) {
            return FissionStatus.WasteFull;
        }
        return LoadThrottle < 1.0f ? FissionStatus.LoadFollowing : FissionStatus.Running;
    }

    private float coolingConstant() {
        return PassiveCooling + Cooling.total();
    }

    private void produceWaste() {
        float Amount = WastePerTick * Power + WasteCarry;
        int Whole = (int) Amount;
        WasteCarry = Amount - Whole;
        if (Whole > 0) {
            Waste.fill(new FluidStack(FissionRegistry.WasteWater.get(), Whole), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Nullable
    private ReactorFuel activeFuel() {
        ItemStack Rod = Contents.getStackInSlot(ActiveRod);
        return Rod.isEmpty() ? null : ReactorFuel.of(Rod);
    }

    private void loadRod() {
        for (int Slot = FirstFuel; Slot < FirstFuel + FuelSlots; Slot++) {
            ItemStack Stack = Contents.getStackInSlot(Slot);
            if (!ReactorFuel.isFuel(Stack)) {
                continue;
            }
            ItemStack Rod = Stack.copyWithCount(1);
            Contents.extractItem(Slot, 1, false);
            Contents.setStackInSlot(ActiveRod, Rod);
            BurnTotal = ReactorFuel.of(Rod).BurnTicks();
            BurnDone = 0.0f;
            NeedsSync = true;
            return;
        }
    }

    private void finishRod() {
        ItemStack Depleted = ReactorFuel.depletedRod();
        if (!Depleted.isEmpty()) {
            if (!placeOutput(FirstSpent, SpentSlots, Depleted, true)) {
                return;
            }
            placeOutput(FirstSpent, SpentSlots, Depleted, false);
        }
        Contents.setStackInSlot(ActiveRod, ItemStack.EMPTY);
        BurnDone = 0.0f;
        BurnTotal = 0;
        if (Running && !Interlocked) {
            loadRod();
        }
    }

    private boolean placeOutput(int First, int Count, ItemStack Offered, boolean Simulate) {
        ItemStack Remaining = Offered.copy();
        for (int Pass = 0; Pass < 2 && !Remaining.isEmpty(); Pass++) {
            for (int Slot = First; Slot < First + Count && !Remaining.isEmpty(); Slot++) {
                ItemStack Held = Contents.getStackInSlot(Slot);
                boolean Candidate = Pass == 0 ? !Held.isEmpty() && ItemStack.isSameItemSameComponents(Held, Remaining) : Held.isEmpty();
                if (!Candidate) {
                    continue;
                }
                int Room = Math.min(Contents.getSlotLimit(Slot), Remaining.getMaxStackSize()) - Held.getCount();
                int Moved = Math.min(Room, Remaining.getCount());
                if (Moved <= 0) {
                    continue;
                }
                if (!Simulate) {
                    Contents.setStackInSlot(Slot, Held.isEmpty() ? Remaining.copyWithCount(Moved) : Held.copyWithCount(Held.getCount() + Moved));
                }
                Remaining.shrink(Moved);
            }
        }
        return Remaining.isEmpty();
    }

    private void handleGas(ServerLevel World, BlockPos Origin, Direction Facing) {
        ReactorExhaust.Exhaust Exhaust = ReactorExhaust.find(World, ReactorLayout.gasPorts(Origin, Facing));
        float Vented = 0.0f;
        float Released = 0.0f;
        if (Exhaust.isVenting() && Gas >= 1.0f) {
            int Batch = (int) Math.min(Gas, VentPerCycle);
            Vented = Exhaust.deliver(World, new GasStack(GasRegistry.ToxicGas.get(), Batch));
            float Rest = Batch - Vented;
            if (Rest > 0.0f && !Exhaust.Mouths().isEmpty()) {
                float Share = Rest / Exhaust.Mouths().size();
                for (Vec3 Mouth : Exhaust.Mouths()) {
                    ToxicGasCloud.feed(World, Mouth, Share);
                }
                Released = Rest;
                Vented += Rest;
            }
            Gas -= Vented;
        }
        float Start = GasCapacity * GasLeakStart;
        Leaked = 0.0f;
        if (Gas > Start) {
            Leaked = (Gas - Start) * GasLeakShare;
            Gas -= Leaked;
            Vec3 Centre = ReactorLayout.centre(Origin);
            List<Vec3> Openings = new ArrayList<>();
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                Vec3 Point = new Vec3(Centre.x + Side.getStepX() * LeakReach, Origin.getY() + LeakHeight, Centre.z + Side.getStepZ() * LeakReach);
                BlockPos Spot = BlockPos.containing(Point);
                if (World.isLoaded(Spot) && !World.getBlockState(Spot).isCollisionShapeFullBlock(World, Spot)) {
                    Openings.add(Point);
                }
            }
            if (Openings.isEmpty()) {
                Openings.add(new Vec3(Centre.x, Origin.getY() + ReactorLayout.Height + 1.0, Centre.z));
            }
            float Share = Leaked / Openings.size();
            for (Vec3 Point : Openings) {
                ToxicGasCloud.feed(World, Point, Share);
            }
            if (World.getRandom().nextInt(HissOdds) == 0) {
                Vec3 Spot = Openings.get(World.getRandom().nextInt(Openings.size()));
                World.playSound(null, Spot.x, Spot.y, Spot.z, FissionRegistry.GasHiss.get(), SoundSource.BLOCKS, 0.5f, 0.9f + World.getRandom().nextFloat() * 0.2f);
            }
        }
        if (Leaked > 0.0f) {
            Venting = VentLeaking;
        } else if (Released > 0.0f) {
            Venting = VentOpen;
        } else if (Exhaust.hasSinks()) {
            Venting = VentDissolved;
        } else {
            Venting = Vented > 0.0f || Exhaust.isVenting() ? VentOpen : VentSealed;
        }
    }

    private void tickIrradiation(ServerLevel World) {
        RecipeManager Recipes = World.getRecipeManager();
        if (Recipes != KnownRecipes) {
            KnownRecipes = Recipes;
            CurrentRecipe = null;
            RecipeDirty = true;
        }
        if (RecipeDirty) {
            RecipeDirty = false;
            refreshRecipe(World);
        }
        if (CurrentRecipe == null || Power <= 0.0f) {
            return;
        }
        IrradiatingRecipe Recipe = CurrentRecipe.value();
        ItemStack Result = Recipe.getResult();
        if (!placeOutput(IrradiateOutput, 1, Result, true)) {
            return;
        }
        IrradiationDone += Power;
        if (IrradiationDone >= Recipe.getTime()) {
            IrradiationDone = 0.0f;
            placeOutput(IrradiateOutput, 1, Result.copy(), false);
            Contents.extractItem(IrradiateInput, 1, false);
        }
    }

    private void refreshRecipe(Level World) {
        ItemStack Loaded = Contents.getStackInSlot(IrradiateInput);
        RecipeHolder<IrradiatingRecipe> Found = Loaded.isEmpty() ? null
                : World.getRecipeManager().getRecipeFor(FissionRegistry.Irradiating.get(), new SingleRecipeInput(Loaded), World).orElse(null);
        ResourceLocation FoundId = Found == null ? null : Found.id();
        if (!Objects.equals(FoundId, CurrentRecipeId)) {
            IrradiationDone = 0.0f;
        }
        CurrentRecipe = Found;
        CurrentRecipeId = FoundId;
    }

    private void refreshRedstone(ServerLevel World, BlockPos Origin) {
        boolean Powered = false;
        for (int Row = 0; Row < ReactorLayout.Height && !Powered; Row++) {
            for (int Z = 0; Z < ReactorLayout.Width && !Powered; Z++) {
                for (int X = 0; X < ReactorLayout.Width && !Powered; X++) {
                    if (!ReactorLayout.isEnclosed(X, Row, Z) && World.hasNeighborSignal(Origin.offset(X, Row, Z))) {
                        Powered = true;
                    }
                }
            }
        }
        if (Powered != Interlocked) {
            Interlocked = Powered;
            NeedsSync = true;
            UrgentSync = true;
        }
    }

    public void markRedstoneDirty() {
        RedstoneDirty = true;
    }

    private void updateComparator(ServerLevel World, BlockPos Origin) {
        int Signal = comparatorLevel();
        if (Signal == Comparator) {
            return;
        }
        Comparator = Signal;
        Block Reactor = getBlockState().getBlock();
        for (BlockPos Cell : ReactorLayout.cells(Origin)) {
            if (!ReactorLayout.isEnclosed(Cell.getX() - Origin.getX(), Cell.getY() - Origin.getY(), Cell.getZ() - Origin.getZ())) {
                World.updateNeighbourForOutputSignal(Cell, Reactor);
            }
        }
    }

    public int comparatorLevel() {
        float Fraction = ReactorThermal.fraction(Temperature);
        return Temperature <= ReactorThermal.Ambient + 5.0f ? 0 : Math.min(15, 1 + (int) (Fraction * 14.0f));
    }

    public void pressButton(int Id, Player User) {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        switch (Id) {
            case ButtonToggle -> {
                if (Running) {
                    scram(World, worldPosition);
                } else {
                    start(World, worldPosition);
                }
            }
            case ButtonLower -> PowerLimit = Math.max(MinLimit, PowerLimit - LimitStep);
            case ButtonRaise -> PowerLimit = Math.min(MaxLimit, PowerLimit + LimitStep);
            default -> {
                return;
            }
        }
        setChanged();
        NeedsSync = true;
        UrgentSync = true;
    }

    private void start(ServerLevel World, BlockPos Origin) {
        if (Interlocked) {
            return;
        }
        Running = true;
        Vec3 Centre = ReactorLayout.centre(Origin);
        World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorStart.get(), SoundSource.BLOCKS, 1.6f, 1.0f);
        World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorClunk.get(), SoundSource.BLOCKS, 1.2f, 1.0f);
    }

    private void scram(ServerLevel World, BlockPos Origin) {
        Running = false;
        Vec3 Centre = ReactorLayout.centre(Origin);
        World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.ReactorScram.get(), SoundSource.BLOCKS, 1.8f, 1.0f);
        PendingClunks = ScramClunks;
        NextClunk = World.getGameTime();
        NeedsSync = true;
        UrgentSync = true;
    }

    private void meltdown(ServerLevel World, BlockPos Origin) {
        Vec3 Centre = ReactorLayout.centre(Origin);
        FissionReactorBlock.meltdown(World, Origin, getBlockState());
        DiscordNotices.meltdown(World, Origin, false);
        DamageSource Source = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FissionRegistry.MeltdownDamage));
        World.explode(null, Source, null, Centre.x, Centre.y, Centre.z, MeltdownPower, false, Level.ExplosionInteraction.BLOCK,
                ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, FissionRegistry.Meltdown);
        World.playSound(null, Centre.x, Centre.y, Centre.z, FissionRegistry.MeltdownRumble.get(), SoundSource.BLOCKS, 4.0f, 1.0f);
        ToxicGasCloud.fallout(World, Centre);
    }

    private void sync(ServerLevel World, long Now) {
        int PowerShown = Math.round(Power * 100.0f);
        int HeatShown = Math.round(Temperature);
        int LeakShown = Math.round(Mth.clamp(Leaked / 30.0f, 0.0f, 1.0f) * 100.0f);
        int WasteShown = Math.round(Waste.getFluidAmount() * 100.0f / WasteCapacity);
        int GasShown = Math.round(Gas * 100.0f / GasCapacity);
        if (PowerShown != LastSyncedPower || Math.abs(HeatShown - LastSyncedHeat) >= 2 || LeakShown != LastSyncedLeak || WasteShown != LastSyncedWaste || GasShown != LastSyncedGas) {
            NeedsSync = true;
            UrgentSync |= (PowerShown == 0) != (LastSyncedPower == 0) || ReactorThermal.of(HeatShown) != ReactorThermal.of(LastSyncedHeat);
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            LastSyncedPower = PowerShown;
            LastSyncedHeat = HeatShown;
            LastSyncedLeak = LeakShown;
            LastSyncedWaste = WasteShown;
            LastSyncedGas = GasShown;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void tickClient() {
        if (!Primed) {
            Primed = true;
            ShownPower = Power;
            ShownHeat = Temperature;
        }
        PreviousShownPower = ShownPower;
        PreviousShownHeat = ShownHeat;
        ShownPower += (Power - ShownPower) * 0.08f;
        ShownHeat += (Temperature - ShownHeat) * 0.1f;
    }

    public float getShownPower(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousShownPower, ShownPower);
    }

    public float getShownHeat(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousShownHeat, ShownHeat);
    }

    public float getPower() {
        return Power;
    }

    public float getTemperature() {
        return Temperature;
    }

    public boolean isRunning() {
        return Running;
    }

    public boolean isInterlocked() {
        return Interlocked;
    }

    public FissionStatus getStatus() {
        return Status;
    }

    public ReactorThermal getThermal() {
        return ReactorThermal.of(Temperature);
    }

    public int getPowerLimit() {
        return PowerLimit;
    }

    public int getGeneration() {
        return Generation;
    }

    public int getWasteShown() {
        return level != null && level.isClientSide ? ClientWaste : Math.round(Waste.getFluidAmount() * 100.0f / WasteCapacity);
    }

    public int getGasShown() {
        return level != null && level.isClientSide ? ClientGas : Math.round(Gas * 100.0f / GasCapacity);
    }

    public int getLeakShown() {
        return level != null && level.isClientSide ? ClientLeak : Math.round(Mth.clamp(Leaked / 30.0f, 0.0f, 1.0f) * 100.0f);
    }

    public float getGas() {
        return Gas;
    }

    public int getVenting() {
        return Venting;
    }

    public ReactorCooling.Survey getCooling() {
        return Cooling;
    }

    public int getQueuedRods() {
        int Count = 0;
        for (int Slot = FirstFuel; Slot < FirstFuel + FuelSlots; Slot++) {
            ItemStack Stack = Contents.getStackInSlot(Slot);
            if (ReactorFuel.isFuel(Stack)) {
                Count += Stack.getCount();
            }
        }
        return Count;
    }

    public boolean hasActiveRod() {
        return !Contents.getStackInSlot(ActiveRod).isEmpty();
    }

    public int rodRemaining() {
        if (!hasActiveRod() || BurnTotal <= 0) {
            return -1;
        }
        return Math.round(Mth.clamp(1.0f - BurnDone / BurnTotal, 0.0f, 1.0f) * 1000.0f);
    }

    public int rodMinutes() {
        if (!hasActiveRod() || BurnTotal <= 0) {
            return 0;
        }
        float Rate = Power > 0.01f ? Power : Math.max(0.1f, PowerLimit / 100.0f);
        return Math.round(Math.max(0.0f, BurnTotal - BurnDone) / Rate / 1200.0f);
    }

    public int settleTemperature() {
        ReactorFuel Fuel = activeFuel();
        float Drive = Math.max(Power, TargetPower);
        if (Fuel == null || Drive <= 0.0f) {
            return Math.round(ReactorThermal.Ambient);
        }
        float Settle = ReactorThermal.Ambient + Fuel.Heat() * Drive / coolingConstant();
        return Math.round(Math.min(MaxSettle, Settle));
    }

    public int irradiationPermille() {
        if (CurrentRecipe == null) {
            return 0;
        }
        return Math.round(Mth.clamp(IrradiationDone / CurrentRecipe.value().getTime(), 0.0f, 1.0f) * 1000.0f);
    }

    public AABB getRenderBounds() {
        return ReactorLayout.bounds(worldPosition).inflate(1.5);
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            ItemStack Stack = Contents.getStackInSlot(Slot).copy();
            if (Slot == ActiveRod && !Stack.isEmpty() && BurnDone > 0.0f) {
                Stack = ReactorFuel.depletedRod();
            }
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Stack);
            Contents.setStackInSlot(Slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Contents", Contents.serializeNBT(Registries));
        Tag.put("Waste", Waste.writeToNBT(Registries, new CompoundTag()));
        Tag.putBoolean("Running", Running);
        Tag.putInt("Limit", PowerLimit);
        Tag.putFloat("Power", Power);
        Tag.putFloat("Temperature", Temperature);
        Tag.putFloat("BurnDone", BurnDone);
        Tag.putInt("BurnTotal", BurnTotal);
        Tag.putFloat("Gas", Gas);
        Tag.putInt("Stored", Stored);
        Tag.putFloat("Irradiation", IrradiationDone);
        if (CurrentRecipeId != null) {
            Tag.putString("Recipe", CurrentRecipeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            Power = Tag.getByte("Power") / 100.0f;
            Temperature = Tag.getShort("Heat");
            Running = Tag.getBoolean("Running");
            Interlocked = Tag.getBoolean("Interlocked");
            Status = FissionStatus.byIndex(Tag.getByte("Status"));
            ClientLeak = Tag.getByte("Leak");
            ClientWaste = Tag.getByte("Waste");
            ClientGas = Tag.getByte("Gas");
            PowerLimit = Tag.getByte("Limit");
            Generation = Tag.getShort("Output");
            return;
        }
        if (Tag.contains("Contents")) {
            Contents.deserializeNBT(Registries, Tag.getCompound("Contents"));
        }
        if (Tag.contains("Waste")) {
            Waste.readFromNBT(Registries, Tag.getCompound("Waste"));
        }
        Running = false;
        PowerLimit = Tag.contains("Limit") ? Mth.clamp(Tag.getInt("Limit"), MinLimit, MaxLimit) : MaxLimit;
        Power = 0.0f;
        Temperature = ReactorThermal.Ambient;
        BurnDone = Math.max(0.0f, Tag.getFloat("BurnDone"));
        BurnTotal = Math.max(0, Tag.getInt("BurnTotal"));
        if (BurnTotal <= 0 && !Contents.getStackInSlot(ActiveRod).isEmpty()) {
            BurnTotal = ReactorFuel.of(Contents.getStackInSlot(ActiveRod)).BurnTicks();
        }
        Gas = 0.0f;
        Stored = 0;
        IrradiationDone = Math.max(0.0f, Tag.getFloat("Irradiation"));
        if (Tag.contains("Recipe")) {
            CurrentRecipeId = ResourceLocation.tryParse(Tag.getString("Recipe"));
        }
        RecipeDirty = true;
        RedstoneDirty = true;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        Tag.putByte("Power", (byte) Math.round(Power * 100.0f));
        Tag.putShort("Heat", (short) Math.round(Math.min(Temperature, Short.MAX_VALUE)));
        Tag.putBoolean("Running", Running);
        Tag.putBoolean("Interlocked", Interlocked);
        Tag.putByte("Status", (byte) Status.ordinal());
        Tag.putByte("Leak", (byte) getLeakShown());
        Tag.putByte("Waste", (byte) getWasteShown());
        Tag.putByte("Gas", (byte) getGasShown());
        Tag.putByte("Limit", (byte) PowerLimit);
        Tag.putShort("Output", (short) Generation);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.fission_reactor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new FissionReactorMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this && ReactorLayout.bounds(worldPosition).distanceToSqr(User.getEyePosition()) <= 64.0;
    }
}
