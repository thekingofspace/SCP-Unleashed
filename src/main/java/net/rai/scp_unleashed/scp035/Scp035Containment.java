package net.rai.scp_unleashed.scp035;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageChange;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;

public final class Scp035Containment implements ContainedBehaviour {
    public static final Scp035Containment Instance = new Scp035Containment();
    public static final int NpcInterval = 20;
    public static final float UnveilVolume = 0.45f;

    private Scp035Containment() {
    }

    @Override
    public void tick(ServerLevel World, StorageContainerBlockEntity Locker) {
        BlockPos Pos = Locker.getStoragePos();
        long Clock = World.getGameTime() + Math.floorMod(Pos.hashCode(), 240);
        boolean Exposed = Locker.isVisible();
        if (Clock % Scp035Sources.RefreshInterval == 0L) {
            if (Exposed) {
                Scp035Sources.mark(World, Pos);
            } else {
                Scp035Sources.forget(World, Pos);
            }
        }
        if (Exposed && Clock % NpcInterval == 0L) {
            Scp035Lure.drawPeople(World, Locker);
        }
    }

    @Override
    public void changed(ServerLevel World, StorageContainerBlockEntity Locker, StorageChange Change, ItemStack Subject) {
        BlockPos Pos = Locker.getStoragePos();
        if (Change == StorageChange.Removed || !Locker.isVisible()) {
            Scp035Sources.forget(World, Pos);
            return;
        }
        boolean Unveiled = Change == StorageChange.Inserted
                || Change == StorageChange.GlassFitted && !Locker.isOpen()
                || Change == StorageChange.Opened && !Locker.hasGlassFront();
        if (Unveiled) {
            Scp035Sources.mark(World, Pos);
            Vec3 Where = Locker.soundCentre();
            World.playSound(null, Where.x, Where.y, Where.z, Scp035Registry.WhisperLow.get(), SoundSource.BLOCKS, UnveilVolume, 0.9f + World.random.nextFloat() * 0.1f);
        }
    }
}
