package net.rai.scp_unleashed.intro.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.intro.IntroRegistry;
import net.rai.scp_unleashed.intro.IntroWorld;

public class IntroGateCoreBlockEntity extends BlockEntity {
    public IntroGateCoreBlockEntity(BlockPos Pos, BlockState State) {
        super(IntroRegistry.GateCoreEntity.get(), Pos, State);
    }

    public Direction front() {
        BlockState State = getBlockState();
        return State.hasProperty(IntroGateCoreBlock.Facing) ? State.getValue(IntroGateCoreBlock.Facing) : Direction.SOUTH;
    }

    public AABB field() {
        AABB Local = IntroWorld.GateField;
        double OffsetX = getBlockPos().getX() - IntroWorld.GateCore.getX();
        double OffsetY = getBlockPos().getY() - IntroWorld.GateCore.getY();
        double OffsetZ = getBlockPos().getZ() - IntroWorld.GateCore.getZ();
        return Local.move(OffsetX, OffsetY, OffsetZ);
    }

    public AABB renderBounds() {
        return field().inflate(3.0);
    }
}
