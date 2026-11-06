package net.rai.scp_unleashed.scp529;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class Scp529Effects {
    public static final int ScratchBits = 6;
    public static final double PawReach = 0.32;

    private Scp529Effects() {
    }

    public static void voice(Entity Owner, SoundEvent Sound, float Volume, float Pitch) {
        Owner.playSound(Sound, Volume, Pitch);
    }

    public static void hearts(ServerLevel World, Entity Owner, int Count) {
        World.sendParticles(ParticleTypes.HEART, Owner.getX(), Owner.getY() + Owner.getBbHeight() * 0.9, Owner.getZ(), Count, 0.2, 0.1, 0.2, 0.02);
    }

    public static void scratch(ServerLevel World, Entity Owner) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, Owner.getYRot()).scale(PawReach);
        BlockPos Below = BlockPos.containing(Owner.getX() + Facing.x, Owner.getY() - 0.2, Owner.getZ() + Facing.z);
        BlockState Ground = World.getBlockState(Below);
        if (Ground.isAir()) {
            return;
        }
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), Owner.getX() + Facing.x, Owner.getY() + 0.05, Owner.getZ() + Facing.z, ScratchBits, 0.12, 0.02,
                0.12, 0.05);
    }
}
