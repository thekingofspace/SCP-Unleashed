package net.rai.scp_unleashed.computer.client.apps;

import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.computer.client.OsLayout;
import net.rai.scp_unleashed.computer.client.OsUi;
import net.rai.scp_unleashed.computer.data.ComputerFile;

import java.util.List;

public final class NoteView {
    private NoteView() {
    }

    public static void draw(OsUi Ui, int X, int Y, int W, int H, ComputerFile File, Runnable Close) {
        Ui.hit(X, Y, W, H, null, null, null);
        Ui.box(X, Y, W, H, OsLayout.Phosphor, OsLayout.PhosphorDark);
        Ui.glyph(OsLayout.GlyphNote, X + 4, Y + 3, OsLayout.PhosphorTitle);
        Ui.clipped(File.Name(), X + 15, Y + 3, W - 34, OsLayout.PhosphorHot);
        Ui.glyphButton(X + W - 14, Y + 2, 11, 11, OsLayout.GlyphClose, true, Close, Component.translatable("gui.scp_unleashed.computer.close"));
        Ui.fill(X + 3, Y + 14, W - 6, 1, OsLayout.PhosphorFaint);
        List<int[]> Lines = Ui.wrap(File.Text(), W - 10);
        int LineY = Y + 18;
        for (int[] Span : Lines) {
            if (LineY > Y + H - 10) {
                Ui.text(OsUi.Ellipsis, X + 5, LineY, OsLayout.PhosphorDim);
                break;
            }
            Ui.text(File.Text().substring(Span[0], Span[1]), X + 5, LineY, OsLayout.Phosphor);
            LineY += 9;
        }
    }
}
