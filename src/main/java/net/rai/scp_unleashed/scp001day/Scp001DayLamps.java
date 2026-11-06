package net.rai.scp_unleashed.scp001day;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.rai.scp_unleashed.props.FluorescentLightBlock;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayCode;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayPlan;

public final class Scp001DayLamps {
    public static final int CheckTicks = 4;
    public static final double Range = 24.0;
    public static final double ToggleOdds = 0.14;

    private Scp001DayLamps() {
    }

    public static void tick(ServerLevel World, long Now) {
        if (Now % CheckTicks != 0) {
            return;
        }
        for (ServerPlayer User : World.players()) {
            int LowX = Scp001DayPlan.cellOf((int) Math.floor(User.getX() - Range));
            int HighX = Scp001DayPlan.cellOf((int) Math.floor(User.getX() + Range));
            int LowZ = Scp001DayPlan.cellOf((int) Math.floor(User.getZ() - Range));
            int HighZ = Scp001DayPlan.cellOf((int) Math.floor(User.getZ() + Range));
            for (int CellX = LowX; CellX <= HighX; CellX++) {
                for (int CellZ = LowZ; CellZ <= HighZ; CellZ++) {
                    Scp001DayPlan.sewerLamps(CellX, CellZ, (X, Y, Z, Code) -> {
                        if (Code != Scp001DayCode.FlickerLamp || User.distanceToSqr(X + 0.5, Y + 0.5, Z + 0.5) > Range * Range || World.random.nextDouble() > ToggleOdds) {
                            return;
                        }
                        BlockPos Pos = new BlockPos(X, Y, Z);
                        if (!World.isLoaded(Pos)) {
                            return;
                        }
                        BlockState State = World.getBlockState(Pos);
                        if (State.getBlock() instanceof FluorescentLightBlock) {
                            World.setBlock(Pos, State.setValue(BlockStateProperties.LIT, !State.getValue(BlockStateProperties.LIT)), Block.UPDATE_CLIENTS);
                        }
                    });
                }
            }
        }
    }
}
