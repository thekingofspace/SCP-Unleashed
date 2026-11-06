package net.rai.scp_unleashed.scp096.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp096Censor {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp096_censor");
    public static final int CellsAcross = 9;
    public static final int MinCell = 2;
    public static final int MaxCell = 64;
    public static final int MaxCells = 2400;
    public static final int GreyChance = 12;
    public static final int BrightChance = 3;
    public static final int FlickerTicks = 2;

    private Scp096Censor() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Client.player == null) {
            return;
        }
        float Scale = (float) Client.getWindow().getGuiScale();
        long Phase = Client.level.getGameTime() / FlickerTicks;
        Graphics.pose().pushPose();
        Graphics.pose().scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        for (Scp096Faces.View Each : Scp096Faces.views()) {
            if (Each.Censor()) {
                block(Graphics, Each, Phase, Client.getWindow().getWidth(), Client.getWindow().getHeight());
            }
        }
        Graphics.pose().popPose();
    }

    private static void block(GuiGraphics Graphics, Scp096Faces.View Each, long Phase, int Width, int Height) {
        int Cell = Math.max(MinCell, Math.min(MaxCell, Math.round(Each.Size() / CellsAcross)));
        int Left = (int) Math.floor(Each.Left()) - Cell;
        int Top = (int) Math.floor(Each.Top()) - Cell;
        int Right = (int) Math.ceil(Each.Right()) + Cell;
        int Bottom = (int) Math.ceil(Each.Bottom()) + Cell;
        int Columns = Math.max(3, (Right - Left + Cell - 1) / Cell);
        int Rows = Math.max(3, (Bottom - Top + Cell - 1) / Cell);
        while (Columns * Rows > MaxCells) {
            Cell *= 2;
            Columns = Math.max(3, (Right - Left + Cell - 1) / Cell);
            Rows = Math.max(3, (Bottom - Top + Cell - 1) / Cell);
        }
        int Band = (int) Math.floorMod(Phase, Rows + 6L) - 3;
        for (int Row = 0; Row < Rows; Row++) {
            int Y0 = Top + Row * Cell;
            if (Y0 > Height || Y0 + Cell < 0) {
                continue;
            }
            int Shift = Row == Band ? Cell / 2 : 0;
            for (int Column = 0; Column < Columns; Column++) {
                boolean Corner = (Row == 0 || Row == Rows - 1) && (Column == 0 || Column == Columns - 1);
                if (Corner) {
                    continue;
                }
                int X0 = Left + Column * Cell + (Column == 0 ? 0 : Shift);
                int X1 = Left + (Column + 1) * Cell + (Column == Columns - 1 ? 0 : Shift);
                if (X0 > Width || X1 < 0) {
                    continue;
                }
                Graphics.fill(X0, Y0, X1, Y0 + Cell, colour(Each.Id(), Column, Row, Phase, Row == Band));
            }
        }
    }

    private static int colour(int Id, int Column, int Row, long Phase, boolean Banded) {
        long Hash = mix(Id * 0x9E3779B97F4A7C15L + Column * 0xC2B2AE3D27D4EB4FL + Row * 0x165667B19E3779F9L + Phase * 0x27D4EB2F165667C5L);
        int Shade = 7 + (int) (Hash & 7L);
        int Roll = (int) ((Hash >>> 8) & 127L) % 100;
        if (Roll < BrightChance) {
            Shade = 150 + (int) ((Hash >>> 20) & 63L);
        } else if (Roll < BrightChance + GreyChance) {
            Shade = 46 + (int) ((Hash >>> 20) & 63L);
        }
        if (Banded) {
            Shade = Math.min(255, Shade + 26);
        }
        int Blue = Math.min(255, Shade + (Shade > 40 ? 6 : 2));
        return 0xFF000000 | Shade << 16 | Shade << 8 | Blue;
    }

    private static long mix(long Value) {
        Value ^= Value >>> 33;
        Value *= 0xFF51AFD7ED558CCDL;
        Value ^= Value >>> 33;
        Value *= 0xC4CEB9FE1A85EC53L;
        Value ^= Value >>> 33;
        return Value;
    }
}
