package net.rai.scp_unleashed.scp3008.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Scp3008CounterBlockEntity extends BlockEntity {
    public static final long Cooldown = 6000L;
    private final Map<UUID, Long> Served = new HashMap<>();

    public Scp3008CounterBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp3008Registry.CounterEntity.get(), Pos, State);
    }

    public boolean ready(UUID Visitor, long Now) {
        Long Last = Served.get(Visitor);
        return Last == null || Now < Last || Now - Last >= Cooldown;
    }

    public void serve(UUID Visitor, long Now) {
        Served.put(Visitor, Now);
        setChanged();
    }

    public void forget(UUID Visitor) {
        if (Served.remove(Visitor) != null) {
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Lookup) {
        super.loadAdditional(Saved, Lookup);
        Served.clear();
        ListTag Rows = Saved.getList("Served", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Rows.size(); Index++) {
            CompoundTag Row = Rows.getCompound(Index);
            if (Row.hasUUID("Id")) {
                Served.put(Row.getUUID("Id"), Row.getLong("At"));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Lookup) {
        super.saveAdditional(Saved, Lookup);
        ListTag Rows = new ListTag();
        for (Map.Entry<UUID, Long> Entry : Served.entrySet()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Entry.getKey());
            Row.putLong("At", Entry.getValue());
            Rows.add(Row);
        }
        Saved.put("Served", Rows);
    }
}
