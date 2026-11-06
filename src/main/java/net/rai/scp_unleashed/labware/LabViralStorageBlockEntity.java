package net.rai.scp_unleashed.labware;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class LabViralStorageBlockEntity extends BlockEntity implements WireDevice, MenuProvider {
    public static final int SlotCount = LabVialRack.SlotCount;
    public static final int PowerDraw = 3;
    public static final int PowerGrace = 10;
    public static final double HatchHeight = 0.88;
    public static final float LidOpenAngle = 95.0f;
    public static final float LidStep = 0.1f;
    public static final int DataPowered = 0;
    public static final int DataReceived = 1;
    public static final int DataCultures = 2;
    public static final int DataCount = 3;

    private static Consumer<LabViralStorageBlockEntity> ClientEffects = Locker -> {
    };

    private final LabVialRack Rack = new LabVialRack() {
        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                BlockState State = getBlockState();
                level.updateNeighbourForOutputSignal(worldPosition, State.getBlock());
                level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
            }
        }
    };

    private int Received;
    private int LastReceived;
    private long LastPowered = Long.MIN_VALUE / 2;
    private boolean Powered;
    private boolean Stocked;
    private float LidSwing;
    private float LastLidSwing;
    private boolean LidPrimed;
    private int[] MistColours = new int[0];

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            return switch (Index) {
                case DataPowered -> isPowered() ? 1 : 0;
                case DataReceived -> Math.min(Short.MAX_VALUE, LastReceived);
                case DataCultures -> Rack.countCultures();
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

    public LabViralStorageBlockEntity(BlockPos Pos, BlockState State) {
        super(LabwareRegistry.ViralStorageEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<LabViralStorageBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, LabViralStorageBlockEntity Locker) {
        if (World instanceof ServerLevel Server && State.getBlock() instanceof LabViralStorageBlock) {
            Locker.tickServer(Server, Pos);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, LabViralStorageBlockEntity Locker) {
        Locker.stepLid();
        ClientEffects.accept(Locker);
    }

    private void stepLid() {
        float Wanted = isLidOpen() ? 1.0f : 0.0f;
        if (!LidPrimed) {
            LidPrimed = true;
            LidSwing = Wanted;
            LastLidSwing = Wanted;
            return;
        }
        LastLidSwing = LidSwing;
        LidSwing = Mth.clamp(LidSwing + (Wanted > LidSwing ? LidStep : -LidStep), 0.0f, 1.0f);
    }

    public float getLidAngle(float PartialTick) {
        float Blended = Mth.lerp(PartialTick, LastLidSwing, LidSwing);
        float Eased = 1.0f - Blended;
        return (1.0f - Eased * Eased * Eased) * LidOpenAngle;
    }

    private void tickServer(ServerLevel World, BlockPos Pos) {
        LastReceived = Received;
        Received = 0;
        Stocked = Rack.countVials() > 0;
        if (LastReceived > 0) {
            LastPowered = World.getGameTime();
        }
        boolean WasPowered = Powered;
        Powered = World.getGameTime() - LastPowered <= PowerGrace;
        if (WasPowered && !Powered && Rack.countCultures() > 0) {
            LabwareSounds.play(World, Pos, LabwareSounds.Alarmed, 1.0f);
        }
        boolean Open = isLidOpen();
        boolean Breached = Open && !Powered && Rack.countCultures() > 0;
        applyState(World, Pos, Powered, Breached);
        if (Breached && LabExposure.isPulse(World, Pos.asLong())) {
            vent(World, Pos);
        }
    }

    private void applyState(ServerLevel World, BlockPos Pos, boolean Chilled, boolean Breached) {
        BlockState Current = getBlockState();
        BlockState Wanted = Current.setValue(LabViralStorageBlock.Chilled, Chilled).setValue(LabViralStorageBlock.Venting, Breached);
        if (Wanted != Current) {
            World.setBlock(Pos, Wanted, Block.UPDATE_CLIENTS);
        }
    }

    public void toggleLid(ServerLevel World, BlockPos Pos) {
        BlockState Current = getBlockState();
        boolean Opening = !Current.getValue(LabViralStorageBlock.Hatch);
        BlockState Wanted = Current.setValue(LabViralStorageBlock.Hatch, Opening);
        if (!Opening) {
            Wanted = Wanted.setValue(LabViralStorageBlock.Venting, false);
        }
        World.setBlock(Pos, Wanted, Block.UPDATE_CLIENTS);
        LabwareSounds.play(World, Pos, Opening ? LabwareSounds.Opened : LabwareSounds.Closed, LabwareSounds.pitch(World));
    }

    private void vent(ServerLevel World, BlockPos Pos) {
        List<ResourceLocation> Samples = new ArrayList<>();
        for (int Slot = 0; Slot < Rack.getSlots(); Slot++) {
            ItemStack Vial = Rack.getStackInSlot(Slot);
            if (LabVials.carriesCulture(Vial)) {
                Samples.add(LabVials.sampleOf(Vial));
            }
        }
        LabOrganisms.exposeAll(World, Vec3.atBottomCenterOf(Pos).add(0.0, HatchHeight, 0.0), Pos::equals, Samples);
        for (ResourceLocation Sample : new LinkedHashSet<>(Samples)) {
            LabOrganisms.find(Sample).ifPresent(Organism -> Organism.ventFrom(World, Pos, World.getRandom()));
        }
    }

    private int[] cultureMists() {
        Set<Integer> Colours = new LinkedHashSet<>();
        for (int Slot = 0; Slot < Rack.getSlots(); Slot++) {
            ItemStack Vial = Rack.getStackInSlot(Slot);
            if (LabVials.carriesCulture(Vial)) {
                Colours.add(LabOrganisms.mistColourOf(LabVials.sampleOf(Vial)));
            }
        }
        return Colours.stream().mapToInt(Integer::intValue).toArray();
    }

    public int[] getMistColours() {
        return MistColours;
    }

    public int getSignal() {
        int Filled = Rack.countVials();
        return Filled == 0 ? 0 : Math.max(1, Filled * 15 / SlotCount);
    }

    public boolean isPowered() {
        return Powered;
    }

    public boolean isStocked() {
        return Stocked;
    }

    public boolean isLidOpen() {
        BlockState State = getBlockState();
        return State.hasProperty(LabViralStorageBlock.Hatch) && State.getValue(LabViralStorageBlock.Hatch);
    }

    public int getLastReceived() {
        return LastReceived;
    }

    public boolean isBreached() {
        BlockState State = getBlockState();
        return State.hasProperty(LabViralStorageBlock.Venting) && State.getValue(LabViralStorageBlock.Venting);
    }

    public LabVialRack getRack() {
        return Rack;
    }

    public ContainerData getData() {
        return Data;
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this && isLidOpen()
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < Rack.getSlots(); Slot++) {
            ItemStack Vial = Rack.getStackInSlot(Slot);
            if (!Vial.isEmpty()) {
                Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Vial.copy());
                Rack.setStackInSlot(Slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.lab_viral_storage");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new LabViralStorageMenu(ContainerId, PlayerInventory, this, Rack, Data);
    }

    @Override
    public int getPowerDemand() {
        return Stocked ? PowerDraw : 0;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    public void observeNetwork(long Available) {
        if (Available > 0 && level != null) {
            LastPowered = level.getGameTime();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Items", Rack.serializeNBT(Registries));
        Tag.putBoolean("Powered", Powered);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Sync")) {
            MistColours = Tag.getIntArray("Mist");
            return;
        }
        if (Tag.contains("Items")) {
            CompoundTag Saved = Tag.getCompound("Items");
            Saved.putInt("Size", SlotCount);
            Rack.deserializeNBT(Registries, Saved);
        }
        Powered = Tag.getBoolean("Powered");
        Stocked = Rack.countVials() > 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (Powered && level != null) {
            LastPowered = level.getGameTime();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Sync", true);
        Tag.putInt("Vials", Rack.countVials());
        Tag.putIntArray("Mist", cultureMists());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
