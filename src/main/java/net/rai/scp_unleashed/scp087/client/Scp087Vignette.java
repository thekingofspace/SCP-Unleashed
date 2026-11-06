package net.rai.scp_unleashed.scp087.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp087Vignette {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp087/vignette.png");
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 36;

    private Scp087Vignette() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || !Scp087ClientState.inStairwell(Client)) {
            return;
        }
        float Strength = Scp087ClientState.shadow(Delta.getGameTimeDeltaPartialTick(false));
        if (Strength <= 0.01f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Strength);
        Graphics.blit(Texture, 0, 0, Graphics.guiWidth(), Graphics.guiHeight(), 0.0f, 0.0f, TextureWidth, TextureHeight, TextureWidth, TextureHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
