package net.rai.scp_unleashed.guns;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public interface GunStruckBlock {
    void onGunHit(ServerLevel World, BlockPos Pos, BlockState State, BlockHitResult Hit, Entity User, float Damage);
}
