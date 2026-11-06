package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.WireDevice;
import net.rai.scp_unleashed.wire.WireNetworkManager;

public class NetworkRouterBlockEntity extends BlockEntity implements WireDevice {
    public NetworkRouterBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.NetworkRouter.get(), Pos, State);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel World) {
            WireNetworkManager.get(World).routerLoaded(worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level instanceof ServerLevel World) {
            WireNetworkManager.routerUnloaded(World, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel World) {
            WireNetworkManager.routerUnloaded(World, worldPosition);
        }
    }
}
