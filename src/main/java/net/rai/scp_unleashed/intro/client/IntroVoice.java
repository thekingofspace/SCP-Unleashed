package net.rai.scp_unleashed.intro.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public final class IntroVoice {
    public static final float Jitter = 0.025f;
    private final SoundEvent Main;
    @Nullable
    private final SoundEvent Layer;
    @Nullable
    private final SoundEvent Click;
    private final float Volume;
    private final float LayerVolume;
    private final float LayerPitch;
    private final RandomSource Random = RandomSource.create();
    private final int[] RecentMain = {-1, -1};
    private final int[] RecentLayer = {-1, -1};

    public IntroVoice(SoundEvent Main, @Nullable SoundEvent Layer, @Nullable SoundEvent Click, float Volume, float LayerVolume, float LayerPitch) {
        this.Main = Main;
        this.Layer = Layer;
        this.Click = Click;
        this.Volume = Volume;
        this.LayerVolume = LayerVolume;
        this.LayerPitch = LayerPitch;
    }

    private static int count(SoundEvent Sound) {
        WeighedSoundEvents Found = Minecraft.getInstance().getSoundManager().getSoundEvent(Sound.getLocation());
        return Found == null ? 0 : Math.max(0, Found.getWeight());
    }

    private int choose(SoundEvent Sound, int[] Recent) {
        int Count = count(Sound);
        if (Count <= 0) {
            return 0;
        }
        int Chosen = Random.nextInt(Count);
        for (int Attempt = 0; Attempt < 8 && Count > 2 && (Chosen == Recent[0] || Chosen == Recent[1]); Attempt++) {
            Chosen = Random.nextInt(Count);
        }
        Recent[1] = Recent[0];
        Recent[0] = Chosen;
        return Chosen;
    }

    public void syllable(float Pitch, float Loudness, int Hold) {
        float Shake = 1.0f + (Random.nextFloat() - 0.5f) * 2.0f * Jitter;
        float Level = Loudness * (0.85f + Random.nextFloat() * 0.15f);
        Minecraft Client = Minecraft.getInstance();
        Client.getSoundManager().play(new IntroSyllable(Main, new IntroPick(Random, choose(Main, RecentMain)), Volume * Level, Pitch * Shake, Hold));
        if (Layer != null) {
            Client.getSoundManager().play(new IntroSyllable(Layer, new IntroPick(Random, choose(Layer, RecentLayer)), LayerVolume * Level, Pitch * Shake * LayerPitch, Hold));
        }
    }

    public void click() {
        if (Click != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Click, 1.0f + (Random.nextFloat() - 0.5f) * 0.1f, 0.5f));
        }
    }
}
