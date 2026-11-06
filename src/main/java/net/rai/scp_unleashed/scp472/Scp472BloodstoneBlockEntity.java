package net.rai.scp_unleashed.scp472;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Scp472BloodstoneBlockEntity extends BlockEntity {
    public Scp472BloodstoneBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp472Registry.BloodstoneEntity.get(), Pos, State);
    }

    public static void tick(Level World, BlockPos Pos, BlockState State, Scp472BloodstoneBlockEntity Stone) {
        Scp472Stones.add(World, Pos);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            Scp472Stones.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            Scp472Stones.remove(level, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null) {
            Scp472Stones.remove(level, worldPosition);
        }
    }
}
