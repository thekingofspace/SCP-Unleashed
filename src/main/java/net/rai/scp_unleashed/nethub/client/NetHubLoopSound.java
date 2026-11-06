package net.rai.scp_unleashed.nethub.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.nethub.NetworkHubBlock;
import net.rai.scp_unleashed.nethub.NetworkHubBlockEntity;

import java.lang.ref.WeakReference;

public class NetHubLoopSound extends AbstractTickableSoundInstance {
    public static final float FadeStep = 0.05f;
    public static final double StopRange = 28.0;

    private final WeakReference<NetworkHubBlockEntity> Hub;
    private float Level;
    private boolean Fading;

    public NetHubLoopSound(NetworkHubBlockEntity Hub, SoundEvent Loop) {
        super(Loop, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.Hub = new WeakReference<>(Hub);
        Vec3 Centre = Hub.getCentre();
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.pitch = 1.0f;
        this.x = Centre.x;
        this.y = Centre.y;
        this.z = Centre.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    public void fadeOut() {
        Fading = true;
    }

    @Override
    public void tick() {
        NetworkHubBlockEntity Current = Hub.get();
        Minecraft Game = Minecraft.getInstance();
        if (Current == null || Current.isRemoved() || Current.getLevel() != Game.level || Game.player == null) {
            stop();
            return;
        }
        BlockState State = Current.getBlockState();
        boolean Wanted = !Fading && State.hasProperty(NetworkHubBlock.Status) && State.getValue(NetworkHubBlock.Status).isPowered()
                && Game.player.distanceToSqr(x, y, z) < StopRange * StopRange;
        Level = Math.max(0.0f, Math.min(1.0f, Level + (Wanted ? FadeStep : -FadeStep * 2.0f)));
        volume = Level;
        if (!Wanted && Level <= 0.0f) {
            stop();
        }
    }
}
