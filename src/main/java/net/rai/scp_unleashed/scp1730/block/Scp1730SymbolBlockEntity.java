package net.rai.scp_unleashed.scp1730.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730Symbols;

public class Scp1730SymbolBlockEntity extends BlockEntity {
    public Scp1730SymbolBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp1730Registry.SymbolEntity.get(), Pos, State);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            Scp1730Symbols.indexed(level, worldPosition, true);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            Scp1730Symbols.indexed(level, worldPosition, false);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null) {
            Scp1730Symbols.indexed(level, worldPosition, false);
        }
        super.onChunkUnloaded();
    }
}
