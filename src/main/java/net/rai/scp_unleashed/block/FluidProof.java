package net.rai.scp_unleashed.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FluidProof {
    private static final Map<Block, Boolean> Ours = new ConcurrentHashMap<>();

    private FluidProof() {
    }

    public static boolean holdsBack(BlockState State) {
        Block Found = State.getBlock();
        if (Found instanceof LiquidBlockContainer || State.canBeReplaced() || State.isAir()) {
            return false;
        }
        return Ours.computeIfAbsent(Found, Each -> Scp_unleashed.ModId.equals(BuiltInRegistries.BLOCK.getKey(Each).getNamespace()));
    }
}
