package net.rai.scp_unleashed.scp012;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageChange;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.StorageView;

public final class Scp012Containment implements ContainedBehaviour {
    public static final Scp012Containment Instance = new Scp012Containment();
    public static final int NoteInterval = 50;
    public static final double RedNote = 0.25;

    private Scp012Containment() {
    }

    public static boolean isExposed(StorageView Locker) {
        return Locker.isVisible();
    }

    @Override
    public void tick(ServerLevel World, StorageContainerBlockEntity Locker) {
        BlockPos Pos = Locker.getStoragePos();
        long Clock = World.getGameTime() + Math.floorMod(Pos.hashCode(), 240);
        if (Clock % Scp012Sources.RefreshInterval == 0L) {
            if (isExposed(Locker)) {
                Scp012Sources.mark(World, Pos);
            } else {
                Scp012Sources.forget(World, Pos);
            }
        }
        if (Locker.hasGlassFront() && !Locker.isOpen() && Clock % NoteInterval == 0L) {
            RandomSource Random = World.random;
            float Top = Locker.isTall() ? 26.0f : 11.0f;
            Vec3 Front = StorageGeometry.point(Pos, Locker.getFacing(), 5.0 + Random.nextDouble() * 6.0, 6.0 + Random.nextDouble() * (Top - 6.0), -0.6);
            World.sendParticles(ParticleTypes.NOTE, Front.x, Front.y, Front.z, 0, RedNote, 0.0, 0.0, 1.0);
        }
    }

    @Override
    public void changed(ServerLevel World, StorageContainerBlockEntity Locker, StorageChange Change, ItemStack Subject) {
        BlockPos Pos = Locker.getStoragePos();
        if (Change == StorageChange.Removed || !isExposed(Locker)) {
            Scp012Sources.forget(World, Pos);
            return;
        }
        boolean Unveiled = Change == StorageChange.Inserted
                || Change == StorageChange.GlassFitted && !Locker.isOpen()
                || Change == StorageChange.Opened && !Locker.hasGlassFront();
        if (Unveiled) {
            Scp012Sources.mark(World, Pos);
            Vec3 Where = Locker.soundCentre();
            World.playSound(null, Where.x, Where.y, Where.z, Scp012Registry.Unveiled.get(), SoundSource.BLOCKS, 0.6f, 0.94f + World.random.nextFloat() * 0.08f);
        }
    }
}
