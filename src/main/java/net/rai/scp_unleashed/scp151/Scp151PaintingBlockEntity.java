package net.rai.scp_unleashed.scp151;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Scp151PaintingBlockEntity extends BlockEntity {
    public static final int SweepInterval = 2;

    private int Watchers;

    public Scp151PaintingBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp151Registry.PaintingEntity.get(), Pos, State);
    }

    public int getWatchers() {
        return Watchers;
    }

    public static void serverTick(ServerLevel World, BlockPos Pos, BlockState State, Scp151PaintingBlockEntity Painting) {
        if (Math.floorMod(World.getGameTime() + Pos.hashCode(), (long) SweepInterval) != 0L) {
            return;
        }
        if (!(State.getBlock() instanceof Scp151PaintingBlock) || !Scp151PaintingBlock.isLower(State) || Scp151PaintingBlock.isCovered(State)) {
            Painting.Watchers = 0;
            return;
        }
        Painting.Watchers = Scp151Gaze.sweep(World, Pos, State.getValue(HorizontalDirectionalBlock.FACING));
    }
}
