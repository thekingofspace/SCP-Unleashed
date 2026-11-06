package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.DuctBlock;
import net.rai.scp_unleashed.pressure.PressureNetwork;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;

public class CreativePressurizerBlockEntity extends BlockEntity {
    public CreativePressurizerBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.CreativePressurizer.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, CreativePressurizerBlockEntity Pump) {
        if (!(World instanceof ServerLevel ServerWorld)) {
            return;
        }
        PressureNetworkManager Manager = PressureNetworkManager.get(ServerWorld);
        for (Direction Side : Direction.values()) {
            BlockPos Next = Pos.relative(Side);
            if (!World.isLoaded(Next)) {
                continue;
            }
            BlockState NextState = World.getBlockState(Next);
            if (NextState.getBlock() instanceof DuctBlock Duct && NextState.getValue(DuctBlock.Connections.get(Side.getOpposite()))) {
                PressureNetwork Network = Manager.networkAt(ServerWorld, Next, Duct.getKind());
                Network.pump(Network.headroom(Duct.getKind().getTargetPressure()));
            }
        }
    }
}
