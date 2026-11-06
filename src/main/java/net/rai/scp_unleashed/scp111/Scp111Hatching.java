package net.rai.scp_unleashed.scp111;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp111Hatching {
    public static final int LeaveTicks = 400;
    public static final double HatchSpread = 0.18;

    private Scp111Hatching() {
    }

    public static boolean isChilled(BlockGetter World, BlockPos Pos) {
        BlockState Below = World.getBlockState(Pos.below());
        return Below.is(BlockTags.ICE) || Below.is(BlockTags.SNOW) || Below.is(Blocks.SNOW_BLOCK) || Below.is(Blocks.POWDER_SNOW);
    }

    @Nullable
    public static Scp111Snail hatchling(ServerLevel World, Vec3 Spot, Scp111Variant Variant, Vec3 Push, @Nullable BlockPos LeaveFrom) {
        Scp111Snail Baby = Scp111Registry.Snail.get().create(World);
        if (Baby == null) {
            return null;
        }
        RandomSource Random = World.random;
        float Yaw = Random.nextFloat() * 360.0f;
        if (Push.horizontalDistanceSqr() > 1.0E-6) {
            Yaw = (float) (Math.atan2(Push.z, Push.x) * (180.0 / Math.PI)) - 90.0f;
        }
        Baby.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Baby.setYBodyRot(Yaw);
        Baby.setYHeadRot(Yaw);
        Baby.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.BREEDING, null);
        Baby.setVariant(Variant);
        Baby.setAge(-Scp111Snail.GrowTicks);
        Baby.setFed(Scp111Snail.HatchlingFed);
        Baby.setPersistenceRequired();
        if (LeaveFrom != null) {
            Baby.leaveFrom(LeaveFrom, LeaveTicks + Random.nextInt(LeaveTicks / 2));
        }
        Baby.setDeltaMovement(Push);
        return World.addFreshEntity(Baby) ? Baby : null;
    }

    public static int hatchClutch(ServerLevel World, BlockPos Pos, Scp111Variant Variant, int Count) {
        RandomSource Random = World.random;
        int Born = 0;
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Spot = new Vec3(Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * HatchSpread * 2.0, Pos.getY() + 0.05,
                    Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * HatchSpread * 2.0);
            Vec3 Push = new Vec3((Random.nextDouble() - 0.5) * 0.08, 0.12, (Random.nextDouble() - 0.5) * 0.08);
            if (hatchling(World, Spot, Variant, Push, null) != null) {
                Born++;
            }
        }
        Vec3 Centre = Vec3.atBottomCenterOf(Pos).add(0.0, 0.12, 0.0);
        shellBurst(World, Centre, Variant, 10 + Count * 4);
        Scp111Breath.puff(World, Centre.add(0.0, 0.1, 0.0), new Vec3(0.0, 1.0, 0.0), Variant, 4, 0.5f);
        Scp111Snail.playLayers(World, Centre, Scp111Registry.HatchLayers, 0.9f, 1.0f + Random.nextFloat() * 0.1f);
        return Born;
    }

    public static void shellBurst(ServerLevel World, Vec3 Centre, Scp111Variant Variant, int Count) {
        ItemParticleOption Shards = new ItemParticleOption(ParticleTypes.ITEM, new Scp111Egg(Variant, 0).stack(1));
        World.sendParticles(Shards, Centre.x, Centre.y, Centre.z, Count, 0.12, 0.06, 0.12, 0.06);
    }
}
