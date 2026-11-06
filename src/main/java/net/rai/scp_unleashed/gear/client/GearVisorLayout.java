package net.rai.scp_unleashed.gear.client;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class GearVisorLayout {
    public static final int TexelTarget = 96;

    private GearVisorLayout() {
    }

    public static int texel(int Width, int Height) {
        return Math.max(1, (int) Math.floor(Math.min(Width, Height) / (double) TexelTarget + 0.5));
    }

    public static List<Blit> plan(Spec Layout, int Width, int Height) {
        int Texel = texel(Width, Height);
        int[] Columns = axis(Layout.Columns(), Layout.StretchColumns(), Width / Texel);
        int[] Rows = axis(Layout.Rows(), Layout.StretchRows(), Height / Texel);
        int GridWidth = sum(Columns) * Texel;
        int GridHeight = sum(Rows) * Texel;
        int OffsetX = Math.floorDiv(Width - GridWidth, 2);
        int OffsetY = Math.floorDiv(Height - GridHeight, 2);
        int GridRight = OffsetX + GridWidth;
        int GridBottom = OffsetY + GridHeight;
        List<Blit> Blits = new ArrayList<>();
        margin(Blits, 0, 0, OffsetX, Height);
        margin(Blits, GridRight, 0, Width - GridRight, Height);
        margin(Blits, OffsetX, 0, GridWidth, OffsetY);
        margin(Blits, OffsetX, GridBottom, GridWidth, Height - GridBottom);
        int SourceY = 0;
        int DestY = 0;
        for (int Row = 0; Row < Rows.length; Row++) {
            int SourceX = 0;
            int DestX = 0;
            int SourceHeight = Layout.Rows()[Row];
            for (int Column = 0; Column < Columns.length; Column++) {
                int SourceWidth = Layout.Columns()[Column];
                if (Columns[Column] > 0 && Rows[Row] > 0) {
                    if (Layout.StretchColumns()[Column] && Layout.StretchRows()[Row]) {
                        Blits.add(new Blit(OffsetX + DestX * Texel, OffsetY + DestY * Texel, Columns[Column] * Texel, Rows[Row] * Texel, SourceX, SourceY, SourceWidth, SourceHeight));
                    } else {
                        for (int TileY = 0; TileY < Rows[Row]; TileY += SourceHeight) {
                            for (int TileX = 0; TileX < Columns[Column]; TileX += SourceWidth) {
                                int PieceWidth = Math.min(SourceWidth, Columns[Column] - TileX);
                                int PieceHeight = Math.min(SourceHeight, Rows[Row] - TileY);
                                Blits.add(new Blit(OffsetX + (DestX + TileX) * Texel, OffsetY + (DestY + TileY) * Texel, PieceWidth * Texel, PieceHeight * Texel, SourceX, SourceY, PieceWidth, PieceHeight));
                            }
                        }
                    }
                }
                SourceX += SourceWidth;
                DestX += Columns[Column];
            }
            SourceY += SourceHeight;
            DestY += Rows[Row];
        }
        return Blits;
    }

    private static int[] axis(int[] Sizes, boolean[] Stretch, int Available) {
        int Fixed = 0;
        int Count = 0;
        for (int Index = 0; Index < Sizes.length; Index++) {
            if (Stretch[Index]) {
                Count++;
            } else {
                Fixed += Sizes[Index];
            }
        }
        int Each = Math.max(0, Math.floorDiv(Available - Fixed, Count));
        Each -= Each % 2;
        int[] Result = new int[Sizes.length];
        for (int Index = 0; Index < Sizes.length; Index++) {
            Result[Index] = Stretch[Index] ? Each : Sizes[Index];
        }
        return Result;
    }

    private static int sum(int[] Values) {
        int Total = 0;
        for (int Value : Values) {
            Total += Value;
        }
        return Total;
    }

    private static void margin(List<Blit> Blits, int X, int Y, int Width, int Height) {
        if (Width > 0 && Height > 0) {
            Blits.add(new Blit(X, Y, Width, Height, 0, 0, 1, 1));
        }
    }

    public record Spec(ResourceLocation Texture, int TextureWidth, int TextureHeight, int[] Columns, boolean[] StretchColumns, int[] Rows, boolean[] StretchRows) {
    }

    public record Blit(int X, int Y, int Width, int Height, int U, int V, int RegionWidth, int RegionHeight) {
    }
}
