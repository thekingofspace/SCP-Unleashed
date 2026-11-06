package net.rai.scp_unleashed.scp472.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp472.Scp472Mind;
import net.rai.scp_unleashed.scp472.Scp472Registry;
import net.rai.scp_unleashed.senses.Senses;

public final class Scp472Overlay {
    public static final ResourceLocation LayerId = Scp472Registry.location("scp472_overlay");
    public static final ResourceLocation Sheet = Scp472Registry.location("textures/gui/scp472/visions.png");
    public static final int SheetSize = 64;
    public static final int EyeWidth = 16;
    public static final int EyeHeight = 8;
    public static final int SymbolCell = 16;
    public static final int SymbolTop = 32;
    public static final int ClosedRow = 24;
    public static final int CellTarget = 64;
    public static final int SpriteTarget = 90;
    public static final int[] StageDepth = {0, 1, 2, 3, 4, 5, 7};
    public static final float[] StageAlpha = {0.0f, 0.16f, 0.24f, 0.32f, 0.4f, 0.48f, 0.58f};
    public static final int ThrobStage = 3;
    public static final float ThrobAlpha = 0.06f;
    public static final float SoftThrobAlpha = 0.03f;
    public static final float CornerShare = 0.4f;
    public static final float InnerFade = 0.6f;
    public static final int JagWidth = 2;
    public static final int JagOdds = 3;
    public static final int DripOdds = 13;
    public static final int DripStage = 2;
    public static final int ShimmerStage = 5;
    public static final int[] Tones = {0x420408, 0x600810, 0x7E0C16, 0x98121C};

    private Scp472Overlay() {
    }

    @FunctionalInterface
    public interface Sink {
        void fill(int Left, int Top, int Right, int Bottom, int Colour);
    }

    public static int cell(int Width, int Height) {
        return Math.max(2, Math.round(Math.min(Width, Height) / (float) CellTarget));
    }

    public static int unit(int Width, int Height) {
        return Math.max(1, Math.round(Math.min(Width, Height) / (float) SpriteTarget));
    }

    public static int jag(long Seed, int Side, int Along, int Stage) {
        long Mixed = Scp472Visions.mix(Seed * 31L + Side, Along / JagWidth);
        int Roll = (int) Math.floorMod(Mixed, 1009L);
        if (Stage >= DripStage && Roll % DripOdds == 0) {
            return 2;
        }
        return Roll % JagOdds == 0 ? 1 : 0;
    }

    public static void ring(int Width, int Height, int Stage, boolean Throb, long Seed, Sink Out) {
        if (Stage <= 0) {
            return;
        }
        int Clamped = Math.min(Stage, StageDepth.length - 1);
        int Size = cell(Width, Height);
        int Columns = Math.max(4, Width / Size);
        int Rows = Math.max(4, Height / Size);
        boolean Strong = Throb && Clamped >= ThrobStage;
        int Depth = StageDepth[Clamped] + (Strong ? 1 : 0);
        float Alpha = StageAlpha[Clamped] + (Throb ? (Strong ? ThrobAlpha : SoftThrobAlpha) : 0.0f);
        int Scan = Depth * 3 + 4;
        for (int Row = 0; Row < Rows; Row++) {
            int EdgeY = Math.min(Row, Rows - 1 - Row);
            if (EdgeY < Depth + 3 || Scan * 2 >= Columns) {
                runs(Width, Height, Columns, Rows, Row, EdgeY, 0, Columns, Depth, Alpha, Seed, Clamped, Out);
            } else {
                runs(Width, Height, Columns, Rows, Row, EdgeY, 0, Scan, Depth, Alpha, Seed, Clamped, Out);
                runs(Width, Height, Columns, Rows, Row, EdgeY, Columns - Scan, Columns, Depth, Alpha, Seed, Clamped, Out);
            }
        }
    }

    private static void runs(int Width, int Height, int Columns, int Rows, int Row, int EdgeY, int From, int To, int Depth, float Alpha, long Seed, int Stage, Sink Out) {
        int RunStart = From;
        int RunColour = 0;
        for (int Column = From; Column <= To; Column++) {
            int Colour = Column < To ? colour(Columns, Rows, Column, Row, EdgeY, Depth, Alpha, Seed, Stage) : 0;
            if (Colour == RunColour) {
                continue;
            }
            if (RunColour != 0) {
                Out.fill(edge(RunStart, Columns, Width), edge(Row, Rows, Height), edge(Column, Columns, Width), edge(Row + 1, Rows, Height), RunColour);
            }
            RunStart = Column;
            RunColour = Colour;
        }
    }

    private static int colour(int Columns, int Rows, int Column, int Row, int EdgeY, int Depth, float Alpha, long Seed, int Stage) {
        int EdgeX = Math.min(Column, Columns - 1 - Column);
        boolean Horizontal = EdgeY <= EdgeX;
        int Along = Horizontal ? Column : Row;
        int Side = Horizontal ? (Row < Rows / 2 ? 0 : 1) : (Column < Columns / 2 ? 2 : 3);
        int Distance = Math.min(Math.min(EdgeX, EdgeY), (int) ((EdgeX + EdgeY) * CornerShare));
        int Reach = Depth + jag(Seed, Side, Along, Stage);
        if (Distance >= Reach) {
            return 0;
        }
        int Tone = Distance == 0 ? 0 : Reach > 1 && Distance == Reach - 1 ? 3 : Math.min(2, Distance);
        float Shade = Alpha * (1.0f - InnerFade * Distance / (float) Reach);
        int Opacity = Mth.clamp(Math.round(Shade * 255.0f), 1, 255);
        return Opacity << 24 | Tones[Tone];
    }

    private static int edge(int Index, int Count, int Span) {
        return Math.round(Index * Span / (float) Count);
    }

    public static long seed(int Stage) {
        long Base = Stage * 7919L;
        if (Stage >= ShimmerStage) {
            Base += Scp472ClientState.clock() / Math.max(1, Scp472ClientState.interval());
        }
        return Base;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Scp472ClientState.heard() == null) {
            return;
        }
        int Stage = Scp472ClientState.stage();
        if (Stage <= 0) {
            return;
        }
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ring(Width, Height, Stage, Scp472ClientState.throbbing(), seed(Stage), Graphics::fill);
        if (Stage >= Scp472Mind.VisionStage && !Senses.isBlindfolded(Client.player)) {
            visions(Graphics, Width, Height);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void visions(GuiGraphics Graphics, int Width, int Height) {
        int Unit = unit(Width, Height);
        PoseStack Pose = Graphics.pose();
        for (Scp472Visions.Vision Each : Scp472Visions.shown()) {
            if (Each.flickeredOut()) {
                continue;
            }
            int SpriteWidth = Each.Eye ? EyeWidth : SymbolCell;
            int SpriteHeight = Each.Eye ? EyeHeight : SymbolCell;
            int U;
            int V;
            if (Each.Eye) {
                int Frame = Each.eyeFrame();
                U = Frame == 4 ? Each.Design * EyeWidth : Frame * EyeWidth;
                V = Frame == 4 ? ClosedRow : Each.Design * EyeHeight;
            } else {
                U = (Each.Design % 4) * SymbolCell;
                V = SymbolTop + (Each.Design / 4) * SymbolCell;
            }
            int Left = Math.round(Each.X * Width) - SpriteWidth * Unit / 2;
            int Top = Math.round(Each.Y * Height) - SpriteHeight * Unit / 2;
            Graphics.setColor(1.0f, 1.0f, 1.0f, Each.alpha());
            Pose.pushPose();
            Pose.translate(Left, Top, 0.0f);
            Pose.scale(Unit, Unit, 1.0f);
            Graphics.blit(Sheet, 0, 0, U, V, SpriteWidth, SpriteHeight, SheetSize, SheetSize);
            Pose.popPose();
        }
    }
}
