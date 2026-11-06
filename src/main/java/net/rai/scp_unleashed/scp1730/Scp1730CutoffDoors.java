package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.door.PanelDoorBlockEntity;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Cutoffs;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Fixture;

public final class Scp1730CutoffDoors {
    private Scp1730CutoffDoors() {
    }

    public static void settle(LevelAccessor World, BlockPos Pos, Scp1730Fixture Fixture) {
        Scp1730Cutoffs.Cut Made = Scp1730Cutoffs.of(Fixture);
        if (!(World.getBlockEntity(Pos) instanceof PanelDoorBlockEntity Door)) {
            return;
        }
        CompoundTag Tag = new CompoundTag();
        Tag.putFloat("Progress", Made.Open() ? 1.0f : 0.0f);
        Tag.putBoolean("Powered", Made.Open());
        Tag.putBoolean("Latched", false);
        Tag.putBoolean("Held", false);
        Door.loadCustomOnly(Tag, World.registryAccess());
        Door.setChanged();
        if (World instanceof Level Real) {
            BlockState State = Real.getBlockState(Pos);
            Real.sendBlockUpdated(Pos, State, State, Block.UPDATE_CLIENTS);
        }
    }
}
