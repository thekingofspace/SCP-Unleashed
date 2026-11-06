package net.rai.scp_unleashed.scp217.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp217Tint {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp217_tint");
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp217/tint.png");
    public static final ResourceLocation PerfectTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp217/tint_perfect.png");
    public static final float PerfectAlpha = 0.35f;
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 36;
    public static final int TintStage = 4;
    public static final float SettledAlpha = 0.5f;
    public static final float WoundAlpha = 0.85f;
    public static final int Steps = 4;

    private Scp217Tint() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.getCameraEntity() != Client.player) {
            return;
        }
        Scp217ClientState.Entry Found = Scp217ClientState.get(Client.player.getId());
        if (Found == null || Found.stage() < TintStage) {
            return;
        }
        float Stepped = Math.round(Found.windingProgress() * Steps) / (float) Steps;
        float Alpha = (Found.perfect() ? PerfectAlpha : Mth.lerp(Stepped, SettledAlpha, WoundAlpha)) * Client.options.screenEffectScale().get().floatValue();
        if (Alpha <= 0.0f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
        Graphics.blit(Found.perfect() ? PerfectTexture : Texture, 0, 0, Graphics.guiWidth(), Graphics.guiHeight(), 0.0f, 0.0f, TextureWidth, TextureHeight, TextureWidth, TextureHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
