package net.rai.scp_unleashed.scp244.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp244.Scp244Registry;
import net.rai.scp_unleashed.scp244.Scp244VaseBlockEntity;

public final class Scp244VaseEffects {
    public static final float PourFrom = 0.4f;
    public static final int BurstCount = 10;
    public static final double PourSpeed = 0.028;
    public static final int VentGap = 46;
    public static final int VentSpread = 30;

    private Scp244VaseEffects() {
    }

    public static void tick(Scp244VaseBlockEntity Vase) {
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null || Vase.getLevel() != World) {
            return;
        }
        boolean Wanted = Vase.isOpen() || Vase.intensity() > 0.0f || Scp244FogEffect.peek(Vase) != null;
        if (!Wanted) {
            return;
        }
        Scp244FogEffect Fog = Scp244FogEffect.of(Vase);
        Fog.update(Game);
        float Lid = Vase.lid(1.0f);
        boolean Pouring = Vase.isOpen() && Lid >= PourFrom;
        RandomSource Random = World.random;
        if (Fog.takeBurst(Pouring)) {
            for (int Index = 0; Index < BurstCount; Index++) {
                pour(World, Vase, Random, 1.6);
            }
        }
        if (!Pouring) {
            return;
        }
        ParticleStatus Status = Game.options.particles().get();
        long Now = World.getGameTime();
        int Count = Status == ParticleStatus.ALL ? (Now % 3 == 0 ? 2 : 1) : Status == ParticleStatus.DECREASED ? (Now % 2 == 0 ? 1 : 0) : (Now % 4 == 0 ? 1 : 0);
        for (int Index = 0; Index < Count; Index++) {
            pour(World, Vase, Random, 1.0);
        }
        if (Fog.ventDue()) {
            Vec3 Mouth = Vase.mouth();
            float Strength = 0.35f + 0.25f * Vase.intensity();
            World.playLocalSound(Mouth.x, Mouth.y, Mouth.z, Scp244Registry.Vent.get(), SoundSource.BLOCKS, Strength, 0.9f + Random.nextFloat() * 0.15f, false);
            World.playLocalSound(Mouth.x, Mouth.y, Mouth.z, Scp244Registry.VentWind.get(), SoundSource.BLOCKS, Strength * 0.8f, 0.85f + Random.nextFloat() * 0.15f, false);
            Fog.ventIn(VentGap + Random.nextInt(VentSpread));
        }
    }

    private static void pour(ClientLevel World, Scp244VaseBlockEntity Vase, RandomSource Random, double Push) {
        Vec3 Mouth = Vase.mouth();
        float Half = Vase.style().mouthHalf();
        float Angle = Random.nextFloat() * Mth.TWO_PI;
        double Reach = Half * (0.4 + Random.nextFloat() * 0.6);
        double X = Mouth.x + Mth.cos(Angle) * Reach;
        double Z = Mouth.z + Mth.sin(Angle) * Reach;
        double Y = Mouth.y + 0.02 + Random.nextFloat() * 0.06;
        double Speed = PourSpeed * Push * (0.6 + Random.nextFloat() * 0.6);
        World.addParticle(Scp244Registry.Vapour.get(), X, Y, Z, Mth.cos(Angle) * Speed, 0.012 + Random.nextFloat() * 0.01, Mth.sin(Angle) * Speed);
    }
}
