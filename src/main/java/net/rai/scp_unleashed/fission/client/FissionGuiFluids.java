package net.rai.scp_unleashed.fission.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public final class FissionGuiFluids {
    public static final int Tile = 16;

    private FissionGuiFluids() {
    }

    public static void drawFluid(GuiGraphics Graphics, Fluid Liquid, int X, int Y, int Width, int Height) {
        IClientFluidTypeExtensions Extensions = IClientFluidTypeExtensions.of(Liquid);
        drawFluid(Graphics, Extensions.getStillTexture(), Extensions.getTintColor(), X, Y, Width, Height);
    }

    public static void drawFluid(GuiGraphics Graphics, ResourceLocation SpriteId, int Tint, int X, int Y, int Width, int Height) {
        if (Width <= 0 || Height <= 0) {
            return;
        }
        TextureAtlasSprite Sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SpriteId);
        float Red = (Tint >> 16 & 255) / 255.0f;
        float Green = (Tint >> 8 & 255) / 255.0f;
        float Blue = (Tint & 255) / 255.0f;
        float Alpha = (Tint >>> 24) == 0 ? 1.0f : (Tint >>> 24) / 255.0f;
        RenderSystem.enableBlend();
        Graphics.enableScissor(X, Y, X + Width, Y + Height);
        int Bottom = Y + Height;
        for (int TileY = Bottom - Tile; TileY > Y - Tile; TileY -= Tile) {
            for (int TileX = X; TileX < X + Width; TileX += Tile) {
                Graphics.blit(TileX, TileY, 0, Tile, Tile, Sprite, Red, Green, Blue, Alpha);
            }
        }
        Graphics.disableScissor();
        RenderSystem.disableBlend();
    }
}
