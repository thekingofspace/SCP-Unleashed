package net.rai.scp_unleashed.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.computer.ComputerDeviceEntity;

public class PointBankBlockEntity extends ComputerDeviceEntity {
    public static final int Draw = 3;
    public static final long MaxBalance = Long.MAX_VALUE / 4L;

    private long Balance;

    public PointBankBlockEntity(BlockPos Pos, BlockState State) {
        super(MiningRegistry.PointBankEntity.get(), Pos, State);
    }

    @Override
    public int getDraw() {
        return Draw;
    }

    public long getBalance() {
        return Balance;
    }

    public void deposit(long Points) {
        if (Points <= 0L) {
            return;
        }
        Balance = Math.min(MaxBalance, Balance + Points);
        setChanged();
    }

    public long withdraw(long Wanted) {
        long Taken = Math.max(0L, Math.min(Balance, Wanted));
        if (Taken > 0L) {
            Balance -= Taken;
            setChanged();
        }
        return Taken;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putLong("Balance", Balance);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Balance = Math.max(0L, Math.min(MaxBalance, Tag.getLong("Balance")));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        if (Balance > 0L) {
            Components.set(MiningRegistry.BalanceComponent.get(), Balance);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        Long Stored = Input.get(MiningRegistry.BalanceComponent.get());
        if (Stored != null) {
            Balance = Math.max(0L, Math.min(MaxBalance, Stored));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag Tag) {
        super.removeComponentsFromTag(Tag);
        Tag.remove("Balance");
    }
}
