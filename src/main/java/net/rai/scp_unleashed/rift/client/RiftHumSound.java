package net.rai.scp_unleashed.rift.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftRegistry;

public class RiftHumSound extends AbstractTickableSoundInstance {
    public static final double Reach = 34.0;

    private final RiftEntity Rift;
    private final float BaseVolume;
    private final float BasePitch;
    private float Loudness;

    public RiftHumSound(RiftEntity Rift) {
        super(RiftRegistry.RiftHum.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.Rift = Rift;
        int Tier = Rift.getRarity().getTier();
        BaseVolume = 0.38f + 0.1f * Tier;
        BasePitch = 1.16f - 0.085f * Tier;
        looping = true;
        delay = 0;
        volume = 0.001f;
        pitch = BasePitch;
        attenuation = SoundInstance.Attenuation.LINEAR;
        follow();
    }

    public RiftEntity getRift() {
        return Rift;
    }

    private void follow() {
        Vec3 Center = Rift.getCenter(1.0f);
        x = Center.x;
        y = Center.y;
        z = Center.z;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        Minecraft Client = Minecraft.getInstance();
        boolean Gone = Rift.isRemoved() || Client.level == null || Rift.level() != Client.level;
        if (!Gone) {
            follow();
            Gone = Client.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z) > Reach * Reach;
        }
        float Extraction = Gone ? 0.0f : Rift.getShownExtraction(1.0f);
        float Collapse = Gone ? 0.0f : Rift.getCollapse(1.0f);
        float Target = Gone ? 0.0f : BaseVolume * Rift.getOpenness(1.0f) * (1.0f + 0.7f * Extraction) * (1.0f - Collapse);
        Loudness += (Math.min(Target, 1.0f) - Loudness) * (Gone ? 0.25f : 0.12f);
        if (Gone && Loudness < 0.01f) {
            stop();
            return;
        }
        volume = Math.max(Loudness, 0.001f);
        pitch = BasePitch * (1.0f + 0.22f * Extraction + 0.5f * Collapse);
    }
}
