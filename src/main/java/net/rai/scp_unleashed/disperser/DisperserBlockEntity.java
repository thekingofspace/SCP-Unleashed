package net.rai.scp_unleashed.disperser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabVials;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DisperserBlockEntity extends BlockEntity implements WireDevice {
    private final ItemStackHandler Chamber = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return LabVials.carriesCulture(Stack);
        }

        @Override
        public int getSlotLimit(int Slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int Slot) {
            contentsChanged();
        }
    };

    private final IItemHandler Automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int Slot) {
            return Chamber.getStackInSlot(Slot);
        }

        @Override
        public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
            return Chamber.insertItem(Slot, Stack, Simulate);
        }

        @Override
        public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int Slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Chamber.isItemValid(Slot, Stack);
        }
    };

    private int Received;
    private int LastReceived;
    private long LastFed = Long.MIN_VALUE / 2;
    private boolean Fed;
    private boolean Signalled;
    private int Cooldown = DisperserTuning.WarmUpTicks;
    private int Pulses;
    @Nullable
    private ResourceLocation Synced;

    public DisperserBlockEntity(BlockPos Pos, BlockState State) {
        super(DisperserRegistry.DisperserEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, DisperserBlockEntity Machine) {
        if (World instanceof ServerLevel Server && State.getBlock() instanceof DisperserBlock) {
            Machine.tickServer(Server, Pos);
        }
    }

    private void tickServer(ServerLevel World, BlockPos Pos) {
        long Now = World.getGameTime();
        LastReceived = Received;
        Received = 0;
        if (LastReceived >= DisperserTuning.PowerDraw) {
            LastFed = Now;
        }
        Fed = Now - LastFed <= DisperserTuning.PowerGrace;
        Signalled = World.hasNeighborSignal(Pos);
        boolean Loaded = getCulture() != null;
        boolean Running = Loaded && Signalled && Fed;
        applyState(World, Pos, Running, Loaded);
        if (!Running) {
            Cooldown = Math.max(DisperserTuning.WarmUpTicks, Cooldown - 1);
            return;
        }
        Cooldown--;
        if (Cooldown <= 0) {
            Cooldown = DisperserTuning.PulseTicks;
            pulse(World, Pos);
        }
    }

    private void applyState(ServerLevel World, BlockPos Pos, boolean Running, boolean Loaded) {
        BlockState Current = getBlockState();
        BlockState Wanted = Current.setValue(DisperserBlock.Active, Running).setValue(DisperserBlock.Loaded, Loaded);
        if (Wanted != Current) {
            World.setBlock(Pos, Wanted, Block.UPDATE_CLIENTS);
        }
    }

    public void pulse(ServerLevel World, BlockPos Pos) {
        LabOrganism Organism = LabOrganisms.find(getCulture()).orElse(null);
        if (Organism == null) {
            return;
        }
        Vec3 Nozzle = nozzle(Pos);
        DisperserSpread.release(World, Nozzle, Pos::equals, Organism, DisperserTuning.Radius);
        DisperserSpread.spray(World, Nozzle.add(0.0, 0.12, 0.0), Organism.getMistColour(), DisperserTuning.SprayParticles, DisperserTuning.SpraySpeed, DisperserTuning.SprayRise);
        DisperserSpread.play(World, Nozzle, DisperserRegistry.Released, 1.0F);
        Pulses++;
        setChanged();
    }

    public static Vec3 nozzle(BlockPos Pos) {
        return Vec3.atBottomCenterOf(Pos).add(0.0, DisperserTuning.NozzleHeight, 0.0);
    }

    private void contentsChanged() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState State = level.getBlockState(worldPosition);
        if (!(State.getBlock() instanceof DisperserBlock) || level.getBlockEntity(worldPosition) != this) {
            return;
        }
        boolean Loaded = getCulture() != null;
        if (State.getValue(DisperserBlock.Loaded) != Loaded) {
            BlockState Wanted = State.setValue(DisperserBlock.Loaded, Loaded);
            if (!Loaded) {
                Wanted = Wanted.setValue(DisperserBlock.Active, false);
            }
            level.setBlock(worldPosition, Wanted, Block.UPDATE_ALL);
        } else {
            level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    public ResourceLocation getCulture() {
        if (level != null && level.isClientSide) {
            return Synced;
        }
        ItemStack Vial = Chamber.getStackInSlot(0);
        return LabVials.carriesCulture(Vial) ? LabVials.sampleOf(Vial) : null;
    }

    public int getTint() {
        return LabOrganisms.vialColourOf(getCulture());
    }

    public ItemStack getVial() {
        return Chamber.getStackInSlot(0);
    }

    public boolean accepts(ItemStack Stack) {
        return LabVials.carriesCulture(Stack);
    }

    public ItemStack swap(ItemStack Vial) {
        ItemStack Previous = Chamber.getStackInSlot(0).copy();
        Chamber.setStackInSlot(0, Vial.copyWithCount(1));
        return Previous;
    }

    public ItemStack take() {
        ItemStack Previous = Chamber.getStackInSlot(0).copy();
        Chamber.setStackInSlot(0, ItemStack.EMPTY);
        return Previous;
    }

    public IItemHandler automation() {
        return Automation;
    }

    public ItemStackHandler chamber() {
        return Chamber;
    }

    public boolean isPowered() {
        return Fed;
    }

    public boolean isSignalled() {
        return Signalled;
    }

    public boolean isRunning() {
        BlockState State = getBlockState();
        return State.hasProperty(DisperserBlock.Active) && State.getValue(DisperserBlock.Active);
    }

    public int getPulses() {
        return Pulses;
    }

    public void dropContents(Level World, BlockPos Pos) {
        ItemStack Vial = take();
        if (!Vial.isEmpty()) {
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Vial);
        }
    }

    @Override
    public int getPowerDemand() {
        return getCulture() != null && Signalled ? DisperserTuning.PowerDraw : 0;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Chamber", Chamber.serializeNBT(Registries));
        Tag.putInt("Cooldown", Cooldown);
        Tag.putInt("Pulses", Pulses);
        Tag.putBoolean("Fed", Fed);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            Synced = Tag.contains("Culture") ? ResourceLocation.tryParse(Tag.getString("Culture")) : null;
            return;
        }
        if (Tag.contains("Chamber")) {
            CompoundTag Saved = Tag.getCompound("Chamber");
            Saved.putInt("Size", 1);
            Chamber.deserializeNBT(Registries, Saved);
        }
        Cooldown = Tag.contains("Cooldown") ? Tag.getInt("Cooldown") : DisperserTuning.WarmUpTicks;
        Pulses = Tag.getInt("Pulses");
        Fed = Tag.getBoolean("Fed");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (Fed && level != null) {
            LastFed = level.getGameTime();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        ResourceLocation Culture = getCulture();
        if (Culture != null) {
            Tag.putString("Culture", Culture.toString());
        }
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection Link, ClientboundBlockEntityDataPacket Update, HolderLookup.Provider Registries) {
        ResourceLocation Before = Synced;
        super.onDataPacket(Link, Update, Registries);
        if (level != null && level.isClientSide && !Objects.equals(Before, Synced)) {
            BlockState State = getBlockState();
            level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_IMMEDIATE);
        }
    }
}
