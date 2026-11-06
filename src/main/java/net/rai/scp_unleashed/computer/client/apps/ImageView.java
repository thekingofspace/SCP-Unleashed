package net.rai.scp_unleashed.computer.client.apps;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.computer.client.OsLayout;
import net.rai.scp_unleashed.computer.client.OsUi;
import net.rai.scp_unleashed.computer.data.ComputerFile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ImageView {
    public static final String Prefix = "gui.scp_unleashed.computer.image.";
    public static final int Header = 17;
    public static final int Dead = 0xFF0B110D;
    public static final int DeadBand = 0xFF141C17;
    private static final Map<ResourceLocation, Painter> Painters = new ConcurrentHashMap<>();

    private ImageView() {
    }

    public static void register(ResourceLocation Picture, Painter Paint) {
        Painters.put(Picture, Paint);
    }

    public static void draw(OsUi Ui, int X, int Y, int W, int H, ComputerFile File, Runnable Close) {
        Ui.hit(X, Y, W, H, null, null, null);
        Ui.box(X, Y, W, H, OsLayout.Phosphor, OsLayout.GlassDeep);
        Ui.glyph(OsLayout.GlyphNote, X + 4, Y + 3, OsLayout.PhosphorTitle);
        Ui.clipped(File.Name(), X + 15, Y + 3, W - 34, OsLayout.PhosphorHot);
        Ui.glyphButton(X + W - 14, Y + 2, 11, 11, OsLayout.GlyphClose, true, Close, Component.translatable("gui.scp_unleashed.computer.close"));
        Ui.fill(X + 3, Y + 14, W - 6, 1, OsLayout.PhosphorFaint);
        int AreaX = X + 3;
        int AreaY = Y + Header;
        int AreaW = W - 6;
        int AreaH = H - Header - 3;
        Painter Paint = Painters.get(File.Document());
        if (Paint == null) {
            damaged(Ui, AreaX, AreaY, AreaW, AreaH);
            return;
        }
        Paint.paint(Ui.graphics(), AreaX, AreaY, AreaW, AreaH, Ui.ticks());
    }

    private static void damaged(OsUi Ui, int X, int Y, int W, int H) {
        Ui.fill(X, Y, W, H, Dead);
        for (int Band = Y + 3; Band < Y + H - 2; Band += 6) {
            Ui.fill(X + 2, Band, W - 4, 2, DeadBand);
        }
        int Middle = Y + H / 2;
        Ui.fill(X + W / 2 - 52, Middle - 10, 104, 22, OsLayout.GlassDeep);
        Ui.centered(Component.translatable(Prefix + "damaged").getString(), X + W / 2, Middle - 7, OsLayout.Amber);
        Ui.centered(Component.translatable(Prefix + "empty").getString(), X + W / 2, Middle + 3, OsLayout.PhosphorDim);
    }

    @FunctionalInterface
    public interface Painter {
        void paint(GuiGraphics Graphics, int X, int Y, int W, int H, int Ticks);
    }
}
