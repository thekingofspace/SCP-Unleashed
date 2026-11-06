package net.rai.scp_unleashed.scp207;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Scp207CrateBlockEntity extends BlockEntity {
    private long Day;
    private int Taken;

    public Scp207CrateBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp207Registry.CrateEntity.get(), Pos, State);
    }

    public int leftOn(long Today) {
        return new Scp207Ration.State(Day, Taken).leftOn(Today);
    }

    public int left() {
        return level == null ? Scp207Ration.Daily : leftOn(Scp207Ration.today(level));
    }

    public boolean take() {
        if (level == null) {
            return false;
        }
        long Today = Scp207Ration.today(level);
        int Already = new Scp207Ration.State(Day, Taken).takenOn(Today);
        if (Already >= Scp207Ration.Daily) {
            return false;
        }
        store(Today, Already + 1);
        return true;
    }

    public void setTaken(int Value) {
        if (level != null) {
            store(Scp207Ration.today(level), Mth.clamp(Value, 0, Scp207Ration.Daily));
        }
    }

    private void store(long Today, int Count) {
        Day = Today;
        Taken = Count;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putLong("RationDay", Day);
        Tag.putInt("RationTaken", Taken);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Day = Tag.getLong("RationDay");
        Taken = Tag.getInt("RationTaken");
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        if (Taken > 0) {
            Components.set(Scp207Registry.RationData.get(), new Scp207Ration.State(Day, Taken));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        Scp207Ration.State Stored = Input.get(Scp207Registry.RationData.get());
        if (Stored != null) {
            Day = Stored.Day();
            Taken = Mth.clamp(Stored.Taken(), 0, Scp207Ration.Daily);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag Tag) {
        super.removeComponentsFromTag(Tag);
        Tag.remove("RationDay");
        Tag.remove("RationTaken");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putLong("RationDay", Day);
        Tag.putInt("RationTaken", Taken);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
