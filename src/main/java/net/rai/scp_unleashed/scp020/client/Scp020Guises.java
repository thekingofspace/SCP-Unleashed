package net.rai.scp_unleashed.scp020.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Scp020Guises {
    public static final int MinIntensity = 25;
    public static final float FewestChance = 0.15f;
    public static final float MostChance = 0.85f;
    public static final int ShortHold = 140;
    public static final int LongHold = 520;
    public static final int HoldJitter = 120;
    public static final int PruneTicks = 200;
    private static final Map<UUID, Guise> Rolled = new HashMap<>();
    private static long Clock;

    private Scp020Guises() {
    }

    private record Guise(boolean Zombie, long Until) {
    }

    public static float chance(int Intensity) {
        if (Intensity < MinIntensity) {
            return 0.0f;
        }
        return Mth.lerp((Intensity - MinIntensity) / (float) (100 - MinIntensity), FewestChance, MostChance);
    }

    public static int hold(int Intensity, RandomSource Random) {
        float Progress = Mth.clamp((Intensity - MinIntensity) / (float) (100 - MinIntensity), 0.0f, 1.0f);
        return Math.round(Mth.lerp(Progress, ShortHold, LongHold)) + Random.nextInt(HoldJitter);
    }

    public static void tick(Minecraft Client) {
        if (Client.level == null || Client.player == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        Clock++;
        int Intensity = Scp020ClientState.intensity();
        if (Intensity < MinIntensity) {
            Rolled.clear();
            return;
        }
        RandomSource Random = Client.player.getRandom();
        float Chance = chance(Intensity);
        Set<UUID> Present = new HashSet<>();
        for (AbstractClientPlayer Other : Client.level.players()) {
            if (Other == Client.player) {
                continue;
            }
            UUID Key = Other.getUUID();
            Present.add(Key);
            Guise Current = Rolled.get(Key);
            if (Current == null || Clock >= Current.Until()) {
                Rolled.put(Key, new Guise(Random.nextFloat() < Chance, Clock + hold(Intensity, Random)));
            }
        }
        if (Clock % PruneTicks == 0) {
            Rolled.keySet().retainAll(Present);
        }
    }

    public static boolean isZombie(Player Target) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Target == Client.player || Target.isInvisible() || Target.isSpectator() || Scp020ClientState.intensity() < MinIntensity) {
            return false;
        }
        if (Client.level.getEntity(Target.getId()) != Target) {
            return false;
        }
        Guise Found = Rolled.get(Target.getUUID());
        return Found != null && Found.Zombie();
    }

    public static void reset() {
        Rolled.clear();
        Clock = 0;
    }
}
