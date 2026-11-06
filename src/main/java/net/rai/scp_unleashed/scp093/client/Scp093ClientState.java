package net.rai.scp_unleashed.scp093.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp093.Scp093Registry;
import net.rai.scp_unleashed.scp093.Scp093Tears;
import net.rai.scp_unleashed.scp093.Scp093Wastes;
import net.rai.scp_unleashed.scp093.network.Scp093FlashPayload;
import net.rai.scp_unleashed.scp093.network.Scp093VisionPayload;

import java.util.ArrayList;
import java.util.List;

public final class Scp093ClientState {
    public static final int VisionFadeIn = 20;
    public static final int VisionFadeOut = 80;
    public static final int MaxFlickers = 6;
    public static final int WhisperMin = 50;
    public static final int WhisperSpread = 110;
    public static final int Torsos = 3;
    public static final double ShakeReach = 28.0;
    public static final float ShakeDegrees = 1.2f;
    public static final float ShakeDecay = 0.8f;

    public record Flicker(int Kind, float X, float Y, float Scale, boolean Mirrored, int Born, int Life, float Alpha) {
    }

    private static int FlashPhase = -1;
    private static int FlashTicks;
    private static int FlashAge;
    private static float Doses;
    private static int Vision;
    private static int VisionAge;
    private static int Claim = -1;
    private static int ClaimAge;
    private static int Ticks;
    private static int NextWhisper;
    private static float Rumble;
    private static float RumbleOld;
    private static final List<Flicker> Flickers = new ArrayList<>();
    private static final RandomSource Random = RandomSource.create();

    private Scp093ClientState() {
    }

    public static void flash(Scp093FlashPayload Payload) {
        if (Payload.Phase() == Scp093FlashPayload.Cancel) {
            FlashPhase = -1;
            return;
        }
        FlashPhase = Payload.Phase();
        FlashTicks = Math.max(1, Payload.Ticks());
        FlashAge = 0;
    }

    public static void vision(Scp093VisionPayload Payload) {
        Doses = Payload.Doses();
        if (Payload.Vision() > Vision) {
            VisionAge = Vision > 0 ? Math.min(VisionAge, VisionFadeIn) : 0;
        }
        Vision = Payload.Vision();
        if (Payload.Claim() >= 0 && Claim < 0) {
            ClaimAge = 0;
        }
        Claim = Payload.Claim();
        if (Vision > 0 && NextWhisper <= 0) {
            NextWhisper = 30;
        }
    }

    public static void reset() {
        FlashPhase = -1;
        Doses = 0.0f;
        Vision = 0;
        Claim = -1;
        Rumble = 0.0f;
        RumbleOld = 0.0f;
        Flickers.clear();
    }

    public static void thud(double X, double Y, double Z, float Weight) {
        Entity Viewer = Minecraft.getInstance().getCameraEntity();
        if (Viewer == null) {
            return;
        }
        double Distance = Viewer.position().distanceTo(new Vec3(X, Y, Z));
        if (Distance >= ShakeReach) {
            return;
        }
        float Near = (float) (1.0 - Distance / ShakeReach);
        Rumble = Math.max(Rumble, Mth.clamp(Weight, 0.0f, 1.5f) * Near * Near);
    }

    public static float rumble(float Partial) {
        return Mth.lerp(Partial, RumbleOld, Rumble);
    }

    public static boolean inWastes(Minecraft Client) {
        return Client.level != null && Scp093Wastes.inside(Client.level);
    }

    public static void tick(Minecraft Client) {
        Ticks++;
        if (FlashPhase >= 0) {
            FlashAge++;
            if (FlashPhase == Scp093FlashPayload.FadeOut && FlashAge > FlashTicks) {
                FlashPhase = -1;
            } else if (FlashPhase == Scp093FlashPayload.FadeIn && FlashAge > FlashTicks + 60) {
                FlashPhase = -1;
            }
        }
        if (Client.isPaused()) {
            return;
        }
        RumbleOld = Rumble;
        Rumble = Rumble < 0.004f ? 0.0f : Rumble * ShakeDecay;
        if (Vision > 0) {
            Vision--;
            VisionAge++;
        }
        if (Claim > 0) {
            Claim--;
        }
        if (Claim >= 0) {
            ClaimAge++;
        }
        Flickers.removeIf(Old -> Ticks - Old.Born() > Old.Life());
        float Strength = intensity(0.0f);
        if (Strength <= 0.01f || Client.player == null) {
            return;
        }
        float Chance = 0.02f + 0.06f * Strength + (claiming() ? 0.2f : 0.0f);
        if (Flickers.size() < MaxFlickers && Random.nextFloat() < Chance) {
            boolean Face = Random.nextFloat() < (claiming() ? 0.5f : 0.22f);
            int Kind = Face ? Torsos : Random.nextInt(Torsos);
            float Scale = Face ? 0.5f + Random.nextFloat() * 0.6f : 0.35f + Random.nextFloat() * 0.5f;
            Flickers.add(new Flicker(Kind, 0.1f + Random.nextFloat() * 0.8f, Face ? 0.3f + Random.nextFloat() * 0.3f : 0.55f + Random.nextFloat() * 0.3f, Scale,
                    Random.nextBoolean(), Ticks, 2 + Random.nextInt(claiming() ? 7 : 4), 0.35f + Random.nextFloat() * 0.5f * Strength));
        }
        if (--NextWhisper <= 0) {
            NextWhisper = (int) ((WhisperMin + Random.nextInt(WhisperSpread)) * (1.2f - 0.6f * Strength));
            whisper(Client.player, Strength);
        }
    }

    private static void whisper(LocalPlayer User, float Strength) {
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double Reach = 1.5 + Random.nextDouble() * 2.5;
        User.level().playLocalSound(User.getX() + Math.cos(Angle) * Reach, User.getEyeY() + Random.nextDouble() - 0.5, User.getZ() + Math.sin(Angle) * Reach,
                Scp093Registry.Whisper.get(), SoundSource.AMBIENT, 0.35f + 0.45f * Strength, 0.85f + Random.nextFloat() * 0.25f, false);
        if (Random.nextFloat() < 0.18f + 0.3f * Strength) {
            User.level().playLocalSound(User.getX(), User.getEyeY(), User.getZ(), Scp093Registry.VisionSting.get(), SoundSource.AMBIENT, 0.25f + 0.35f * Strength,
                    0.8f + Random.nextFloat() * 0.2f, false);
        }
    }

    public static boolean claiming() {
        return Claim >= 0;
    }

    public static float claimShare(float Partial) {
        if (Claim < 0) {
            return 0.0f;
        }
        return Mth.clamp((ClaimAge + Partial) / Scp093Tears.ClaimTicks, 0.0f, 1.0f);
    }

    public static float intensity(float Partial) {
        if (Vision <= 0 && Claim < 0) {
            return 0.0f;
        }
        float Base = Mth.clamp(0.35f + Doses / Scp093Tears.Lethal * 0.65f, 0.0f, 1.0f);
        float In = Mth.clamp((VisionAge + Partial) / VisionFadeIn, 0.0f, 1.0f);
        float Out = Mth.clamp((Vision - Partial) / VisionFadeOut, 0.0f, 1.0f);
        float Value = Base * In * Out;
        return claiming() ? Math.max(Value, 0.6f + 0.4f * claimShare(Partial)) : Value;
    }

    public static float flash(float Partial) {
        if (FlashPhase < 0) {
            return 0.0f;
        }
        float Share = Mth.clamp((FlashAge + Partial) / FlashTicks, 0.0f, 1.0f);
        if (FlashPhase == Scp093FlashPayload.FadeIn) {
            return Share * Share;
        }
        float Left = 1.0f - Share;
        return Left * Left * (3.0f - 2.0f * Left);
    }

    public static int ticks() {
        return Ticks;
    }

    public static List<Flicker> flickers() {
        return Flickers;
    }

    public static float doses() {
        return Doses;
    }
}
