package net.rai.scp_unleashed.worldend.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.worldend.WorldEndRegistry;
import org.joml.Matrix4f;

public final class WorldEndOverlay {
    public static final ResourceLocation Layer = WorldEndRegistry.location("worldend_flesh");
    public static final ResourceLocation Tile = WorldEndRegistry.location("textures/gui/worldend/flesh.png");
    public static final ResourceLocation Edge = WorldEndRegistry.location("textures/gui/worldend/flesh_edge.png");
    public static final ResourceLocation Eye = WorldEndRegistry.location("textures/gui/worldend/flesh_eye.png");
    public static final int Cell = 8;
    public static final int Sheet = 64;
    public static final int EyeFrames = 4;
    public static final int Sectors = 14;
    public static final float EyeChance = 0.035f;
    public static final float BigEyeChance = 0.012f;

    private WorldEndOverlay() {
    }

    public static float hash(int X, int Y, int Seed) {
        int Value = X * 73856093 ^ Y * 19349663 ^ Seed * 83492791;
        Value ^= Value >>> 13;
        Value *= 0x5bd1e995;
        Value ^= Value >>> 15;
        return (Value & 0xFFFF) / 65535.0f;
    }

    public static float arrival(int Column, int Row, int Columns, int Rows) {
        float Nx = (Column + 0.5f) / Columns;
        float Ny = (Row + 0.5f) / Rows;
        float EdgeDistance = Math.min(Math.min(Nx, 1.0f - Nx), Math.min(Ny, 1.0f - Ny)) * 2.0f;
        double Angle = Math.atan2(Ny - 0.5f, Nx - 0.5f);
        int Sector = (int) Math.floor((Angle + Math.PI) / (Math.PI * 2.0) * Sectors) % Sectors;
        float Reach = 0.65f + 0.55f * hash(Sector, 3, 11);
        float Jitter = hash(Column, Row, 5) * 0.08f + hash(Column / 3, Row / 3, 7) * 0.1f;
        return Mth.clamp((EdgeDistance * Reach + Jitter) / 1.25f, 0.0f, 0.995f);
    }

    public static boolean covered(int Column, int Row, int Columns, int Rows, float Cover) {
        if (Column < 0 || Row < 0 || Column >= Columns || Row >= Rows) {
            return true;
        }
        return Cover >= arrival(Column, Row, Columns, Rows);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        if (!WorldEndClient.visible()) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Cover = WorldEndClient.cover(Partial);
        float Dark = WorldEndClient.darkness(Partial);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        if (Cover > 0.0005f) {
            int Columns = (Width + Cell - 1) / Cell;
            int Rows = (Height + Cell - 1) / Cell;
            float Pulse = WorldEndClient.beat(Partial);
            float Shade = 0.86f + 0.14f * Pulse;
            Matrix4f Matrix = Graphics.pose().last().pose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            for (int Pass = 0; Pass < 3; Pass++) {
                ResourceLocation Texture = Pass == 0 ? Tile : Pass == 1 ? Edge : Eye;
                RenderSystem.setShaderTexture(0, Texture);
                BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                boolean Any = false;
                for (int Row = 0; Row < Rows; Row++) {
                    for (int Column = 0; Column < Columns; Column++) {
                        float Arrive = arrival(Column, Row, Columns, Rows);
                        if (Cover < Arrive) {
                            continue;
                        }
                        boolean Front = !covered(Column - 1, Row, Columns, Rows, Cover) || !covered(Column + 1, Row, Columns, Rows, Cover)
                                || !covered(Column, Row - 1, Columns, Rows, Cover) || !covered(Column, Row + 1, Columns, Rows, Cover);
                        int X = Column * Cell;
                        int Y = Row * Cell;
                        if (Pass == 0 && !Front) {
                            float U = (X % Sheet) / (float) Sheet;
                            float V = (Y % Sheet) / (float) Sheet;
                            quad(Builder, Matrix, X, Y, Cell, U, V, Cell / (float) Sheet, Cell / (float) Sheet, Shade);
                            Any = true;
                        } else if (Pass == 1 && Front) {
                            float U = ((X + Row * 24) % Sheet) / (float) Sheet;
                            float V = (Y % Sheet) / (float) Sheet;
                            quad(Builder, Matrix, X, Y, Cell, U, V, Cell / (float) Sheet, Cell / (float) Sheet, Shade);
                            Any = true;
                        } else if (Pass == 2 && !Front && hash(Column, Row, 31) < EyeChance && Cover - Arrive > 0.12f) {
                            int Frame = eyeFrame(Column, Row, WorldEndClient.elapsed() + Partial, Cover - Arrive);
                            int Big = hash(Column, Row, 31) < BigEyeChance ? Cell : 0;
                            quad(Builder, Matrix, X - Big / 2, Y - Big / 2, Cell + Big, Frame / (float) EyeFrames, 0.0f, 1.0f / EyeFrames, 1.0f, Shade);
                            Any = true;
                        }
                    }
                }
                if (Any) {
                    BufferUploader.drawWithShader(Builder.buildOrThrow());
                } else {
                    Builder.build();
                }
            }
            RenderSystem.disableBlend();
        }
        if (Dark > 0.0f) {
            Graphics.fill(0, 0, Width, Height, ((int) (Dark * 255.0f) << 24) | 0x050102);
        }
    }

    public static int eyeFrame(int Column, int Row, float Time, float Age) {
        if (Age < 0.16f) {
            return 2;
        }
        if (Age < 0.2f) {
            return 1;
        }
        float Period = 50.0f + hash(Column, Row, 41) * 70.0f;
        float Phase = (Time + hash(Column, Row, 43) * Period) % Period;
        if (Phase < 3.0f) {
            return 2;
        }
        if (Phase < 5.0f) {
            return 1;
        }
        return hash(Column, Row, 47) > 0.6f ? 3 : 0;
    }

    private static void quad(BufferBuilder Builder, Matrix4f Matrix, int X, int Y, int Size, float U, float V, float Du, float Dv, float Shade) {
        Builder.addVertex(Matrix, X, Y + Size, 0.0f).setUv(U, V + Dv).setColor(Shade, Shade, Shade, 1.0f);
        Builder.addVertex(Matrix, X + Size, Y + Size, 0.0f).setUv(U + Du, V + Dv).setColor(Shade, Shade, Shade, 1.0f);
        Builder.addVertex(Matrix, X + Size, Y, 0.0f).setUv(U + Du, V).setColor(Shade, Shade, Shade, 1.0f);
        Builder.addVertex(Matrix, X, Y, 0.0f).setUv(U, V).setColor(Shade, Shade, Shade, 1.0f);
    }
}
