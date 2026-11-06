package net.rai.scp_unleashed.scp330;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Scp330BowlBlockEntity extends BlockEntity {
    public static final String TakersKey = "Takers";
    public static final String IdKey = "Id";
    public static final String CountKey = "Count";

    private final Map<UUID, Integer> Takes = new HashMap<>();

    public Scp330BowlBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp330Registry.BowlEntity.get(), Pos, State);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            Scp330Bowls.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        Scp330Bowls.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        Scp330Bowls.remove(this);
    }

    public int takes(UUID Taker) {
        return Takes.getOrDefault(Taker, 0);
    }

    public int record(UUID Taker) {
        int Count = Takes.merge(Taker, 1, Integer::sum);
        setChanged();
        return Count;
    }

    public boolean forget(UUID Taker) {
        boolean Known = Takes.remove(Taker) != null;
        if (Known) {
            setChanged();
        }
        return Known;
    }

    public int takers() {
        return Takes.size();
    }

    public Scp330Memory memory() {
        return new Scp330Memory(Takes);
    }

    private void remember(Scp330Memory Remembered) {
        Takes.clear();
        Remembered.Takes().forEach((Taker, Count) -> {
            if (Count > 0) {
                Takes.put(Taker, Count);
            }
        });
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        ListTag Takers = new ListTag();
        Takes.forEach((Taker, Count) -> {
            CompoundTag Entry = new CompoundTag();
            Entry.putUUID(IdKey, Taker);
            Entry.putInt(CountKey, Count);
            Takers.add(Entry);
        });
        Saved.put(TakersKey, Takers);
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        Takes.clear();
        ListTag Takers = Saved.getList(TakersKey, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Takers.size(); Index++) {
            CompoundTag Entry = Takers.getCompound(Index);
            if (Entry.hasUUID(IdKey) && Entry.getInt(CountKey) > 0) {
                Takes.put(Entry.getUUID(IdKey), Entry.getInt(CountKey));
            }
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        if (!Takes.isEmpty()) {
            Components.set(Scp330Registry.Memory.get(), memory());
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        Scp330Memory Remembered = Input.get(Scp330Registry.Memory.get());
        if (Remembered != null) {
            remember(Remembered);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag Saved) {
        super.removeComponentsFromTag(Saved);
        Saved.remove(TakersKey);
    }
}
