package net.rai.scp_unleashed.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class StorageAmbience {
    public static final float OpenEnough = 25.0f;
    public static final double HeaterDepth = 12.5;
    public static final int EmberChance = 3;
    public static final int OpenHazeChance = 5;
    public static final int CrackleChance = 160;
    public static final int OpenMistChance = 2;
    public static final int CreakChance = 260;

    private StorageAmbience() {
    }

    public static void tick(Level World, BlockPos Pos, BlockState State, StorageContainerBlockEntity Locker) {
        StorageEnvironment Environment = State.getValue(StorageContainerBlock.Environment);
        if (!Environment.isConditioned()) {
            return;
        }
        RandomSource Random = World.random;
        Direction Facing = State.getValue(StorageContainerBlock.Facing);
        boolean Tall = Locker.isTall();
        double Top = StorageGeometry.cavityTop(Tall);
        if (Locker.getDoorAngle() <= OpenEnough) {
            return;
        }
        if (Environment == StorageEnvironment.Heated) {
            if (Random.nextInt(EmberChance) == 0) {
                double X = 4.0 + Random.nextDouble() * 8.0;
                double Y = 5.0 + Random.nextDouble() * (Top - 9.0);
                spawn(World, StorageRegistry.Ember.get(), Pos, Facing, X, Y, HeaterDepth, (Random.nextDouble() - 0.5) * 0.006, 0.01 + Random.nextDouble() * 0.012, -0.018 - Random.nextDouble() * 0.014);
            }
            if (Random.nextInt(OpenHazeChance) == 0) {
                double X = 3.5 + Random.nextDouble() * 9.0;
                spawn(World, StorageRegistry.Haze.get(), Pos, Facing, X, Top - 1.0, 1.5, 0.0, 0.02 + Random.nextDouble() * 0.01, -0.006);
            }
            if (Random.nextInt(CrackleChance) == 0) {
                sound(World, Locker, StorageSounds.Heater.get(), 0.9f + Random.nextFloat() * 0.25f);
            }
        } else {
            if (Random.nextInt(OpenMistChance) == 0) {
                double X = 3.0 + Random.nextDouble() * 10.0;
                double Y = 4.5 + Random.nextDouble() * 2.0;
                spawn(World, StorageRegistry.Mist.get(), Pos, Facing, X, Y, 2.5 + Random.nextDouble() * 2.0, (Random.nextDouble() - 0.5) * 0.01, -0.004, -0.025 - Random.nextDouble() * 0.02);
            }
            if (Random.nextInt(CreakChance) == 0) {
                sound(World, Locker, StorageSounds.Frost.get(), 0.85f + Random.nextFloat() * 0.3f);
            }
        }
    }

    private static void spawn(Level World, ParticleOptions Type, BlockPos Pos, Direction Facing, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
        Vec3 At = StorageGeometry.point(Pos, Facing, X, Y, Z);
        Vec3 Speed = StorageGeometry.motion(Facing, SpeedX, SpeedY, SpeedZ);
        World.addParticle(Type, At.x, At.y, At.z, Speed.x, Speed.y, Speed.z);
    }

    private static void sound(Level World, StorageContainerBlockEntity Locker, SoundEvent Event, float Pitch) {
        Vec3 Where = Locker.soundCentre();
        World.playLocalSound(Where.x, Where.y, Where.z, Event, SoundSource.BLOCKS, 1.0f, Pitch, false);
    }
}
