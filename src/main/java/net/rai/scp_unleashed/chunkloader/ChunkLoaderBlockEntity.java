package net.rai.scp_unleashed.chunkloader;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ChunkLoaderBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int RestartTicks = 10;
    public static final int SyncTicks = 10;
    public static final int StepLarge = 4;
    public static final int ButtonShrink = 0;
    public static final int ButtonGrow = 1;
    public static final int ButtonShrinkMore = 2;
    public static final int ButtonGrowMore = 3;
    public static final int ButtonToggle = 4;
    public static final int DataRadius = 0;
    public static final int DataMaxRadius = 1;
    public static final int DataStatus = 2;
    public static final int DataLoaded = 3;
    public static final int DataEnabled = 4;
    public static final int DataGrace = 5;
    public static final int DataDemand = 6;
    public static final int DataReceived = 8;
    public static final int DataBase = 10;
    public static final int DataPerChunk = 12;
    public static final int DataCount = 14;

    private static Consumer<ChunkLoaderBlockEntity> ClientEffects = Loader -> {
    };

    private final LongOpenHashSet Forced = new LongOpenHashSet();
    private int Radius;
    private boolean Enabled = true;
    private boolean Holding;
    private boolean Verified;
    private boolean NeedsTrim;
    private int Cursor;
    private int Received;
    private int LastReceived;
    private int Demand;
    private int ShortTicks;
    private int FedTicks;
    private ChunkLoaderStatus Status = ChunkLoaderStatus.Unpowered;
    private int ShownLoaded;
    private int SyncedLoaded = -1;
    private boolean NeedsSync;
    private boolean UrgentSync;
    private long LastSync = Long.MIN_VALUE / 2;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataRadius -> Radius;
                case DataMaxRadius -> ChunkLoaderConfig.maxRadius();
                case DataStatus -> Status.ordinal();
                case DataLoaded -> Forced.size();
                case DataEnabled -> Enabled ? 1 : 0;
                case DataGrace -> getGraceLeft();
                case DataDemand, DataDemand + 1 -> half(ChunkLoaderConfig.demand(Radius), Index - DataDemand);
                case DataReceived, DataReceived + 1 -> half(LastReceived, Index - DataReceived);
                case DataBase, DataBase + 1 -> half(ChunkLoaderConfig.basePower(), Index - DataBase);
                case DataPerChunk, DataPerChunk + 1 -> half(ChunkLoaderConfig.powerPerChunk(), Index - DataPerChunk);
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

    public ChunkLoaderBlockEntity(BlockPos Pos, BlockState State) {
        super(ChunkLoaderRegistry.ChunkLoaderEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<ChunkLoaderBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    private static int half(int Value, int Part) {
        return Part == 0 ? Value & 0xFFFF : Value >>> 16 & 0xFFFF;
    }

    public static int join(int Low, int High) {
        return (Low & 0xFFFF) | (High & 0xFFFF) << 16;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, ChunkLoaderBlockEntity Loader) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof ChunkLoaderBlock) {
            Loader.tickServer(ServerWorld, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, ChunkLoaderBlockEntity Loader) {
        ClientEffects.accept(Loader);
    }

    private void tickServer(ServerLevel World, BlockState State) {
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        int Asked = Demand;
        int Cap = ChunkLoaderConfig.maxRadius();
        if (Radius > Cap) {
            Radius = Cap;
            NeedsTrim = true;
            Cursor = 0;
            setChanged();
        }
        if (!Verified) {
            Verified = true;
            reassert(World);
        }
        ChunkLoaderStatus Next;
        if (!Enabled) {
            Demand = 0;
            ShortTicks = 0;
            FedTicks = 0;
            if (Holding || !Forced.isEmpty()) {
                stopHolding(World, true);
            }
            Next = ChunkLoaderStatus.Off;
        } else {
            int Cost = ChunkLoaderConfig.demand(Radius);
            Demand = Cost;
            if (Cost <= 0 || Asked > 0 && Delivered >= Asked) {
                ShortTicks = 0;
                FedTicks = Math.min(FedTicks + 1, RestartTicks);
                if (!Holding && FedTicks >= RestartTicks) {
                    Holding = true;
                    Cursor = 0;
                    setChanged();
                    play(World, ChunkLoaderRegistry.Start.get());
                }
            } else if (Asked > 0) {
                FedTicks = 0;
                if (Holding && ++ShortTicks > ChunkLoaderConfig.graceTicks()) {
                    stopHolding(World, true);
                }
            }
            if (Holding) {
                trim(World);
                acquire(World);
                Next = ShortTicks > 0 ? ChunkLoaderStatus.LowPower : Forced.size() < ChunkLoaderArea.chunkCount(Radius) ? ChunkLoaderStatus.Starting : ChunkLoaderStatus.Active;
            } else {
                Next = ChunkLoaderStatus.Unpowered;
            }
        }
        if (Next != Status) {
            Status = Next;
            NeedsSync = true;
            UrgentSync = true;
        }
        if (State.getValue(ChunkLoaderBlock.Active) != Status.isHolding()) {
            World.setBlock(worldPosition, State.setValue(ChunkLoaderBlock.Active, Status.isHolding()), Block.UPDATE_CLIENTS);
        }
        updateSync(World);
    }

    private void play(ServerLevel World, SoundEvent Cue) {
        World.playSound(null, worldPosition, Cue, SoundSource.BLOCKS, 0.8f, 0.95f + World.getRandom().nextFloat() * 0.1f);
    }

    private void stopHolding(ServerLevel World, boolean Audible) {
        boolean Was = Holding;
        Holding = false;
        ShortTicks = 0;
        FedTicks = 0;
        releaseAll(World);
        if (Was && Audible) {
            play(World, ChunkLoaderRegistry.Stop.get());
        }
    }

    private void reassert(ServerLevel World) {
        if (Forced.isEmpty()) {
            return;
        }
        ChunkPos Centre = new ChunkPos(worldPosition);
        int Budget = ChunkLoaderConfig.chunksPerTick();
        LongIterator Each = Forced.iterator();
        while (Each.hasNext()) {
            long Chunk = Each.nextLong();
            boolean Keep = Enabled && Holding && ChunkLoaderArea.contains(Centre, Radius, Chunk);
            if (Keep && Budget > 0) {
                if (ChunkLoaderTickets.hold(World, worldPosition, Chunk)) {
                    Budget--;
                }
            } else {
                ChunkLoaderTickets.release(World, worldPosition, Chunk);
                Each.remove();
            }
        }
        Cursor = 0;
        setChanged();
    }

    private void trim(ServerLevel World) {
        if (!NeedsTrim) {
            return;
        }
        NeedsTrim = false;
        ChunkPos Centre = new ChunkPos(worldPosition);
        LongIterator Each = Forced.iterator();
        while (Each.hasNext()) {
            long Chunk = Each.nextLong();
            if (!ChunkLoaderArea.contains(Centre, Radius, Chunk)) {
                ChunkLoaderTickets.release(World, worldPosition, Chunk);
                Each.remove();
            }
        }
        setChanged();
    }

    private void acquire(ServerLevel World) {
        int Total = ChunkLoaderArea.chunkCount(Radius);
        if (Cursor >= Total) {
            return;
        }
        ChunkPos Centre = new ChunkPos(worldPosition);
        int Budget = ChunkLoaderConfig.chunksPerTick();
        boolean Changed = false;
        while (Cursor < Total && Budget > 0) {
            long Chunk = ChunkLoaderArea.chunkAt(Centre, Cursor);
            Cursor++;
            if (Forced.add(Chunk)) {
                Changed = true;
                boolean Ready = World.getChunkSource().hasChunk(ChunkPos.getX(Chunk), ChunkPos.getZ(Chunk));
                if (ChunkLoaderTickets.hold(World, worldPosition, Chunk) && !Ready) {
                    Budget--;
                }
            }
        }
        if (Changed) {
            setChanged();
        }
    }

    public void releaseAll(ServerLevel World) {
        if (!Forced.isEmpty()) {
            LongIterator Each = Forced.iterator();
            while (Each.hasNext()) {
                ChunkLoaderTickets.release(World, worldPosition, Each.nextLong());
            }
            Forced.clear();
            setChanged();
        }
        Cursor = 0;
    }

    public void removed(ServerLevel World) {
        Holding = false;
        releaseAll(World);
    }

    public boolean wantsOnLoad(long Chunk) {
        return Enabled && Holding && ChunkLoaderArea.contains(new ChunkPos(worldPosition), Math.min(Radius, ChunkLoaderConfig.maxRadius()), Chunk);
    }

    public void adoptTickets(LongSet Kept) {
        Forced.clear();
        Forced.addAll(Kept);
        Cursor = 0;
        setChanged();
    }

    public void pressButton(int Id) {
        switch (Id) {
            case ButtonShrink -> changeRadius(Radius - 1);
            case ButtonGrow -> changeRadius(Radius + 1);
            case ButtonShrinkMore -> changeRadius(Radius - StepLarge);
            case ButtonGrowMore -> changeRadius(Radius + StepLarge);
            case ButtonToggle -> {
                Enabled = !Enabled;
                FedTicks = 0;
                setChanged();
                NeedsSync = true;
                UrgentSync = true;
            }
            default -> {
            }
        }
    }

    private void changeRadius(int Wanted) {
        int Clamped = Mth.clamp(Wanted, 0, ChunkLoaderConfig.maxRadius());
        if (Clamped == Radius) {
            return;
        }
        if (Clamped < Radius) {
            NeedsTrim = true;
        }
        Radius = Clamped;
        Cursor = 0;
        setChanged();
        NeedsSync = true;
        UrgentSync = true;
    }

    private void updateSync(ServerLevel World) {
        long Now = World.getGameTime();
        if (Forced.size() != SyncedLoaded) {
            NeedsSync = true;
        }
        if (NeedsSync && (UrgentSync || Now - LastSync >= SyncTicks)) {
            NeedsSync = false;
            UrgentSync = false;
            LastSync = Now;
            SyncedLoaded = Forced.size();
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public int getRadius() {
        return Radius;
    }

    public boolean isEnabled() {
        return Enabled;
    }

    public ChunkLoaderStatus getStatus() {
        return Status;
    }

    public int getLoadedCount() {
        return level != null && level.isClientSide ? ShownLoaded : Forced.size();
    }

    public int getDemand() {
        return Enabled ? ChunkLoaderConfig.demand(Radius) : 0;
    }

    public int getLastReceived() {
        return LastReceived;
    }

    public int getGraceLeft() {
        return Holding && ShortTicks > 0 ? Math.max(0, ChunkLoaderConfig.graceTicks() - ShortTicks) : 0;
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
        Tag.putInt("Radius", Radius);
        Tag.putBoolean("Enabled", Enabled);
        Tag.putBoolean("Holding", Holding);
        Tag.putLongArray("Forced", Forced.toLongArray());
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Radius = Mth.clamp(Tag.getInt("Radius"), 0, ChunkLoaderArea.HardMaxRadius);
        Enabled = !Tag.contains("Enabled") || Tag.getBoolean("Enabled");
        if (Tag.contains("Sync")) {
            Status = ChunkLoaderStatus.byIndex(Tag.getByte("Status"));
            ShownLoaded = Math.max(0, Tag.getInt("Loaded"));
            return;
        }
        Holding = Tag.getBoolean("Holding");
        Forced.clear();
        for (long Chunk : Tag.getLongArray("Forced")) {
            Forced.add(Chunk);
        }
        Status = Holding ? ChunkLoaderStatus.Starting : Enabled ? ChunkLoaderStatus.Unpowered : ChunkLoaderStatus.Off;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        Tag.putInt("Radius", Radius);
        Tag.putBoolean("Enabled", Enabled);
        Tag.putByte("Status", (byte) Status.ordinal());
        Tag.putInt("Loaded", Forced.size());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.chunk_loader");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new ChunkLoaderMenu(ContainerId, PlayerInventory, this, Data);
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
