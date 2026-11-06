package net.rai.scp_unleashed.props;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SecurityCameraBlockEntity extends BlockEntity {
    public SecurityCameraBlockEntity(BlockPos Pos, BlockState State) {
        super(PropsRegistry.SecurityCameraEntity.get(), Pos, State);
    }

    public float getPhase() {
        long Seed = getBlockPos().asLong() * 0x9E3779B97F4A7C15L;
        return (float) ((Seed >>> 40) & 0xFFFF) / 65536.0f;
    }
}
