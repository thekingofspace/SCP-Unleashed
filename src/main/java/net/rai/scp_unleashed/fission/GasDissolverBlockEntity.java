package net.rai.scp_unleashed.fission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.upgrade.MachineUpgrades;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.upgrade.Upgradeable;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class GasDissolverBlockEntity extends BlockEntity implements MenuProvider, Upgradeable {
    public static final int TankCapacity = 4000;
    public static final float WaterPerGas = 1.0f;
    public static final float DissolveRate = 3.0f;
    public static final float BudgetTicks = 20.0f;
    public static final float ScrubRate = 2.0f;
    public static final double ScrubRadius = 7.0;
    public static final int ScrubInterval = 10;
    public static final int RateWindow = 20;
    public static final int WorkingHold = 40;
    public static final int SyncTicks = 10;
    public static final float RiseRate = 0.08f;
    public static final float FallRate = 0.04f;
    public static final int DataWater = 0;
    public static final int DataNetwork = 1;
    public static final int DataArea = 2;
    public static final int DataUse = 3;
    public static final int DataStatus = 4;
    public static final int DataUpgrades = 5;
    public static final int DataCount = 6;

    private static Consumer<GasDissolverBlockEntity> ClientEffects = Dissolver -> {
    };

    private final FluidTank Tank = new FluidTank(TankCapacity, Stack -> Stack.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final MachineUpgrades Upgrades = new MachineUpgrades(this::setChanged, UpgradeType.Speed);
    private final GasHandler Intake = new GasHandler() {
        @Override
        public GasStack getGas() {
            return GasStack.EMPTY;
        }

        @Override
        public int getCapacity() {
            return Math.round(budgetCap());
        }

        @Override
        public int fill(GasStack Offer, boolean Simulate) {
            if (Offer.isEmpty() || !Offer.is(GasRegistry.ToxicGas.get()) || !(level instanceof ServerLevel World)) {
                return 0;
            }
            int Possible = (int) Math.min(Offer.getAmount(), Math.floor(Math.min(Budget, (Tank.getFluidAmount() + WaterCredit) / WaterPerGas) + 0.001f));
            if (Possible <= 0) {
                return 0;
            }
            if (Simulate) {
                return Possible;
            }
            return (int) Math.floor(dissolve(World, Possible, true) + 0.001f);
        }

        @Override
        public GasStack drain(int Amount, boolean Simulate) {
            return GasStack.EMPTY;
        }
    };
    private float Budget;
    private float WaterCredit;
    private float NetworkWindow;
    private float AreaWindow;
    private float UseWindow;
    private int NetworkRate;
    private int AreaRate;
    private int UseRate;
    private long LastDissolved = Long.MIN_VALUE / 2;
    private long LastScrubbed = Long.MIN_VALUE / 2;
    private boolean SyncedWorking;
    private boolean SyncedScrubbing;
    private int SyncedFill = -1;
    private long LastSync = Long.MIN_VALUE / 2;
    private boolean ClientWorking;
    private boolean ClientScrubbing;
    private int ClientFill;
    private boolean Primed;
    private float Intensity;
    private float PreviousIntensity;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataWater -> Tank.getFluidAmount();
                case DataNetwork -> NetworkRate;
                case DataArea -> AreaRate;
                case DataUse -> UseRate;
                case DataStatus -> getMode().ordinal();
                case DataUpgrades -> Upgrades.count(UpgradeType.Speed);
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

    public GasDissolverBlockEntity(BlockPos Pos, BlockState State) {
        super(FissionRegistry.GasDissolverEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<GasDissolverBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static int perSecond(float PerTick) {
        return Math.round(PerTick * 20.0f);
    }

    public static String formatAmount(float Amount) {
        return Amount == Math.round(Amount) ? Integer.toString(Math.round(Amount)) : String.format(Locale.ROOT, "%.1f", Amount);
    }

    public FluidTank getTank() {
        return Tank;
    }

    public GasHandler getIntake() {
        return Intake;
    }

    public static float rateFor(int Upgrades) {
        return DissolveRate * (1.0f + MachineUpgrades.SpeedPerUpgrade * Upgrades);
    }

    private float speed() {
        return Upgrades.speedFactor();
    }

    private float budgetCap() {
        return DissolveRate * speed() * BudgetTicks;
    }

    public int getSpeedUpgrades() {
        return Upgrades.count(UpgradeType.Speed);
    }

    public void dropUpgrades(Level World, BlockPos Pos) {
        Upgrades.drop(World, Pos);
    }

    @Override
    public boolean acceptsUpgrade(UpgradeType Type) {
        return Upgrades.accepts(Type);
    }

    @Override
    public boolean hasUpgrade(UpgradeType Type) {
        return Upgrades.count(Type) > 0;
    }

    @Override
    public int countUpgrade(UpgradeType Type) {
        return Upgrades.count(Type);
    }

    @Override
    public void installUpgrade(UpgradeType Type) {
        Upgrades.install(Type);
    }

    @Override
    public Collection<UpgradeType> getUpgrades() {
        return Upgrades.list();
    }

    public boolean hasWater() {
        return Tank.getFluidAmount() > 0 || WaterCredit > 0.001f;
    }

    public DissolverMode getMode() {
        BlockState State = getBlockState();
        return State.hasProperty(GasDissolverBlock.Mode) ? State.getValue(GasDissolverBlock.Mode) : DissolverMode.Empty;
    }

    public int getNetworkRate() {
        return NetworkRate;
    }

    public int getAreaRate() {
        return AreaRate;
    }

    public int getUseRate() {
        return UseRate;
    }

    public float dissolve(ServerLevel World, float Amount, boolean FromNetwork) {
        float Possible = Math.min(Math.min(Amount, Budget), (Tank.getFluidAmount() + WaterCredit) / WaterPerGas);
        if (Possible <= 0.001f) {
            return 0.0f;
        }
        float Needed = Possible * WaterPerGas;
        if (Needed > WaterCredit) {
            WaterCredit += Tank.drain(Mth.ceil(Needed - WaterCredit), IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
        float Used = Math.min(Needed, WaterCredit);
        WaterCredit -= Used;
        float Done = Used / WaterPerGas;
        Budget -= Done;
        UseWindow += Used;
        long Now = World.getGameTime();
        LastDissolved = Now;
        if (FromNetwork) {
            NetworkWindow += Done;
        } else {
            AreaWindow += Done;
            LastScrubbed = Now;
        }
        setChanged();
        return Done;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, GasDissolverBlockEntity Dissolver) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof GasDissolverBlock) {
            Dissolver.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, GasDissolverBlockEntity Dissolver) {
        Dissolver.tickClient();
        ClientEffects.accept(Dissolver);
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        Budget = Math.min(budgetCap(), Budget + DissolveRate * speed());
        if (Math.floorMod(Now + Pos.asLong(), ScrubInterval) == 0) {
            scrub(World, Pos);
        }
        if (Now % RateWindow == 0) {
            NetworkRate = Math.round(NetworkWindow * 20.0f / RateWindow);
            AreaRate = Math.round(AreaWindow * 20.0f / RateWindow);
            UseRate = Math.round(UseWindow * 20.0f / RateWindow);
            NetworkWindow = 0.0f;
            AreaWindow = 0.0f;
            UseWindow = 0.0f;
        }
        DissolverMode Wanted = !hasWater() ? DissolverMode.Empty : Now - LastDissolved <= WorkingHold ? DissolverMode.Working : DissolverMode.Idle;
        if (State.getValue(GasDissolverBlock.Mode) != Wanted) {
            World.setBlock(Pos, State.setValue(GasDissolverBlock.Mode, Wanted), Block.UPDATE_CLIENTS);
        }
        updateSync(World, Now, Wanted == DissolverMode.Working, Now - LastScrubbed <= WorkingHold);
    }

    private void scrub(ServerLevel World, BlockPos Pos) {
        float Allowance = Math.min(Budget, ScrubRate * speed() * ScrubInterval);
        if (Allowance <= 0.01f || !hasWater()) {
            return;
        }
        Vec3 Centre = Vec3.atCenterOf(Pos);
        double Reach = ScrubRadius * ScrubRadius;
        List<ToxicGasCloud> Clouds = World.getEntitiesOfClass(ToxicGasCloud.class, new AABB(Pos).inflate(ScrubRadius),
                Cloud -> Cloud.isAlive() && Cloud.position().distanceToSqr(Centre) <= Reach);
        Clouds.sort(Comparator.comparingDouble(Cloud -> Cloud.position().distanceToSqr(Centre)));
        for (ToxicGasCloud Cloud : Clouds) {
            float Taken = dissolve(World, Math.min(Allowance, Cloud.getGas()), false);
            if (Taken <= 0.0f) {
                break;
            }
            Cloud.drainGas(Taken);
            Allowance -= Taken;
            if (Allowance <= 0.01f) {
                break;
            }
        }
    }

    private void updateSync(ServerLevel World, long Now, boolean Working, boolean Scrubbing) {
        int Fill = Math.round(Tank.getFluidAmount() * 100.0f / TankCapacity);
        boolean Urgent = Working != SyncedWorking || Scrubbing != SyncedScrubbing;
        if (Urgent || (Fill != SyncedFill && Now - LastSync >= SyncTicks)) {
            SyncedWorking = Working;
            SyncedScrubbing = Scrubbing;
            SyncedFill = Fill;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void tickClient() {
        float Target = ClientWorking ? 1.0f : 0.0f;
        if (!Primed) {
            Primed = true;
            Intensity = Target;
        }
        PreviousIntensity = Intensity;
        Intensity += (Target - Intensity) * (Target > Intensity ? RiseRate : FallRate);
        if (Math.abs(Target - Intensity) < 0.002f) {
            Intensity = Target;
        }
    }

    public float getIntensity() {
        return Intensity;
    }

    public float getRenderIntensity(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousIntensity, Intensity);
    }

    public boolean isScrubbing() {
        return ClientScrubbing;
    }

    public int getShownFill() {
        return ClientFill;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Water", Tank.writeToNBT(Registries, new CompoundTag()));
        Tag.putFloat("Credit", WaterCredit);
        Upgrades.save(Tag);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            ClientWorking = Tag.getBoolean("Working");
            ClientScrubbing = Tag.getBoolean("Scrubbing");
            ClientFill = Mth.clamp(Tag.getByte("Fill"), 0, 100);
            return;
        }
        if (Tag.contains("Water")) {
            Tank.readFromNBT(Registries, Tag.getCompound("Water"));
        }
        Upgrades.load(Tag);
        WaterCredit = Mth.clamp(Tag.getFloat("Credit"), 0.0f, WaterPerGas * budgetCap());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        Tag.putBoolean("Working", SyncedWorking);
        Tag.putBoolean("Scrubbing", SyncedScrubbing);
        Tag.putByte("Fill", (byte) Math.round(Tank.getFluidAmount() * 100.0f / TankCapacity));
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.gas_dissolver");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new GasDissolverMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
