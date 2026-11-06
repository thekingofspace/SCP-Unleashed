package net.rai.scp_unleashed.door;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface Screwdrivable {
    InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking);

    default boolean accepts(BlockState State, boolean Sneaking) {
        return true;
    }
}
