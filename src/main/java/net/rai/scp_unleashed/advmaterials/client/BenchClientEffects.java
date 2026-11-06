package net.rai.scp_unleashed.advmaterials.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsBenchBlockEntity;

public final class BenchClientEffects {
    public static final double SparkReach = 32.0;
    public static final float SparkChance = 0.35f;

    private BenchClientEffects() {
    }

    public static void tick(AdvancedMaterialsBenchBlockEntity Machine) {
        Level World = Machine.getLevel();
        Minecraft Client = Minecraft.getInstance();
        if (World == null || Client.player == null || Machine.getCharge() < 0.05f) {
            return;
        }
        Vec3 Head = Machine.getHeadPoint();
        if (Client.player.distanceToSqr(Head) > SparkReach * SparkReach) {
            return;
        }
        RandomSource Random = World.getRandom();
        if (Random.nextFloat() < SparkChance * Machine.getCharge()) {
            World.addParticle(ParticleTypes.ELECTRIC_SPARK, Head.x + (Random.nextDouble() - 0.5) * 0.5, Head.y - 0.15, Head.z + (Random.nextDouble() - 0.5) * 0.4,
                    (Random.nextDouble() - 0.5) * 0.08, -0.02, (Random.nextDouble() - 0.5) * 0.08);
        }
    }
}
