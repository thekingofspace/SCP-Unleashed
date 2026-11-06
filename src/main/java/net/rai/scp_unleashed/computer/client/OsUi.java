package net.rai.scp_unleashed.computer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public final class OsUi {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/computer/os.png");
    public static final String Ellipsis = "...";
    public static final int TooltipWidth = 180;
    public static final int ButtonHover = 0xFF12391F;
    public static final int RowHover = 0xFF0F2E19;

    private static final RandomSource Noise = RandomSource.create();

    private final List<Hit> Hits = new ArrayList<>();
    private final List<Wheel> Wheels = new ArrayList<>();
    private List<Hit> Shown = List.of();
    private List<Wheel> ShownWheels = List.of();
    private GuiGraphics Graphics;
    private Font Text;
    private int MouseX;
    private int MouseY;
    private int Ticks;
    @Nullable
    private Component Tip;
    @Nullable
    private OsField Focus;
    private boolean Pressing;

    public void begin(GuiGraphics NewGraphics, Font NewText, int NewMouseX, int NewMouseY, int NewTicks) {
        Graphics = NewGraphics;
        Text = NewText;
        MouseX = NewMouseX;
        MouseY = NewMouseY;
        Ticks = NewTicks;
        Hits.clear();
        Wheels.clear();
        Tip = null;
        Pressing = GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    public void end() {
        Shown = List.copyOf(Hits);
        ShownWheels = List.copyOf(Wheels);
        if (Tip != null) {
            List<FormattedCharSequence> Lines = Text.split(Tip, TooltipWidth);
            Graphics.renderTooltip(Text, Lines, MouseX, MouseY);
        }
    }

    public GuiGraphics graphics() {
        return Graphics;
    }

    public Font font() {
        return Text;
    }

    public int ticks() {
        return Ticks;
    }

    public boolean hovered(int X, int Y, int W, int H) {
        return MouseX >= X && MouseX < X + W && MouseY >= Y && MouseY < Y + H;
    }

    public void hit(int X, int Y, int W, int H, @Nullable Runnable Left, @Nullable Runnable Right, @Nullable Component Hint) {
        Hits.add(new Hit(X, Y, W, H, Left, Right));
        if (Hint != null && hovered(X, Y, W, H)) {
            Tip = Hint;
        }
    }

    public void tip(int X, int Y, int W, int H, Component Hint) {
        if (hovered(X, Y, W, H)) {
            Tip = Hint;
        }
    }

    public void wheel(int X, int Y, int W, int H, IntConsumer OnScroll) {
        Wheels.add(new Wheel(X, Y, W, H, OnScroll));
    }

    public boolean click(double X, double Y, int Button) {
        for (int Index = Shown.size() - 1; Index >= 0; Index--) {
            Hit Target = Shown.get(Index);
            if (X >= Target.X() && X < Target.X() + Target.W() && Y >= Target.Y() && Y < Target.Y() + Target.H()) {
                Runnable Action = Button == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? Target.Right() : Target.Left();
                if (Action == null) {
                    return Button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
                }
                Action.run();
                return true;
            }
        }
        return false;
    }

    public boolean scroll(double X, double Y, double Amount) {
        for (int Index = ShownWheels.size() - 1; Index >= 0; Index--) {
            Wheel Target = ShownWheels.get(Index);
            if (X >= Target.X() && X < Target.X() + Target.W() && Y >= Target.Y() && Y < Target.Y() + Target.H()) {
                Target.OnScroll().accept(Amount > 0 ? -1 : 1);
                return true;
            }
        }
        return false;
    }

    @Nullable
    public OsField focus() {
        return Focus;
    }

    public void focus(@Nullable OsField Field) {
        Focus = Field;
    }

    public void fill(int X, int Y, int W, int H, int Colour) {
        if (W > 0 && H > 0) {
            Graphics.fill(X, Y, X + W, Y + H, Colour);
        }
    }

    public void frame(int X, int Y, int W, int H, int Colour) {
        fill(X + 1, Y, W - 2, 1, Colour);
        fill(X + 1, Y + H - 1, W - 2, 1, Colour);
        fill(X, Y + 1, 1, H - 2, Colour);
        fill(X + W - 1, Y + 1, 1, H - 2, Colour);
    }

    public void box(int X, int Y, int W, int H, int Border, int Inside) {
        fill(X + 1, Y + 1, W - 2, H - 2, Inside);
        frame(X, Y, W, H, Border);
    }

    public void text(String Line, int X, int Y, int Colour) {
        Graphics.drawString(Text, Line, X, Y, Colour, false);
    }

    public void text(Component Line, int X, int Y, int Colour) {
        Graphics.drawString(Text, Line, X, Y, Colour, false);
    }

    public void centered(String Line, int CentreX, int Y, int Colour) {
        text(Line, CentreX - Text.width(Line) / 2, Y, Colour);
    }

    public void clipped(String Line, int X, int Y, int Width, int Colour) {
        text(fit(Line, Width), X, Y, Colour);
    }

    public String fit(String Line, int Width) {
        if (Text.width(Line) <= Width) {
            return Line;
        }
        return Text.plainSubstrByWidth(Line, Math.max(0, Width - Text.width(Ellipsis))).stripTrailing() + Ellipsis;
    }

    public void right(String Line, int RightX, int Y, int Colour) {
        text(Line, RightX - Text.width(Line), Y, Colour);
    }

    public void glyph(int Index, int X, int Y, int Colour) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((Colour >> 16) & 0xFF) / 255.0f, ((Colour >> 8) & 0xFF) / 255.0f, (Colour & 0xFF) / 255.0f, ((Colour >>> 24) & 0xFF) / 255.0f);
        Graphics.blit(Texture, X, Y, (float) (OsLayout.GlyphU + Index * OsLayout.GlyphSize), (float) OsLayout.GlyphV, OsLayout.GlyphSize, OsLayout.GlyphSize,
                OsLayout.TextureSize, OsLayout.TextureSize);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void sprite(int X, int Y, int U, int V, int W, int H) {
        RenderSystem.enableBlend();
        Graphics.blit(Texture, X, Y, (float) U, (float) V, W, H, OsLayout.TextureSize, OsLayout.TextureSize);
    }

    public void lamp(int X, int Y, boolean On, int Tone) {
        int Bright = Tone == 1 ? OsLayout.Amber : Tone == 2 ? OsLayout.Alert : 0xFF7CFF9A;
        int Dark = Tone == 1 ? OsLayout.AmberDim : Tone == 2 ? OsLayout.AlertDim : 0xFF1F4A2B;
        box(X, Y, 5, 5, On ? Dark : 0xFF101A13, On ? Bright : 0xFF1A2A1E);
        if (On) {
            fill(X + 1, Y + 1, 1, 1, OsLayout.PhosphorHot);
        }
    }

    public boolean button(int X, int Y, int W, int H, Component Label, boolean Enabled, @Nullable Runnable OnClick, @Nullable Component Hint) {
        boolean Over = Enabled && hovered(X, Y, W, H);
        boolean Down = Over && Pressing;
        int Border = !Enabled ? OsLayout.PhosphorFaint : Over ? OsLayout.Phosphor : OsLayout.PhosphorDim;
        int Inside = Down ? OsLayout.Phosphor : Over ? ButtonHover : OsLayout.PhosphorDark;
        int Ink = !Enabled ? OsLayout.PhosphorFaint : Down ? OsLayout.PhosphorDark : Over ? OsLayout.PhosphorHot : OsLayout.Phosphor;
        box(X, Y, W, H, Border, Inside);
        String Shown = fit(Label.getString(), W - 4);
        text(Shown, X + (W - Text.width(Shown)) / 2, Y + (H - 8) / 2 + 1, Ink);
        hit(X, Y, W, H, Enabled ? clicked(OnClick) : null, null, Hint);
        return Over;
    }

    public void cycler(int X, int Y, int W, int H, String Label, boolean Enabled, Runnable Next, Runnable Previous, @Nullable Component Hint) {
        boolean Over = Enabled && hovered(X, Y, W, H);
        int Border = !Enabled ? OsLayout.PhosphorFaint : Over ? OsLayout.Phosphor : OsLayout.PhosphorDim;
        int Ink = !Enabled ? OsLayout.PhosphorFaint : Over ? OsLayout.PhosphorHot : OsLayout.Phosphor;
        box(X, Y, W, H, Border, Over ? ButtonHover : OsLayout.PhosphorDark);
        glyph(OsLayout.GlyphLeft, X + 2, Y + (H - OsLayout.GlyphSize) / 2, Ink);
        glyph(OsLayout.GlyphRight, X + W - 10, Y + (H - OsLayout.GlyphSize) / 2, Ink);
        String Shown = fit(Label, W - 24);
        text(Shown, X + (W - Text.width(Shown)) / 2, Y + (H - 8) / 2 + 1, Ink);
        hit(X, Y, W, H, Enabled ? clicked(Next) : null, Enabled ? clicked(Previous) : null, Hint);
    }

    public boolean toggle(int X, int Y, int W, int H, Component Label, boolean On, Runnable OnClick, @Nullable Component Hint) {
        boolean Over = hovered(X, Y, W, H);
        int Border = On || Over ? OsLayout.Phosphor : OsLayout.PhosphorDim;
        int Inside = On ? OsLayout.Phosphor : Over ? ButtonHover : OsLayout.PhosphorDark;
        int Ink = On ? OsLayout.PhosphorDark : Over ? OsLayout.PhosphorHot : OsLayout.Phosphor;
        box(X, Y, W, H, Border, Inside);
        String Shown = fit(Label.getString(), W - 4);
        text(Shown, X + (W - Text.width(Shown)) / 2, Y + (H - 8) / 2 + 1, Ink);
        hit(X, Y, W, H, clicked(OnClick), null, Hint);
        return Over;
    }

    public void glyphButton(int X, int Y, int W, int H, int Glyph, boolean Enabled, @Nullable Runnable OnClick, @Nullable Component Hint) {
        boolean Over = Enabled && hovered(X, Y, W, H);
        boolean Down = Over && Pressing;
        int Border = !Enabled ? OsLayout.PhosphorFaint : Over ? OsLayout.Phosphor : OsLayout.PhosphorDim;
        int Inside = Down ? OsLayout.Phosphor : Over ? ButtonHover : OsLayout.PhosphorDark;
        int Ink = !Enabled ? OsLayout.PhosphorFaint : Down ? OsLayout.PhosphorDark : Over ? OsLayout.PhosphorHot : OsLayout.Phosphor;
        box(X, Y, W, H, Border, Inside);
        glyph(Glyph, X + (W - OsLayout.GlyphSize) / 2, Y + (H - OsLayout.GlyphSize) / 2, Ink);
        hit(X, Y, W, H, Enabled ? clicked(OnClick) : null, null, Hint);
    }

    public void row(int X, int Y, int W, int H, boolean Selected, @Nullable Runnable Left, @Nullable Runnable Right, @Nullable Component Hint) {
        if (Selected) {
            fill(X, Y, W, H, OsLayout.Phosphor);
        } else if (hovered(X, Y, W, H)) {
            fill(X, Y, W, H, RowHover);
        }
        hit(X, Y, W, H, Left == null ? null : clicked(Left), Right == null ? null : clicked(Right), Hint);
    }

    public int rowInk(boolean Selected) {
        return Selected ? OsLayout.PhosphorDark : OsLayout.Phosphor;
    }

    public int rowDim(boolean Selected) {
        return Selected ? 0xFF1E5A33 : OsLayout.PhosphorDim;
    }

    public void scrollbar(int X, int Y, int H, int First, int Total, int Visible) {
        fill(X, Y, OsLayout.Scrollbar, H, 0xFF0D2215);
        if (Total <= Visible || Total <= 0) {
            fill(X + 1, Y, 2, H, OsLayout.PhosphorFaint);
            return;
        }
        int Thumb = Math.max(8, H * Visible / Total);
        int Travel = H - Thumb;
        int Top = Y + Math.round((float) Travel * First / Math.max(1, Total - Visible));
        fill(X, Top, OsLayout.Scrollbar, Thumb, OsLayout.PhosphorDim);
        fill(X + 1, Top + 1, 2, Thumb - 2, OsLayout.Phosphor);
    }

    public void field(OsField Field, int X, int Y, int W, @Nullable Component Hint, @Nullable Component Tooltip) {
        boolean Focused = Focus == Field;
        boolean Over = hovered(X, Y, W, OsLayout.FieldHeight);
        box(X, Y, W, OsLayout.FieldHeight, Focused ? OsLayout.Phosphor : Over ? OsLayout.PhosphorTitle : OsLayout.PhosphorDim, 0xFF071A0D);
        String Shown = Field.visible(Text, W - 6);
        if (Shown.isEmpty() && !Focused && Hint != null) {
            clipped(Hint.getString(), X + 3, Y + 2, W - 6, OsLayout.PhosphorFaint);
        } else {
            text(Shown, X + 3, Y + 2, OsLayout.Phosphor);
        }
        if (Focused && (Ticks / 6) % 2 == 0) {
            fill(X + 3 + Field.caretX(Text), Y + 2, 1, 9, OsLayout.PhosphorHot);
        }
        hit(X, Y, W, OsLayout.FieldHeight, () -> Focus = Field, null, Tooltip);
    }

    public void area(OsField Field, int X, int Y, int W, int H, @Nullable Component Hint) {
        boolean Focused = Focus == Field;
        boolean Over = hovered(X, Y, W, H);
        box(X, Y, W, H, Focused ? OsLayout.Phosphor : Over ? OsLayout.PhosphorTitle : OsLayout.PhosphorDim, 0xFF071A0D);
        String Value = Field.getValue();
        List<int[]> Lines = wrap(Value, W - 8);
        int Rows = Math.max(1, (H - 4) / 9);
        int CaretLine = 0;
        int Cursor = Field.getCursor();
        for (int Index = 0; Index < Lines.size(); Index++) {
            if (Cursor >= Lines.get(Index)[0]) {
                CaretLine = Index;
            }
        }
        int First = Math.max(0, CaretLine - Rows + 1);
        if (Value.isEmpty() && !Focused && Hint != null) {
            clipped(Hint.getString(), X + 4, Y + 3, W - 8, OsLayout.PhosphorFaint);
        }
        for (int Row = 0; Row < Rows && First + Row < Lines.size(); Row++) {
            int[] Span = Lines.get(First + Row);
            text(Value.substring(Span[0], Span[1]), X + 4, Y + 3 + Row * 9, OsLayout.Phosphor);
        }
        if (Focused && (Ticks / 6) % 2 == 0 && CaretLine >= First && CaretLine < First + Rows) {
            int[] Span = Lines.get(CaretLine);
            int Column = Math.min(Cursor, Span[1]) - Span[0];
            int CaretX = X + 4 + Text.width(Value.substring(Span[0], Span[0] + Math.max(0, Column)));
            fill(CaretX, Y + 3 + (CaretLine - First) * 9, 1, 9, OsLayout.PhosphorHot);
        }
        hit(X, Y, W, H, () -> Focus = Field, null, null);
    }

    public List<int[]> wrap(String Value, int Width) {
        List<int[]> Lines = new ArrayList<>();
        int Start = 0;
        int Length = Value.length();
        while (Start <= Length) {
            int Newline = Value.indexOf('\n', Start);
            int Stop = Newline < 0 ? Length : Newline;
            int LineStart = Start;
            while (true) {
                String Rest = Value.substring(LineStart, Stop);
                if (Text.width(Rest) <= Width) {
                    Lines.add(new int[]{LineStart, Stop});
                    break;
                }
                int Fits = Text.plainSubstrByWidth(Rest, Width).length();
                int Break = Rest.lastIndexOf(' ', Math.max(0, Fits));
                int Cut = Break > 0 ? Break : Math.max(1, Fits);
                Lines.add(new int[]{LineStart, LineStart + Cut});
                LineStart += Cut;
                while (LineStart < Stop && Value.charAt(LineStart) == ' ') {
                    LineStart++;
                }
                if (LineStart >= Stop) {
                    break;
                }
            }
            if (Newline < 0) {
                break;
            }
            Start = Newline + 1;
        }
        return Lines;
    }

    public void stepper(int X, int Y, int W, String Value, Runnable Lower, Runnable Raise, @Nullable Component Hint, IntConsumer OnWheel) {
        glyphButton(X, Y, 11, OsLayout.ButtonHeight, OsLayout.GlyphMinus, true, Lower, null);
        glyphButton(X + W - 11, Y, 11, OsLayout.ButtonHeight, OsLayout.GlyphPlus, true, Raise, null);
        box(X + 12, Y, W - 24, OsLayout.ButtonHeight, OsLayout.PhosphorDim, 0xFF071A0D);
        centered(fit(Value, W - 28), X + W / 2, Y + 3, OsLayout.PhosphorHot);
        if (Hint != null) {
            tip(X + 12, Y, W - 24, OsLayout.ButtonHeight, Hint);
        }
        wheel(X, Y, W, OsLayout.ButtonHeight, OnWheel);
    }

    public void heading(String Label, int X, int Y, int W) {
        text(Label, X, Y, OsLayout.PhosphorTitle);
        int After = X + Text.width(Label) + 3;
        fill(After, Y + 4, X + W - After, 1, OsLayout.PhosphorFaint);
    }

    public void pattern(int X, int Y, int W, int H, int Phase) {
        float Stretch = Math.max(1.0f, (float) W / Math.max(1, H));
        for (int Py = 0; Py < H; Py += 2) {
            for (int Px = 0; Px < W; Px += 2) {
                float Dx = Math.abs(Px + 1 - W / 2.0f) / Stretch;
                float Dy = Math.abs(Py + 1 - H / 2.0f);
                int Ring = (int) (Math.max(Dx, Dy) / 2.0f);
                int Shade = ((Ring + Phase) & 1) == 0 ? 0xFFDDF8E3 : 0xFF0A1A0F;
                if (Ring == 0) {
                    Shade = (Phase & 1) == 0 ? 0xFF0A1A0F : 0xFFF4FFF6;
                } else if (((Px / 2) * 7 + (Py / 2) * 13 + Phase * 5) % 23 == 0) {
                    Shade = 0xFF9CF5B0;
                }
                fill(X + Px, Y + Py, Math.min(2, W - Px), Math.min(2, H - Py), Shade);
            }
        }
    }

    public void scanlines(int X, int Y, int W, int H) {
        for (int Line = Y + 1; Line < Y + H; Line += 2) {
            fill(X, Line, W, 1, OsLayout.Scanline);
        }
    }

    private Runnable clicked(@Nullable Runnable Action) {
        return () -> {
            Minecraft Game = Minecraft.getInstance();
            Game.getSoundManager().play(SimpleSoundInstance.forUI(ComputerRegistry.Key.get(), 1.0f + Noise.nextFloat() * 0.15f, 0.35f));
            if (Action != null) {
                Action.run();
            }
        };
    }

    private record Hit(int X, int Y, int W, int H, @Nullable Runnable Left, @Nullable Runnable Right) {
    }

    private record Wheel(int X, int Y, int W, int H, IntConsumer OnScroll) {
    }
}
