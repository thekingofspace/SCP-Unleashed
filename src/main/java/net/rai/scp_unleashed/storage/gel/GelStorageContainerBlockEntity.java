package net.rai.scp_unleashed.storage.gel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.wire.WireDevice;

public class GelStorageContainerBlockEntity extends StorageContainerBlockEntity implements WireDevice {
    public static final float FormVolume = 0.6f;
    public static final float BreakVolume = 0.7f;
    public static final float AlarmVolume = 0.5f;
    private final FluidTank Water = new FluidTank(GelRules.TankCapacity, GelStorageContainerBlockEntity::isWater) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int Gel;
    private int Hold;
    private int Received;
    private int LastReceived;
    private long LastPowered = Long.MIN_VALUE / 2;
    private boolean Powered;
    private boolean Watered;
    private int SentGel = -1;
    private int FormedSinceDrain;

    public GelStorageContainerBlockEntity(BlockPos Pos, BlockState State) {
        super(GelStorageRegistry.GelContainerEntity.get(), Pos, State);
    }

    public static boolean isWater(FluidStack Stack) {
        return Stack.is(FluidTags.WATER);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, GelStorageContainerBlockEntity Locker) {
        StorageContainerBlockEntity.serverTick(World, Pos, State, Locker);
        if (World instanceof ServerLevel Server && State.getBlock() instanceof GelStorageContainerBlock) {
            Locker.tickGel(Server, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, GelStorageContainerBlockEntity Locker) {
        StorageContainerBlockEntity.clientTick(World, Pos, State, Locker);
        Locker.predict(State);
    }

    private void predict(BlockState State) {
        if (!State.hasProperty(GelStorageContainerBlock.Status)) {
            return;
        }
        GelStatus Shown = State.getValue(GelStorageContainerBlock.Status);
        if (Shown == GelStatus.Forming || Shown == GelStatus.Sealed) {
            Gel = Math.min(GelRules.MaxGel, Gel + GelRules.FormRate);
        } else if (Hold > 0) {
            Hold--;
        } else {
            Gel = Math.max(0, Gel - GelRules.DecayRate);
        }
    }

    private void tickGel(ServerLevel World, BlockPos Pos, BlockState State) {
        LastReceived = Received;
        Received = 0;
        long Now = World.getGameTime();
        if (LastReceived >= GelRules.PowerDraw) {
            LastPowered = Now;
        }
        Powered = Now - LastPowered <= GelRules.PowerGrace;
        if (Now % GelRules.UpkeepEvery == 0L) {
            int Wanted = GelRules.UpkeepWater + FormedSinceDrain / GelRules.FormWaterPerUnit;
            if (Powered) {
                FluidStack Drawn = Water.getFluidAmount() >= Wanted ? Water.drain(Wanted, IFluidHandler.FluidAction.EXECUTE) : FluidStack.EMPTY;
                Watered = Drawn.getAmount() >= Wanted;
            } else {
                Watered = Water.getFluidAmount() >= GelRules.UpkeepWater;
            }
            FormedSinceDrain %= GelRules.FormWaterPerUnit;
        }
        boolean Supplied = Powered && Watered;
        boolean WasSealed = isSealed();
        if (Supplied) {
            Hold = GelRules.HoldTicks;
            if (Gel < GelRules.MaxGel) {
                int Before = Gel;
                Gel = Math.min(GelRules.MaxGel, Gel + GelRules.FormRate);
                FormedSinceDrain += Gel - Before;
            }
        } else if (Hold > 0) {
            Hold--;
        } else if (Gel > 0) {
            Gel = Math.max(0, Gel - GelRules.DecayRate);
        }
        GelStatus Wanted = statusFor(Supplied);
        GelStatus Shown = State.getValue(GelStorageContainerBlock.Status);
        if (Wanted != Shown) {
            World.setBlock(Pos, State.setValue(GelStorageContainerBlock.Status, Wanted), Block.UPDATE_CLIENTS);
            cueStatus(World, Shown, Wanted);
            sync();
        } else if (Math.abs(Gel - SentGel) >= GelRules.SyncStep || (Gel == 0) != (SentGel == 0) || WasSealed != isSealed()) {
            sync();
        }
        setChanged();
    }

    private GelStatus statusFor(boolean Supplied) {
        if (Supplied) {
            return Gel >= GelRules.MaxGel ? GelStatus.Sealed : GelStatus.Forming;
        }
        return Gel > 0 ? GelStatus.Failing : GelStatus.Idle;
    }

    private void cueStatus(ServerLevel World, GelStatus From, GelStatus To) {
        Vec3 Where = soundCentre();
        float Pitch = 0.9f + World.random.nextFloat() * 0.15f;
        if (To == GelStatus.Forming && From != GelStatus.Sealed) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : GelStorageRegistry.FormLayers) {
                World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.BLOCKS, FormVolume, Pitch);
            }
        } else if (To == GelStatus.Failing) {
            World.playSound(null, Where.x, Where.y, Where.z, GelStorageRegistry.GelAlarm.get(), SoundSource.BLOCKS, AlarmVolume, 1.0f);
        } else if (To == GelStatus.Idle && From == GelStatus.Failing) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : GelStorageRegistry.BreakLayers) {
                World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.BLOCKS, BreakVolume, Pitch);
            }
        }
    }

    private void sync() {
        SentGel = Gel;
        if (level != null && !level.isClientSide) {
            BlockState State = getBlockState();
            level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
        }
    }

    public boolean isSealed() {
        return Gel >= GelRules.SealLevel;
    }

    public boolean isPowered() {
        return Powered;
    }

    public boolean isWatered() {
        return Watered;
    }

    public boolean isSupplied() {
        return Powered && Watered;
    }

    public int getGel() {
        return Gel;
    }

    public int getHold() {
        return Hold;
    }

    public float gelShare() {
        return Gel / (float) GelRules.MaxGel;
    }

    public GelStatus getStatus() {
        BlockState State = getBlockState();
        return State.hasProperty(GelStorageContainerBlock.Status) ? State.getValue(GelStorageContainerBlock.Status) : GelStatus.Idle;
    }

    public FluidTank getTank() {
        return Water;
    }

    public void setGel(int Amount, int HoldTicks) {
        Gel = Math.max(0, Math.min(GelRules.MaxGel, Amount));
        Hold = Math.max(0, Math.min(GelRules.HoldTicks, HoldTicks));
        setChanged();
        sync();
    }

    @Override
    public boolean hasGlassFront() {
        return true;
    }

    @Override
    public void dropAll(Level World, BlockPos Pos) {
    }

    @Override
    public int getPowerDemand() {
        return Water.getFluidAmount() > 0 || Gel > 0 || hasContents() ? GelRules.PowerDraw : 0;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Math.max(0, Amount);
    }

    public GelPacked packed() {
        return new GelPacked(getContents(), Gel, Hold);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        if (hasContents() || Gel > 0) {
            Components.set(GelStorageRegistry.Packed.get(), packed());
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        GelPacked Found = Input.get(GelStorageRegistry.Packed.get());
        if (Found != null) {
            Gel = Found.Gel();
            Hold = Found.Hold();
            if (!Found.Contents().isEmpty()) {
                setContents(Found.Contents());
            }
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag Tag) {
        super.removeComponentsFromTag(Tag);
        Tag.remove("Contents");
        Tag.remove("Gel");
        Tag.remove("Hold");
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Gel", Gel);
        Tag.putInt("Hold", Hold);
        Tag.putBoolean("Powered", Powered);
        Tag.putBoolean("Watered", Watered);
        CompoundTag Tank = new CompoundTag();
        Water.writeToNBT(Registries, Tank);
        Tag.put("Water", Tank);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Gel = Math.max(0, Math.min(GelRules.MaxGel, Tag.getInt("Gel")));
        Hold = Math.max(0, Math.min(GelRules.HoldTicks, Tag.getInt("Hold")));
        Powered = Tag.getBoolean("Powered");
        Watered = Tag.getBoolean("Watered");
        Water.readFromNBT(Registries, Tag.getCompound("Water"));
    }
}
