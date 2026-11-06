package net.rai.scp_unleashed.orbital.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.orbital.OrbitalStrikeEntity;
import org.joml.Vector3f;

public final class OrbitalClientEffects {
    public static final DustParticleOptions RedDust = new DustParticleOptions(new Vector3f(1.0F, 0.12F, 0.08F), 1.6F);
    public static final DustParticleOptions EmberDust = new DustParticleOptions(new Vector3f(1.0F, 0.55F, 0.12F), 2.2F);
    public static final double ShakeReach = 128.0;
    public static final double FlashReach = 160.0;
    public static final float ShakeDegrees = 3.2F;
    public static final float WarningShakeDegrees = 0.35F;
    public static final int ShakeTicks = 34;
    public static final int ShakeHold = 8;
    public static final int FlashTicks = 12;
    public static final int FlashColour = 0xFFF0DC;
    public static final float FlashPeak = 0.85F;
    private static long ShakeStart = Long.MIN_VALUE / 2;
    private static float ShakeStrength;
    private static long FlashStart = Long.MIN_VALUE / 2;
    private static float FlashStrength;
    private static float WarningShake;

    private OrbitalClientEffects() {
    }

    private static long now() {
        Minecraft Game = Minecraft.getInstance();
        return Game.level == null ? 0L : Game.level.getGameTime();
    }

    public static void tick(OrbitalStrikeEntity Strike) {
        if (!(Strike.level() instanceof ClientLevel World)) {
            return;
        }
        RandomSource Random = World.getRandom();
        int Age = Strike.getAge();
        int Charge = Strike.getCharge();
        float Radius = Strike.getRadius();
        double X = Strike.getX();
        double Y = Strike.getY();
        double Z = Strike.getZ();
        if (Age < Charge) {
            float Progress = Age / (float) Charge;
            float Ring = Radius * (1.7F - 0.7F * Progress);
            int Count = 4 + Math.round(Progress * 8.0F);
            for (int Index = 0; Index < Count; Index++) {
                float Angle = Random.nextFloat() * Mth.TWO_PI;
                double Px = X + Mth.cos(Angle) * Ring;
                double Pz = Z + Mth.sin(Angle) * Ring;
                World.addParticle(RedDust, true, Px, Y + 0.2 + Random.nextDouble() * 1.5, Pz, 0.0, 0.0, 0.0);
            }
            for (int Index = 0; Index < 2; Index++) {
                World.addParticle(ParticleTypes.ELECTRIC_SPARK, true, X + (Random.nextDouble() - 0.5) * 0.6, Y + Random.nextDouble() * 10.0,
                        Z + (Random.nextDouble() - 0.5) * 0.6, 0.0, -0.4, 0.0);
            }
            if (Age % 4 == 0) {
                World.addParticle(ParticleTypes.END_ROD, true, X, Y + 0.3, Z, 0.0, 0.08, 0.0);
            }
            LocalPlayer Viewer = Minecraft.getInstance().player;
            if (Viewer != null) {
                double Distance = Viewer.position().distanceTo(Strike.position());
                WarningShake = Math.max(WarningShake, (float) Mth.clamp(1.0 - Distance / 24.0, 0.0, 1.0) * Progress);
            }
            return;
        }
        int Since = Age - Charge;
        if (Since < OrbitalStrikeEntity.LingerTicks) {
            for (int Index = 0; Index < 6; Index++) {
                float Angle = Random.nextFloat() * Mth.TWO_PI;
                float Reach = Random.nextFloat() * Radius;
                double Px = X + Mth.cos(Angle) * Reach;
                double Pz = Z + Mth.sin(Angle) * Reach;
                World.addParticle(Index % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE, true, Px, Y + 0.2, Pz, 0.0, 0.08 + Random.nextDouble() * 0.12, 0.0);
            }
            if (Since % 3 == 0) {
                World.addParticle(ParticleTypes.LAVA, true, X + (Random.nextDouble() - 0.5) * Radius, Y + 0.3, Z + (Random.nextDouble() - 0.5) * Radius, 0.0, 0.0, 0.0);
            }
            for (int Index = 0; Index < 3; Index++) {
                World.addParticle(EmberDust, true, X + (Random.nextDouble() - 0.5) * Radius, Y + Random.nextDouble() * 24.0, Z + (Random.nextDouble() - 0.5) * Radius, 0.0, 0.0, 0.0);
            }
        } else if (Since % 2 == 0) {
            World.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, X + (Random.nextDouble() - 0.5) * Radius, Y + 0.3, Z + (Random.nextDouble() - 0.5) * Radius, 0.0, 0.04, 0.0);
        }
    }

    public static void strike(OrbitalStrikeEntity Strike) {
        if (!(Strike.level() instanceof ClientLevel World)) {
            return;
        }
        RandomSource Random = World.getRandom();
        double X = Strike.getX();
        double Y = Strike.getY();
        double Z = Strike.getZ();
        float Radius = Strike.getRadius();
        World.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, X, Y + 0.5, Z, 0.0, 0.0, 0.0);
        World.addParticle(ParticleTypes.FLASH, true, X, Y + 1.0, Z, 0.0, 0.0, 0.0);
        for (int Index = 0; Index < 70; Index++) {
            float Angle = Random.nextFloat() * Mth.TWO_PI;
            double Speed = 0.35 + Random.nextDouble() * 0.6;
            World.addParticle(Index % 3 == 0 ? ParticleTypes.LAVA : ParticleTypes.FLAME, true, X, Y + 0.4, Z, Mth.cos(Angle) * Speed, 0.2 + Random.nextDouble() * 0.5,
                    Mth.sin(Angle) * Speed);
        }
        for (int Index = 0; Index < 48; Index++) {
            float Angle = Index * Mth.TWO_PI / 48.0F;
            World.addParticle(ParticleTypes.LARGE_SMOKE, true, X + Mth.cos(Angle) * Radius, Y + 0.3, Z + Mth.sin(Angle) * Radius, Mth.cos(Angle) * 0.6, 0.05,
                    Mth.sin(Angle) * 0.6);
            World.addParticle(ParticleTypes.FIREWORK, true, X, Y + 0.6, Z, Mth.cos(Angle) * 0.9, 0.1, Mth.sin(Angle) * 0.9);
        }
        for (int Index = 0; Index < 8; Index++) {
            World.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, X + (Random.nextDouble() - 0.5) * Radius, Y + 0.5, Z + (Random.nextDouble() - 0.5) * Radius, 0.0, 0.12, 0.0);
        }
        LocalPlayer Viewer = Minecraft.getInstance().player;
        if (Viewer == null) {
            return;
        }
        double Distance = Viewer.position().distanceTo(Strike.position());
        float Shake = (float) Mth.clamp(1.0 - Distance / ShakeReach, 0.0, 1.0);
        if (Shake > 0.0F) {
            ShakeStart = now();
            ShakeStrength = Math.max(Shake * Shake, 0.15F);
        }
        float Flash = (float) Mth.clamp(1.0 - Distance / FlashReach, 0.0, 1.0);
        if (Flash > 0.0F) {
            FlashStart = now();
            FlashStrength = FlashPeak * Flash;
        }
    }

    public static void clientTick() {
        WarningShake = Math.max(0.0F, WarningShake - 0.08F);
    }

    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        float Partial = (float) Event.getPartialTick();
        float Age = now() + Partial - ShakeStart;
        float Amount = WarningShake * WarningShakeDegrees;
        if (Age >= 0.0F && Age < ShakeTicks) {
            float Left = Age < ShakeHold ? 1.0F : 1.0F - (Age - ShakeHold) / (ShakeTicks - ShakeHold);
            Amount += ShakeDegrees * ShakeStrength * Left * Left;
        }
        if (Amount <= 0.0F) {
            return;
        }
        float Clock = now() + Partial;
        Event.setYaw(Event.getYaw() + Amount * Mth.sin(Clock * 3.3F));
        Event.setPitch(Event.getPitch() + Amount * Mth.cos(Clock * 2.6F + 0.9F));
        Event.setRoll(Event.getRoll() + Amount * 0.6F * Mth.sin(Clock * 4.1F + 1.7F));
    }

    public static void flash(GuiGraphics Graphics, DeltaTracker Tracker) {
        float Age = now() + Tracker.getGameTimeDeltaPartialTick(false) - FlashStart;
        if (Age < 0.0F || Age >= FlashTicks || FlashStrength <= 0.0F) {
            return;
        }
        float Left = 1.0F - Age / FlashTicks;
        int Alpha = Mth.clamp(Math.round(255.0F * FlashStrength * Left * Left), 0, 255);
        if (Alpha <= 0) {
            return;
        }
        Graphics.fill(0, 0, Graphics.guiWidth(), Graphics.guiHeight(), FastColor.ARGB32.color(Alpha, FlashColour));
    }

    public static void reset() {
        ShakeStart = Long.MIN_VALUE / 2;
        FlashStart = Long.MIN_VALUE / 2;
        ShakeStrength = 0.0F;
        FlashStrength = 0.0F;
        WarningShake = 0.0F;
    }
}
