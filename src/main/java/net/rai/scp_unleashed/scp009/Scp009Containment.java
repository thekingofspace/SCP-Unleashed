package net.rai.scp_unleashed.scp009;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageChange;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageEnvironment;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.StorageView;

public final class Scp009Containment implements ContainedBehaviour {
    public static final Scp009Containment Instance = new Scp009Containment();
    public static final int ParticleInterval = 6;
    public static final int WarningInterval = 60;
    public static final double SmallCore = 9.0;
    public static final double TallCore = 16.0;

    private Scp009Containment() {
    }

    public static boolean isContained(StorageView Locker) {
        return Locker.getEnvironment() == StorageEnvironment.Cold;
    }

    @Override
    public void tick(ServerLevel World, StorageContainerBlockEntity Locker) {
        if (isContained(Locker)) {
            return;
        }
        BlockPos Pos = Locker.getStoragePos();
        Direction Facing = Locker.getFacing();
        boolean Tall = Locker.isTall();
        long Clock = World.getGameTime() + Math.floorMod(Pos.hashCode(), 240);
        RandomSource Random = World.random;
        if (Clock % Scp009Exposure.Interval == 0L) {
            Vec3 Core = StorageGeometry.point(Pos, Facing, StorageGeometry.CentreX, Tall ? TallCore : SmallCore, StorageGeometry.CentreZ);
            BlockPos Upper = Pos.above();
            Scp009Exposure.expose(World, Core, Cell -> Cell.equals(Pos) || Tall && Cell.equals(Upper));
        }
        if (Clock % ParticleInterval == 0L) {
            double Top = Tall ? 28.0 : 12.0;
            Vec3 Front = StorageGeometry.point(Pos, Facing, 3.0 + Random.nextDouble() * 10.0, 4.0 + Random.nextDouble() * (Top - 4.0), -0.8);
            World.sendParticles(Scp009Registry.Glint.get(), Front.x, Front.y, Front.z, 1, 0.02, 0.02, 0.02, 0.006);
        }
        if (Clock % WarningInterval == 0L) {
            Vec3 Where = Locker.soundCentre();
            World.playSound(null, Where.x, Where.y, Where.z, Scp009Registry.Warning.get(), SoundSource.BLOCKS, 1.0f, 0.96f + Random.nextFloat() * 0.08f);
        }
    }

    @Override
    public void changed(ServerLevel World, StorageContainerBlockEntity Locker, StorageChange Change, ItemStack Subject) {
        if (Change != StorageChange.Inserted && Change != StorageChange.EnvironmentChanged) {
            return;
        }
        Vec3 Where = Locker.soundCentre();
        boolean Safe = isContained(Locker);
        World.playSound(null, Where.x, Where.y, Where.z, Safe ? Scp009Registry.Settle.get() : Scp009Registry.Warning.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
    }
}
