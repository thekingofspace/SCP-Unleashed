package net.rai.scp_unleashed.scp939.compressor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.processing.MachineItemHandler;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import net.rai.scp_unleashed.scp939.Scp939Rooms;
import net.rai.scp_unleashed.scp939.Scp939Sounds;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class GasCompressorBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int InputSlot = 0;
    public static final int OutputSlot = 1;
    public static final int SlotCount = 2;
    public static final int WorkDraw = 32;
    public static final int CanisterTicks = 2400;
    public static final float MaxSources = 4.0f;
    public static final int RoomCheckTicks = 40;
    public static final int ActiveHold = 10;
    public static final int HissMin = 70;
    public static final int HissMax = 110;
    public static final float ScrubPerSecond = 0.003f;
    public static final float ScrubPerCanister = 0.05f;
    public static final int DataCount = 6;
    public static final int ProgressScale = 1000;

    private final ItemStackHandler Storage = new ItemStackHandler(SlotCount) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Slot == InputSlot && accepts(Stack);
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
        }
    };
    private final IItemHandler InputSide = new MachineItemHandler(Storage, true, false, InputSlot);
    private final IItemHandler OutputSide = new MachineItemHandler(Storage, false, true, OutputSlot);
    private final IItemHandler Automation = new MachineItemHandler(Storage, true, true, InputSlot, OutputSlot);
    private int Received;
    private int LastReceived;
    private int Demand;
    private int LastDemand;
    private float Progress;
    private float Sources;
    private int RoomWait;
    private int HissTimer = HissMin;
    private long LastWorked = Long.MIN_VALUE / 2;
    private CompressorStatus Status = CompressorStatus.Idle;
    private int Made;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case 0 -> Math.min(ProgressScale, Math.round(progress() * ProgressScale));
                case 1 -> Math.min(Short.MAX_VALUE, totalTicks());
                case 2 -> Math.min(Short.MAX_VALUE, LastReceived);
                case 3 -> Math.min(Short.MAX_VALUE, LastDemand);
                case 4 -> Status.ordinal();
                default -> Math.round(Sources * 10.0f);
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

    public GasCompressorBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp939Registry.CompressorEntity.get(), Pos, State);
    }

    public static boolean accepts(ItemStack Stack) {
        return Stack.is(Scp939Registry.EmptyCanister.get());
    }

    public static int ticksFor(float Sources) {
        if (Sources <= 0.0f) {
            return 0;
        }
        return Math.max(1, Math.round(CanisterTicks / Math.min(MaxSources, Sources)));
    }

    public static void tick(Level World, BlockPos Pos, BlockState State, GasCompressorBlockEntity Compressor) {
        if (World instanceof ServerLevel Server) {
            Compressor.tickServer(Server, Pos, State);
        }
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        LastReceived = Received;
        LastDemand = Demand;
        Received = 0;
        if (--RoomWait <= 0) {
            RoomWait = RoomCheckTicks;
            Optional<Scp939Rooms.Room> Room = Scp939Rooms.roomAround(World, Pos);
            Sources = Room.map(Found -> Scp939Rooms.sources(World, Found)).orElse(0.0f);
        }
        ItemStack Input = Storage.getStackInSlot(InputSlot);
        ItemStack Output = Storage.getStackInSlot(OutputSlot);
        boolean Space = Output.isEmpty() || Output.is(Scp939Registry.Canister.get()) && Output.getCount() < Output.getMaxStackSize();
        float Fraction = 0.0f;
        if (Input.isEmpty() || !accepts(Input)) {
            Status = CompressorStatus.Idle;
            Demand = 0;
        } else if (!Space) {
            Status = CompressorStatus.Blocked;
            Demand = 0;
        } else if (Sources <= 0.0f) {
            Status = CompressorStatus.Thin;
            Demand = 0;
        } else {
            Demand = WorkDraw;
            Fraction = LastDemand > 0 ? Math.min(1.0f, LastReceived / (float) WorkDraw) : 0.0f;
            Status = LastDemand <= 0 ? CompressorStatus.Unpowered : LastReceived <= 0 ? CompressorStatus.Unpowered
                    : LastReceived >= WorkDraw ? CompressorStatus.Running : CompressorStatus.Underpowered;
        }
        if (Fraction > 0.0f) {
            LastWorked = Now;
            Progress += Fraction;
            if (Now % 20L == 0L) {
                Scp939Rooms.roomAround(World, Pos).ifPresent(Found -> Scp939Rooms.scrub(World, Found, ScrubPerSecond));
            }
            if (Progress >= totalTicks()) {
                Progress = 0.0f;
                complete(World, Pos);
            }
            if (--HissTimer <= 0) {
                HissTimer = Mth.nextInt(World.getRandom(), HissMin, HissMax);
                Scp939Sounds.play(World, Pos, Scp939Registry.HissLayers, 0.45f, 0.9f + World.getRandom().nextFloat() * 0.15f);
            }
            setChanged();
        }
        boolean Working = Now - LastWorked <= ActiveHold;
        if (State.getValue(GasCompressorBlock.Active) != Working) {
            World.setBlock(Pos, State.setValue(GasCompressorBlock.Active, Working), Block.UPDATE_CLIENTS);
            World.playSound(null, Pos, Working ? Scp939Registry.Start.get() : Scp939Registry.Stop.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
            World.playSound(null, Pos, Scp939Registry.HissAir.get(), SoundSource.BLOCKS, Working ? 0.5f : 0.35f, Working ? 1.1f : 0.85f);
        }
    }

    private void complete(ServerLevel World, BlockPos Pos) {
        Storage.extractItem(InputSlot, 1, false);
        ItemStack Output = Storage.getStackInSlot(OutputSlot);
        if (Output.isEmpty()) {
            Storage.setStackInSlot(OutputSlot, new ItemStack(Scp939Registry.Canister.get()));
        } else {
            Storage.setStackInSlot(OutputSlot, Output.copyWithCount(Output.getCount() + 1));
        }
        Made++;
        Scp939Sounds.play(World, Pos, Scp939Registry.ThumpLayers, 0.7f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        Scp939Rooms.roomAround(World, Pos).ifPresent(Found -> Scp939Rooms.scrub(World, Found, ScrubPerCanister));
    }

    public void recheck() {
        RoomWait = 0;
    }

    public int totalTicks() {
        return ticksFor(Sources);
    }

    public float progress() {
        int Total = totalTicks();
        return Total > 0 ? Mth.clamp(Progress / Total, 0.0f, 1.0f) : 0.0f;
    }

    public float sources() {
        return Sources;
    }

    public CompressorStatus status() {
        return Status;
    }

    public int lastReceived() {
        return LastReceived;
    }

    public int made() {
        return Made;
    }

    public boolean isActive() {
        BlockState State = getBlockState();
        return State.hasProperty(GasCompressorBlock.Active) && State.getValue(GasCompressorBlock.Active);
    }

    public ItemStackHandler getStorage() {
        return Storage;
    }

    public ContainerData getData() {
        return Data;
    }

    public IItemHandler getItemHandler(@Nullable Direction Side) {
        if (Side == null) {
            return Automation;
        }
        return Side == Direction.DOWN ? OutputSide : InputSide;
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Storage.getStackInSlot(Slot).copy());
            Storage.setStackInSlot(Slot, ItemStack.EMPTY);
        }
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
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
    public Component getDisplayName() {
        return Component.translatable("block.scp_unleashed.gas_compressor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new GasCompressorMenu(ContainerId, PlayerInventory, this, Data);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Items", Storage.serializeNBT(Registries));
        Tag.putFloat("Progress", Progress);
        Tag.putFloat("Sources", Sources);
        Tag.putInt("Made", Made);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Items")) {
            Storage.deserializeNBT(Registries, Tag.getCompound("Items"));
        }
        Progress = Math.max(0.0f, Tag.getFloat("Progress"));
        Sources = Math.max(0.0f, Tag.getFloat("Sources"));
        Made = Math.max(0, Tag.getInt("Made"));
    }
}
