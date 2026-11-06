package net.rai.scp_unleashed.scp860.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.scp860.Scp860Registry;
import org.jetbrains.annotations.Nullable;

public final class Scp860Ambience {
    public static final float CreakChance = 0.004f;
    public static final float SnapChance = 0.0015f;
    public static final float RustleChance = 0.002f;
    public static final double NearReach = 10.0;
    public static final double FarReach = 22.0;
    @Nullable
    private static SoundInstance Loop;

    private Scp860Ambience() {
    }

    public static boolean inside(Minecraft Client) {
        return Client.level != null && Client.player != null && Client.level.dimension() == Scp860Registry.Forest;
    }

    public static void tick(Minecraft Client) {
        if (!inside(Client)) {
            stop(Client);
            return;
        }
        if (Loop == null || !Client.getSoundManager().isActive(Loop)) {
            Loop = new SimpleSoundInstance(Scp860Registry.Wind.get().getLocation(), SoundSource.AMBIENT, 0.6f, 1.0f, SoundInstance.createUnseededRandom(), true, 0,
                    SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
            Client.getSoundManager().play(Loop);
        }
        if (Client.isPaused()) {
            return;
        }
        RandomSource Dice = Client.level.getRandom();
        float Roll = Dice.nextFloat();
        if (Roll < CreakChance) {
            distant(Client, Dice, Scp860Registry.Creak.get(), 0.7f);
        } else if (Roll < CreakChance + SnapChance) {
            distant(Client, Dice, Scp860Registry.Snap.get(), 0.6f);
        } else if (Roll < CreakChance + SnapChance + RustleChance) {
            distant(Client, Dice, Scp860Registry.Rustle.get(), 0.4f);
        }
    }

    private static void distant(Minecraft Client, RandomSource Dice, net.minecraft.sounds.SoundEvent Event, float Volume) {
        double Angle = Dice.nextDouble() * Math.PI * 2.0;
        double Reach = NearReach + Dice.nextDouble() * (FarReach - NearReach);
        double X = Client.player.getX() + Math.cos(Angle) * Reach;
        double Z = Client.player.getZ() + Math.sin(Angle) * Reach;
        Client.level.playLocalSound(X, Client.player.getY() + 2.0, Z, Event, SoundSource.AMBIENT, Volume, 0.85f + Dice.nextFloat() * 0.3f, false);
    }

    public static void stop(Minecraft Client) {
        if (Loop != null) {
            Client.getSoundManager().stop(Loop);
            Loop = null;
        }
    }
}
