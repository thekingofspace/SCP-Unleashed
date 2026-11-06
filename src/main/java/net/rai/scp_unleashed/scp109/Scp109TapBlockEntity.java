package net.rai.scp_unleashed.scp109;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.particle.PipeLiquidOptions;

public class Scp109TapBlockEntity extends BlockEntity {
    public static final int MaxPerTick = 200;
    public static final int MaxPerSecond = MaxPerTick * 20;
    public static final int ShownAmount = 1_000_000;
    public static final int RateWindow = 20;
    public static final int FlowHold = 10;
    public static final int FlowSoundTicks = 40;
    public static final int DripChance = 5;
    public static final int DripSoundChance = 30;
    public static final float FlowVolume = 0.55f;
    public static final double DripReach = 0.44;
    public static final double DripHeight = 0.2;

    private ItemStack Held = ItemStack.EMPTY;
    private long BudgetTick = Long.MIN_VALUE;
    private int DrawnThisTick;
    private int Window;
    private int Rate;
    private long LastDrawn = Long.MIN_VALUE / 2;
    private long Total;
    private final Scp109Outlet Outlet = new Scp109Outlet(this, true);
    private final Scp109Outlet Spout = new Scp109Outlet(this, false);

    public Scp109TapBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp109Registry.TapEntity.get(), Pos, State);
    }

    public boolean hasCanteen() {
        return !Held.isEmpty();
    }

    public ItemStack canteen() {
        return Held;
    }

    public IFluidHandler outlet() {
        return Outlet;
    }

    public IFluidHandler spout() {
        return Spout;
    }

    public int getRate() {
        return Rate;
    }

    public long getTotal() {
        return Total;
    }

    public boolean isFlowing() {
        return level != null && hasCanteen() && level.getGameTime() - LastDrawn <= FlowHold;
    }

    public void insert(ItemStack Stack) {
        Held = Stack.copyWithCount(1);
        changed();
    }

    public ItemStack take() {
        ItemStack Taken = Held;
        Held = ItemStack.EMPTY;
        Window = 0;
        Rate = 0;
        changed();
        return Taken;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    int allowance(boolean Limited) {
        if (!hasCanteen() || level == null || level.isClientSide) {
            return 0;
        }
        if (!Limited) {
            return ShownAmount;
        }
        long Now = level.getGameTime();
        if (Now != BudgetTick) {
            BudgetTick = Now;
            DrawnThisTick = 0;
        }
        return Math.max(0, MaxPerTick - DrawnThisTick);
    }

    void drew(int Amount, boolean Limited) {
        if (Limited) {
            DrawnThisTick += Amount;
        }
        Window += Amount;
        Total += Amount;
        if (level != null) {
            LastDrawn = level.getGameTime();
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp109TapBlockEntity Tap) {
        long Now = World.getGameTime();
        if (Now % RateWindow == 0) {
            Tap.Rate = Math.round(Tap.Window * 20.0f / RateWindow);
            if (Tap.Window > 0) {
                Tap.setChanged();
            }
            Tap.Window = 0;
        }
        boolean Seated = Tap.hasCanteen();
        boolean Flowing = Seated && Now - Tap.LastDrawn <= FlowHold;
        if (State.getValue(Scp109TapBlock.Canteen) != Seated || State.getValue(Scp109TapBlock.Flowing) != Flowing) {
            World.setBlock(Pos, State.setValue(Scp109TapBlock.Canteen, Seated).setValue(Scp109TapBlock.Flowing, Flowing), Block.UPDATE_CLIENTS);
            Tap.setChanged();
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp109TapBlockEntity Tap) {
        if (!State.getValue(Scp109TapBlock.Flowing)) {
            return;
        }
        RandomSource Random = World.getRandom();
        double X = Pos.getX() + 0.5;
        double Y = Pos.getY() + 0.5;
        double Z = Pos.getZ() + 0.5;
        if (Math.floorMod(World.getGameTime() + Pos.asLong(), FlowSoundTicks) == 0) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp109Registry.FlowLayers) {
                World.playLocalSound(X, Y, Z, Layer.get(), SoundSource.BLOCKS, FlowVolume, 0.95f + Random.nextFloat() * 0.1f, false);
            }
        }
        if (Random.nextInt(DripSoundChance) == 0) {
            World.playLocalSound(X, Y, Z, Scp109Registry.TapDrip.get(), SoundSource.BLOCKS, 0.35f, 0.9f + Random.nextFloat() * 0.2f, false);
        }
        if (Random.nextInt(DripChance) == 0) {
            Direction Front = State.getValue(Scp109TapBlock.FACING);
            Direction Across = Front.getClockWise();
            double Spread = (Random.nextDouble() - 0.5) * 0.3;
            double DropX = X + Front.getStepX() * DripReach + Across.getStepX() * Spread;
            double DropZ = Z + Front.getStepZ() * DripReach + Across.getStepZ() * Spread;
            World.addParticle(new PipeLiquidOptions(Fluids.WATER, false), DropX, Pos.getY() + DripHeight, DropZ, 0.0, -0.06, 0.0);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (!Held.isEmpty()) {
            Tag.put("Canteen", Held.save(Registries));
        }
        Tag.putLong("Total", Total);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Held = Tag.contains("Canteen") ? ItemStack.parseOptional(Registries, Tag.getCompound("Canteen")) : ItemStack.EMPTY;
        Total = Tag.getLong("Total");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = saveCustomOnly(Registries);
        Tag.putBoolean("Seated", !Held.isEmpty());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
