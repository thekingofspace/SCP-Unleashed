package net.rai.scp_unleashed.scp457;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class Scp457AshPileBlockEntity extends BlockEntity {
    public static final String RemainsKey = "Remains";

    @Nullable
    private CompoundTag Remains;

    public Scp457AshPileBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp457Registry.AshPileEntity.get(), Pos, State);
    }

    @Nullable
    public CompoundTag remains() {
        return Remains == null ? null : Remains.copy();
    }

    public boolean holdsRemains() {
        return Remains != null;
    }

    public void setRemains(@Nullable CompoundTag Saved) {
        Remains = Saved == null ? null : Saved.copy();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        if (Remains != null) {
            Saved.put(RemainsKey, Remains.copy());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        Remains = Saved.contains(RemainsKey, Tag.TAG_COMPOUND) ? Saved.getCompound(RemainsKey).copy() : null;
    }
}
