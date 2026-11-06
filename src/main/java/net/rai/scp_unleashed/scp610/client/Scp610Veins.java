package net.rai.scp_unleashed.scp610.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp610.Scp610Infection;
import net.rai.scp_unleashed.scp610.Scp610Sounds;

public final class Scp610Veins {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp610_veins");
    public static final int TextureWidth = 128;
    public static final int TextureHeight = 72;
    public static final float[] BaseAlpha = {0.25f, 0.4f, 0.55f, 0.7f};
    public static final int[] BeatTicks = {60, 50, 40, 30};
    public static final int Steps = 4;
    public static final int HeartStage = 3;
    private static int Clock;

    private Scp610Veins() {
    }

    public static ResourceLocation texture(int Stage) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp610/veins_" + Mth.clamp(Stage, 1, Scp610Infection.Stages) + ".png");
    }

    public static void tick() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.isPaused()) {
            return;
        }
        int Stage = Scp610ClientState.stage(Client.player.getId());
        if (Stage <= 0) {
            Clock = 0;
            return;
        }
        Clock++;
        int Beat = BeatTicks[Mth.clamp(Stage, 1, Scp610Infection.Stages) - 1];
        if (Stage >= HeartStage && Clock % Beat == 0) {
            Client.player.playSound(Scp610Sounds.Heartbeat.get(), 0.6f, 0.9f);
        }
    }

    public static float pulse(int Stage, float Partial) {
        int Beat = BeatTicks[Mth.clamp(Stage, 1, Scp610Infection.Stages) - 1];
        float Phase = ((Clock + Partial) % Beat) / Beat;
        float Thump = Phase < 0.15f ? Phase / 0.15f : Math.max(0.0f, 1.0f - (Phase - 0.15f) / 0.5f);
        return Math.round(Thump * Steps) / (float) Steps;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.getCameraEntity() != Client.player || Client.options.hideGui) {
            return;
        }
        int Stage = Scp610ClientState.stage(Client.player.getId());
        if (Stage <= 0) {
            return;
        }
        float Base = BaseAlpha[Mth.clamp(Stage, 1, Scp610Infection.Stages) - 1];
        float Alpha = (Base * 0.75f + Base * 0.25f * pulse(Stage, Delta.getGameTimeDeltaPartialTick(false))) * Client.options.screenEffectScale().get().floatValue();
        if (Alpha <= 0.0f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
        Graphics.blit(texture(Stage), 0, 0, Graphics.guiWidth(), Graphics.guiHeight(), 0.0f, 0.0f, TextureWidth, TextureHeight, TextureWidth, TextureHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
