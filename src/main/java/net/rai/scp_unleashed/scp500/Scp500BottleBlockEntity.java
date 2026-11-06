package net.rai.scp_unleashed.scp500;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class Scp500BottleBlockEntity extends BlockEntity {
    public static final String RestockKey = "Restock";

    private long Restock = Scp500Stock.Unset;

    public Scp500BottleBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp500Registry.BottleEntity.get(), Pos, State);
    }

    public Scp500Stock stock() {
        return new Scp500Stock(Scp500BottleBlock.pills(getBlockState()), Restock);
    }

    public Scp500Stock stockAt(long Now) {
        return stock().at(Now);
    }

    public long restock() {
        return Restock;
    }

    public Scp500Stock refresh() {
        return level instanceof ServerLevel World ? refreshAt(World.getGameTime()) : stock();
    }

    public Scp500Stock refreshAt(long Now) {
        Scp500Stock After = stockAt(Now);
        store(After);
        return After;
    }

    public boolean take() {
        if (!(level instanceof ServerLevel World)) {
            return false;
        }
        long Now = World.getGameTime();
        Scp500Stock Current = stockAt(Now);
        boolean Took = Current.Pills() > 0;
        store(Took ? Current.withPills(Current.Pills() - 1) : Current);
        return Took;
    }

    public void restockAt(long When) {
        Restock = When;
        setChanged();
    }

    private void store(Scp500Stock After) {
        if (level == null) {
            return;
        }
        boolean Timed = After.Restock() != Restock;
        if (Timed) {
            Restock = After.Restock();
            setChanged();
        }
        BlockState State = getBlockState();
        if (State.hasProperty(Scp500BottleBlock.Pills) && State.getValue(Scp500BottleBlock.Pills) != After.Pills()) {
            level.setBlock(worldPosition, State.setValue(Scp500BottleBlock.Pills, After.Pills()), Block.UPDATE_ALL);
            setChanged();
        } else if (Timed && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) {
            refresh();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putLong(RestockKey, Restock);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Restock = Tag.getLong(RestockKey);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        Components.set(Scp500Registry.Stock.get(), stock());
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        Scp500Stock Carried = Input.get(Scp500Registry.Stock.get());
        if (Carried != null) {
            Restock = Carried.Restock();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag Tag) {
        super.removeComponentsFromTag(Tag);
        Tag.remove(RestockKey);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putLong(RestockKey, Restock);
        return Tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
