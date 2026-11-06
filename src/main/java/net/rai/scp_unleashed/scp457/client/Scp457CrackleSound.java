package net.rai.scp_unleashed.scp457.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class Scp457CrackleSound extends AbstractTickableSoundInstance {
    public static final double Range = 24.0;
    public static final float FaintVolume = 0.22f;
    private static final Map<Scp457, List<Scp457CrackleSound>> Playing = new WeakHashMap<>();

    private final Scp457 Owner;
    private final float Loudness;

    public Scp457CrackleSound(Scp457 Owner, SoundEvent Sound, float Loudness) {
        super(Sound, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.Owner = Owner;
        this.Loudness = Loudness;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0f;
        this.x = Owner.getX();
        this.y = Owner.getY() + Owner.getBbHeight() * 0.5;
        this.z = Owner.getZ();
    }

    public static void ensure(Scp457 Owner) {
        List<Scp457CrackleSound> Current = Playing.get(Owner);
        if (Current != null && Current.stream().noneMatch(Scp457CrackleSound::isStopped)) {
            return;
        }
        List<Scp457CrackleSound> Made = new ArrayList<>();
        Made.add(new Scp457CrackleSound(Owner, Scp457Registry.Crackle.get(), FaintVolume));
        if (Current != null) {
            Current.forEach(Scp457CrackleSound::halt);
        }
        Playing.put(Owner, Made);
        for (Scp457CrackleSound Layer : Made) {
            Minecraft.getInstance().getSoundManager().play(Layer);
        }
    }

    public static void clear() {
        Playing.values().forEach(Sounds -> Sounds.forEach(Scp457CrackleSound::halt));
        Playing.clear();
    }

    public void halt() {
        stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        Minecraft Client = Minecraft.getInstance();
        if (Owner.isRemoved() || Owner.deathTime > 0 || Owner.level() != Client.level || Client.player == null || Client.player.distanceToSqr(Owner) > (Range + 8.0) * (Range + 8.0)) {
            stop();
            return;
        }
        x = Owner.getX();
        y = Owner.getY() + Owner.getBbHeight() * 0.5;
        z = Owner.getZ();
        volume = Loudness * Owner.flameStrength(1.0f);
    }
}
