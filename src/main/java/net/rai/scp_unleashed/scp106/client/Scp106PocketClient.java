package net.rai.scp_unleashed.scp106.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp106.Scp106Pocket;
import net.rai.scp_unleashed.scp106.Scp106Registry;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;

public final class Scp106PocketClient {
    public static final ResourceLocation Pipeline = VeilPost.id("scp106_pocket");
    public static final ResourceLocation Vignette = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp106/vignette.png");
    public static final int VignetteWidth = 64;
    public static final int VignetteHeight = 36;
    public static final float FogRed = 0.035f;
    public static final float FogGreen = 0.05f;
    public static final float FogBlue = 0.035f;
    public static final float FogFar = 15.0f;
    public static final float FogSwing = 3.0f;
    public static final float BlurLow = 2.0f;
    public static final float BlurHigh = 4.5f;
    public static final float SigmaShare = 0.58f;
    public static final int WhisperMin = 120;
    public static final int WhisperMax = 300;
    public static final int LaughMin = 500;
    public static final int LaughMax = 1100;
    public static final int DroneEvery = 160;
    private static int WhisperTimer = WhisperMin;
    private static int LaughTimer = LaughMin;
    private static int DroneTimer;
    private static int Ticks;

    private Scp106PocketClient() {
    }

    public static boolean inPocket(Minecraft Client) {
        return Client.level != null && Scp106Pocket.inPocket(Client.level);
    }

    public static float pulse(float PartialTick) {
        return 0.5f + 0.5f * Mth.sin((Ticks + PartialTick) * 0.08f);
    }

    public static float fogFar(float PartialTick) {
        return FogFar + FogSwing * (pulse(PartialTick) - 0.5f) * 2.0f;
    }

    public static void tick(Minecraft Client) {
        boolean Inside = inPocket(Client) && Client.player != null;
        if (!Inside) {
            Ticks = 0;
            return;
        }
        Ticks++;
        if (Client.isPaused()) {
            return;
        }
        Player User = Client.player;
        RandomSource Random = User.getRandom();
        if (--WhisperTimer <= 0) {
            WhisperTimer = WhisperMin + Random.nextInt(WhisperMax - WhisperMin + 1);
            play(Client, Scp106Registry.Whisper.get(), User, Random, 0.75f, 0.85f + Random.nextFloat() * 0.25f, 5.0, 10.0);
        }
        if (--LaughTimer <= 0) {
            LaughTimer = LaughMin + Random.nextInt(LaughMax - LaughMin + 1);
            play(Client, Scp106Registry.PocketLaugh.get(), User, Random, 0.9f, 0.97f + Random.nextFloat() * 0.06f, 8.0, 16.0);
        }
        if (--DroneTimer <= 0) {
            DroneTimer = DroneEvery;
            Client.getSoundManager().play(SimpleSoundInstance.forAmbientAddition(Scp106Registry.Drone.get()));
        }
    }

    private static void play(Minecraft Client, SoundEvent Sound, Player User, RandomSource Random, float Volume, float Pitch, double Near, double Far) {
        double Angle = Random.nextDouble() * Mth.TWO_PI;
        double Distance = Near + Random.nextDouble() * (Far - Near);
        double X = User.getX() + Math.cos(Angle) * Distance;
        double Y = User.getEyeY() + (Random.nextDouble() - 0.3) * 2.0;
        double Z = User.getZ() + Math.sin(Angle) * Distance;
        Client.getSoundManager().play(new SimpleSoundInstance(Sound, SoundSource.AMBIENT, Volume, Pitch, Random, X, Y, Z));
    }

    public static void renderBlur(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || !inPocket(Client) || Client.getCameraEntity() != Client.player || !VeilPost.available()) {
            return;
        }
        Graphics.flush();
        float Sigma = SigmaShare * Mth.lerp(pulse(Delta.getGameTimeDeltaPartialTick(false)), BlurLow, BlurHigh);
        VeilPost.run(Pipeline, Values -> Values.set("Sigma", Sigma));
    }

    public static void renderVignette(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || !inPocket(Client) || Client.options.hideGui) {
            return;
        }
        float Strength = 0.6f + 0.25f * pulse(Delta.getGameTimeDeltaPartialTick(false));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Strength);
        Graphics.blit(Vignette, 0, 0, Graphics.guiWidth(), Graphics.guiHeight(), 0.0f, 0.0f, VignetteWidth, VignetteHeight, VignetteWidth, VignetteHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    public static void reset() {
        Ticks = 0;
        WhisperTimer = WhisperMin;
        LaughTimer = LaughMin;
        DroneTimer = 0;
    }
}
