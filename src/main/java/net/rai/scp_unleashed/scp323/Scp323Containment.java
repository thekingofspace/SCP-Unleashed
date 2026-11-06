package net.rai.scp_unleashed.scp323;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageChange;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;

public final class Scp323Containment implements ContainedBehaviour {
    public static final Scp323Containment Instance = new Scp323Containment();
    public static final int NpcInterval = 20;
    public static final float UnveilVolume = 0.45f;

    private Scp323Containment() {
    }

    @Override
    public void tick(ServerLevel World, StorageContainerBlockEntity Locker) {
        BlockPos Pos = Locker.getStoragePos();
        long Clock = World.getGameTime() + Math.floorMod(Pos.hashCode(), 240);
        boolean Exposed = Locker.isVisible();
        if (Clock % Scp323Sources.RefreshInterval == 0L) {
            if (Exposed) {
                Scp323Sources.mark(World, Pos);
            } else {
                Scp323Sources.forget(World, Pos);
            }
        }
        if (Exposed && Clock % NpcInterval == 0L) {
            Scp323Lure.drawPeople(World, Locker);
        }
    }

    @Override
    public void changed(ServerLevel World, StorageContainerBlockEntity Locker, StorageChange Change, ItemStack Subject) {
        BlockPos Pos = Locker.getStoragePos();
        if (Change == StorageChange.Removed || !Locker.isVisible()) {
            Scp323Sources.forget(World, Pos);
            return;
        }
        boolean Unveiled = Change == StorageChange.Inserted
                || Change == StorageChange.GlassFitted && !Locker.isOpen()
                || Change == StorageChange.Opened && !Locker.hasGlassFront();
        if (Unveiled) {
            Scp323Sources.mark(World, Pos);
            Vec3 Where = Locker.soundCentre();
            World.playSound(null, Where.x, Where.y, Where.z, Scp323Registry.WhisperLow.get(), SoundSource.BLOCKS, UnveilVolume, 0.9f + World.random.nextFloat() * 0.1f);
        }
    }
}
