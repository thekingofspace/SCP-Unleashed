package net.rai.scp_unleashed.scp079.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp079.Scp079BlockEntity;
import net.rai.scp_unleashed.scp079.Scp079Registry;

import java.util.HashMap;
import java.util.Map;

public class Scp079HumSound extends AbstractTickableSoundInstance {
    public static final float Volume = 0.32f;
    public static final double Reach = 16.0;
    private static final Map<BlockPos, Scp079HumSound> Playing = new HashMap<>();

    private final BlockPos Pos;

    private Scp079HumSound(Scp079BlockEntity Computer) {
        super(Scp079Registry.Hum.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Pos = Computer.getBlockPos().immutable();
        Vec3 Centre = Computer.screenCentre();
        this.looping = true;
        this.delay = 0;
        this.volume = Volume;
        this.pitch = 1.0f;
        this.attenuation = Attenuation.LINEAR;
        this.relative = false;
        this.x = Centre.x;
        this.y = Centre.y;
        this.z = Centre.z;
    }

    public static void update(Scp079BlockEntity Computer) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Computer.isRemoved()) {
            return;
        }
        BlockPos Pos = Computer.getBlockPos();
        Scp079HumSound Current = Playing.get(Pos);
        boolean Wanted = Computer.isPowered() && Client.player.distanceToSqr(Computer.screenCentre()) < Reach * Reach;
        if (Wanted && (Current == null || Current.isStopped())) {
            Scp079HumSound Started = new Scp079HumSound(Computer);
            Playing.put(Pos.immutable(), Started);
            Client.getSoundManager().play(Started);
        } else if (!Wanted && Current != null) {
            Current.stop();
            Playing.remove(Pos);
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || !(Client.level.getBlockEntity(Pos) instanceof Scp079BlockEntity Computer) || !Computer.isPowered()) {
            stop();
            Playing.remove(Pos);
        }
    }
}
