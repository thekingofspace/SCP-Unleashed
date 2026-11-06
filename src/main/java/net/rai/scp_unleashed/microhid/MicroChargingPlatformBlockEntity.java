package net.rai.scp_unleashed.microhid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.wire.WireDevice;

import java.util.function.Consumer;

public class MicroChargingPlatformBlockEntity extends BlockEntity implements WireDevice {
    public static final int SyncTicks = 10;
    public static final int StarveTicks = 10;

    private static Consumer<MicroChargingPlatformBlockEntity> ClientEffects = Platform -> {
    };

    private ItemStack Docked = ItemStack.EMPTY;
    private int Received;
    private int LastReceived;
    private int Demand;
    private int Starved = StarveTicks;
    private MicroChargingStatus Status = MicroChargingStatus.Empty;
    private boolean NeedsSync = true;
    private long LastSync = Long.MIN_VALUE / 2;

    public MicroChargingPlatformBlockEntity(BlockPos Pos, BlockState State) {
        super(MicroHidRegistry.PlatformEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<MicroChargingPlatformBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, MicroChargingPlatformBlockEntity Platform) {
        if (World instanceof ServerLevel ServerWorld) {
            Platform.tickServer(ServerWorld, Pos);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, MicroChargingPlatformBlockEntity Platform) {
        ClientEffects.accept(Platform);
    }

    private void tickServer(ServerLevel World, BlockPos Pos) {
        LastReceived = Received;
        Received = 0;
        MicroChargingStatus Next;
        if (Docked.isEmpty()) {
            Demand = 0;
            Starved = StarveTicks;
            Next = MicroChargingStatus.Empty;
        } else if (MicroHidEnergy.isFull(Docked)) {
            Demand = 0;
            Next = MicroChargingStatus.Full;
        } else {
            if (LastReceived > 0) {
                MicroHidEnergy.fill(Docked, LastReceived);
                Starved = 0;
                setChanged();
                NeedsSync = true;
            } else if (Starved < StarveTicks) {
                Starved++;
            }
            boolean Topped = MicroHidEnergy.isFull(Docked);
            Demand = Topped ? 0 : Math.min(MicroHidEnergy.ChargeDraw, MicroHidEnergy.Capacity - MicroHidEnergy.get(Docked));
            Next = Topped ? MicroChargingStatus.Full : Starved < StarveTicks ? MicroChargingStatus.Charging : MicroChargingStatus.Unpowered;
        }
        if (Next == MicroChargingStatus.Full && Status == MicroChargingStatus.Charging) {
            MicroHidSounds.play(World, Pos.getCenter(), MicroHidSounds.Full);
        }
        if (Next != Status) {
            Status = Next;
            NeedsSync = true;
            LastSync = Long.MIN_VALUE / 2;
        }
        updateState(World, Pos);
        long Now = World.getGameTime();
        if (NeedsSync && Now - LastSync >= SyncTicks) {
            NeedsSync = false;
            LastSync = Now;
            World.sendBlockUpdated(Pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void updateState(ServerLevel World, BlockPos Pos) {
        BlockState Current = getBlockState();
        if (!(Current.getBlock() instanceof MicroChargingPlatformBlock)) {
            return;
        }
        int Segments = MicroChargingPlatformBlock.segmentsFor(Docked);
        if (Current.getValue(MicroChargingPlatformBlock.Status) != Status || Current.getValue(MicroChargingPlatformBlock.Charge) != Segments) {
            World.setBlock(Pos, Current.setValue(MicroChargingPlatformBlock.Status, Status).setValue(MicroChargingPlatformBlock.Charge, Segments), Block.UPDATE_CLIENTS);
        }
    }

    public void dock(ItemStack Stack) {
        Docked = Stack.copyWithCount(1);
        Starved = StarveTicks;
        changed();
    }

    public ItemStack undock() {
        ItemStack Taken = Docked;
        Docked = ItemStack.EMPTY;
        Demand = 0;
        changed();
        return Taken;
    }

    private void changed() {
        setChanged();
        NeedsSync = true;
        LastSync = Long.MIN_VALUE / 2;
        if (level instanceof ServerLevel World) {
            Status = Docked.isEmpty() ? MicroChargingStatus.Empty : MicroHidEnergy.isFull(Docked) ? MicroChargingStatus.Full : MicroChargingStatus.Unpowered;
            updateState(World, worldPosition);
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            NeedsSync = false;
            LastSync = World.getGameTime();
        }
    }

    public ItemStack getDocked() {
        return Docked;
    }

    public MicroChargingStatus getStatus() {
        return Status;
    }

    public int getLastReceived() {
        return LastReceived;
    }

    public void dropContents(Level World, BlockPos Pos) {
        if (!Docked.isEmpty()) {
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Docked.copy());
            Docked = ItemStack.EMPTY;
        }
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
        if (!Docked.isEmpty()) {
            Tag.put("Docked", Docked.save(Registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Docked = Tag.contains("Docked") ? ItemStack.parseOptional(Registries, Tag.getCompound("Docked")) : ItemStack.EMPTY;
        if (Tag.contains("Status")) {
            Status = MicroChargingStatus.byIndex(Tag.getByte("Status"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        if (!Docked.isEmpty()) {
            Tag.put("Docked", Docked.save(Registries));
        }
        Tag.putByte("Status", (byte) Status.ordinal());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
