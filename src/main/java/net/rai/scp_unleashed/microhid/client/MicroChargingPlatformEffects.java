package net.rai.scp_unleashed.microhid.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlock;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlockEntity;
import net.rai.scp_unleashed.tesla.TeslaRegistry;

public final class MicroChargingPlatformEffects {
    public static final float SparkChance = 0.18F;

    private MicroChargingPlatformEffects() {
    }

    public static void tick(MicroChargingPlatformBlockEntity Platform) {
        Level World = Platform.getLevel();
        if (World == null) {
            return;
        }
        MicroChargingPlatformSound.tick(Platform);
        if (!MicroChargingPlatformSound.charging(Platform)) {
            return;
        }
        RandomSource Random = World.getRandom();
        if (Random.nextFloat() >= SparkChance) {
            return;
        }
        BlockPos Pos = Platform.getBlockPos();
        double[] Tip = MicroChargingPlatformRenderer.ProngTips[Random.nextInt(MicroChargingPlatformRenderer.ProngTips.length)];
        Vec3 Local = MicroChargingPlatformRenderer.place(Tip[0], Tip[1], Tip[2], Platform.getBlockState().getValue(MicroChargingPlatformBlock.Facing));
        World.addParticle(Random.nextBoolean() ? ParticleTypes.ELECTRIC_SPARK : TeslaRegistry.Spark.get(), Pos.getX() + Local.x, Pos.getY() + Local.y, Pos.getZ() + Local.z,
                Random.nextGaussian() * 0.03, 0.02, Random.nextGaussian() * 0.03);
    }
}
