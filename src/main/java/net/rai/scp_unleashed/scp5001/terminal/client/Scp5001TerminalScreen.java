package net.rai.scp_unleashed.scp5001.terminal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp5001.terminal.Scp5001TerminalPayload;
import net.rai.scp_unleashed.scp5001.world.Scp5001WorldRegistry;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Scp5001TerminalScreen extends Screen {
    public static final int PanelWidth = 330;
    public static final int PanelHeight = 214;
    public static final int Frame = 0xFFDCE5EC;
    public static final int FrameLight = 0xFFF4F8FB;
    public static final int FrameDark = 0xFFA7B4C0;
    public static final int Graphite = 0xFF14181E;
    public static final int GraphiteLight = 0xFF1D232B;
    public static final int GridLine = 0xFF1A2028;
    public static final int Cyan = 0xFF9FE6FF;
    public static final int CyanBright = 0xFFE4FAFF;
    public static final int CyanDim = 0xFF4F7C8E;
    public static final int Violet = 0xFF9C8CF0;
    public static final int Amber = 0xFFF0AE45;
    public static final String Prefix = "gui.scp_unleashed.scp5001_terminal.";
    public static final int Pools = 8;
    public static final int LogsPerPool = 6;
    public static final int Maintenance = 12;
    private static final String[] Tabs = {"status", "log", "maintenance"};

    private final Scp5001TerminalPayload Data;
    private int Tab;
    private float Scroll;
    private int Ticks;
    private List<Line> Lines = new ArrayList<>();

    record Line(FormattedCharSequence Text, int Colour) {
    }

    public Scp5001TerminalScreen(Scp5001TerminalPayload Data) {
        super(Component.translatable(Prefix + "title"));
        this.Data = Data;
    }

    private int left() {
        return (width - PanelWidth) / 2;
    }

    private int top() {
        return (height - PanelHeight) / 2;
    }

    private int contentWidth() {
        return PanelWidth - 36;
    }

    private int contentHeight() {
        return PanelHeight - 74;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        Lines = new ArrayList<>();
        List<Entry> Entries = switch (Tab) {
            case 0 -> status();
            case 1 -> log();
            default -> maintenance();
        };
        for (Entry Each : Entries) {
            if (Each.Text() == null) {
                Lines.add(new Line(FormattedCharSequence.EMPTY, 0));
                continue;
            }
            for (FormattedCharSequence Part : font.split(Each.Text(), contentWidth() - 6)) {
                Lines.add(new Line(Part, Each.Colour()));
            }
        }
        Scroll = 0.0f;
    }

    record Entry(FormattedText Text, int Colour) {
    }

    private Component key(String Name, Object... Arguments) {
        return Component.translatable(Prefix + Name, Arguments);
    }

    private String phase() {
        return Data.Phase();
    }

    private boolean phaseIn(String... Names) {
        for (String Name : Names) {
            if (Name.equals(phase())) {
                return true;
            }
        }
        return false;
    }

    private Entry row(String Label, Component Value, int Colour) {
        return new Entry(Component.translatable(Prefix + "row", key("label." + Label), Value), Colour);
    }

    private List<Entry> status() {
        List<Entry> Made = new ArrayList<>();
        String Banner = switch (phase()) {
            case "Unlocked" -> "unlocked";
            case "Rising" -> "rising";
            case "Overdrive" -> "overdrive";
            case "Purging" -> "purging";
            case "Resetting" -> "resetting";
            case "Xk1", "Xk2", "Ended" -> "lost";
            case "Summoning", "Duel" -> "emergency";
            case "Saved" -> "saved";
            default -> null;
        };
        if (Banner != null) {
            Made.add(new Entry(key("banner." + Banner), phaseIn("Saved", "Resetting") ? CyanBright : phaseIn("Summoning", "Duel") ? Violet : Amber));
            Made.add(new Entry(null, 0));
        }
        boolean Lost = phaseIn("Xk1", "Xk2", "Ended", "Summoning", "Duel");
        Made.add(row("life_support", key("value.nominal"), Cyan));
        String Field = switch (phase()) {
            case "Dormant" -> "stable";
            case "Unlocked" -> "unsealed";
            case "Rising" -> "failing";
            case "Overdrive", "Purging" -> "reinforced";
            case "Resetting" -> "restoring";
            case "Saved" -> "restored";
            default -> "absent";
        };
        Made.add(row("field", key("value." + Field), Field.equals("stable") || Field.equals("restored") ? Cyan : Amber));
        boolean Locked = phaseIn("Dormant", "Saved");
        Made.add(row("interlocks", key(Locked ? "value.engaged" : "value.disengaged"), Locked ? Cyan : Amber));
        int Overdriven = Mth.clamp(Data.Overdriven(), 0, 4);
        Made.add(row("generators", Overdriven == 0 ? key("value.bus_nominal") : key("value.bus_overdrive", Overdriven), Overdriven == 0 ? Cyan : Amber));
        Made.add(row("engine", key("value.engine"), Violet));
        Made.add(row("drawn", key("value.none"), CyanDim));
        Component Altitude;
        if (Lost) {
            Altitude = key("value.vessel_missing");
        } else if (Data.Lift() <= 0.001f) {
            Altitude = key("value.seated");
        } else {
            Altitude = key("value.altitude", Math.round(Data.Lift() * 30.0f));
        }
        Made.add(row("altitude", Altitude, Lost || Data.Lift() > 0.001f ? Amber : Cyan));
        boolean Emergency = phaseIn("Summoning", "Duel");
        Made.add(row("emergency", key(Emergency ? "value.engaged" : "value.standby"), Emergency ? Violet : CyanDim));
        Made.add(new Entry(null, 0));
        Made.add(new Entry(key("status.footer", Data.Time() / 24000L + 1, (Data.Time() % 24000L) / 20L), CyanDim));
        return Made;
    }

    private Random random() {
        return new Random(Data.Seed() * 31L + Data.Pool());
    }

    private List<Entry> log() {
        List<Entry> Made = new ArrayList<>();
        Random Pick = random();
        int Pool = Mth.clamp(Data.Pool(), 0, Pools - 1);
        Made.add(new Entry(key("log.header", key("pool." + Pool)), Violet));
        Made.add(new Entry(null, 0));
        List<Integer> Order = new ArrayList<>();
        for (int Index = 0; Index < LogsPerPool; Index++) {
            Order.add(Index);
        }
        java.util.Collections.shuffle(Order, Pick);
        int Shown = 4;
        for (int Index = 0; Index < Shown; Index++) {
            int Stamp = 300 + Pick.nextInt(9000);
            Made.add(new Entry(key("stamp", Stamp / 100, String.format("%02d", Stamp % 100)), CyanDim));
            Made.add(new Entry(key("log." + Pool + "." + Order.get(Index)), Cyan));
            Made.add(new Entry(null, 0));
        }
        if (Pool != 0) {
            Made.add(new Entry(key("stamp", 99, "99"), CyanDim));
            Made.add(new Entry(key("log.0." + Pick.nextInt(LogsPerPool)), Cyan));
        }
        return Made;
    }

    private List<Entry> maintenance() {
        List<Entry> Made = new ArrayList<>();
        Random Pick = random();
        Made.add(new Entry(key("maintenance.header"), Violet));
        Made.add(new Entry(null, 0));
        List<Integer> Order = new ArrayList<>();
        for (int Index = 0; Index < Maintenance; Index++) {
            Order.add(Index);
        }
        java.util.Collections.shuffle(Order, Pick);
        for (int Index = 0; Index < 5; Index++) {
            Made.add(new Entry(key("ticket", 1000 + Pick.nextInt(9000)), Amber));
            Made.add(new Entry(key("maintenance." + Order.get(Index)), Cyan));
            Made.add(new Entry(null, 0));
        }
        return Made;
    }

    @Override
    public void tick() {
        Ticks++;
    }

    private void bevel(GuiGraphics Graphics, int X0, int Y0, int X1, int Y1, int Face, int Light, int Dark) {
        Graphics.fill(X0 + 2, Y0, X1 - 2, Y1, Face);
        Graphics.fill(X0 + 1, Y0 + 1, X1 - 1, Y1 - 1, Face);
        Graphics.fill(X0, Y0 + 2, X1, Y1 - 2, Face);
        Graphics.fill(X0 + 2, Y0, X1 - 2, Y0 + 1, Light);
        Graphics.fill(X0, Y0 + 2, X0 + 1, Y1 - 2, Light);
        Graphics.fill(X0 + 2, Y1 - 1, X1 - 2, Y1, Dark);
        Graphics.fill(X1 - 1, Y0 + 2, X1, Y1 - 2, Dark);
    }

    private void glyph(GuiGraphics Graphics, int X, int Y, int Colour) {
        int[][] Pixels = {{1, 0}, {2, 0}, {3, 0}, {4, 0}, {0, 1}, {5, 1}, {0, 2}, {2, 2}, {3, 2}, {5, 2}, {0, 3}, {2, 3}, {5, 3}, {0, 4}, {5, 4}, {1, 5}, {4, 5}, {2, 6}, {3, 6}};
        for (int[] Each : Pixels) {
            Graphics.fill(X + Each[0] * 2, Y + Each[1] * 2, X + Each[0] * 2 + 2, Y + Each[1] * 2 + 2, Colour);
        }
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        int X = left();
        int Y = top();
        bevel(Graphics, X, Y, X + PanelWidth, Y + PanelHeight, Frame, FrameLight, FrameDark);
        Graphics.fill(X + 6, Y + 6, X + PanelWidth - 6, Y + 7, FrameDark);
        int Sx = X + 10;
        int Sy = Y + 10;
        int Ex = X + PanelWidth - 10;
        int Ey = Y + PanelHeight - 10;
        Graphics.fill(Sx - 1, Sy - 1, Ex + 1, Ey + 1, FrameDark);
        Graphics.fill(Sx, Sy, Ex, Ey, Graphite);
        for (int Line = Sy + 3; Line < Ey; Line += 4) {
            Graphics.fill(Sx, Line, Ex, Line + 1, GridLine);
        }
        Graphics.fill(Sx, Sy, Ex, Sy + 22, GraphiteLight);
        Graphics.fill(Sx, Sy + 22, Ex, Sy + 23, CyanDim);
        glyph(Graphics, Sx + 6, Sy + 4, Violet);
        int Pool = Mth.clamp(Data.Pool(), 0, Pools - 1);
        Graphics.drawString(font, key("title", key("pool." + Pool)), Sx + 22, Sy + 7, CyanBright, false);
        boolean Alert = !Data.Phase().equals("Dormant") && !Data.Phase().equals("Saved");
        int Chip = Alert ? (Ticks / 10 % 2 == 0 ? Amber : 0xFF8A6428) : Cyan;
        Component State = key(Alert ? "chip.alert" : "chip.nominal");
        int ChipWidth = font.width(State) + 10;
        Graphics.fill(Ex - ChipWidth - 6, Sy + 5, Ex - 6, Sy + 17, Chip);
        Graphics.fill(Ex - ChipWidth - 5, Sy + 6, Ex - 7, Sy + 16, Graphite);
        Graphics.drawString(font, State, Ex - ChipWidth - 1, Sy + 7, Chip, false);
        int TabX = Sx + 4;
        for (int Index = 0; Index < Tabs.length; Index++) {
            Component Name = key("tab." + Tabs[Index]);
            int Width = font.width(Name) + 12;
            boolean Chosen = Index == Tab;
            boolean Hover = MouseX >= TabX && MouseX < TabX + Width && MouseY >= Sy + 26 && MouseY < Sy + 38;
            Graphics.fill(TabX, Sy + 26, TabX + Width, Sy + 38, Chosen ? 0xFF2A3A46 : Hover ? 0xFF222B34 : GraphiteLight);
            Graphics.fill(TabX, Sy + 37, TabX + Width, Sy + 38, Chosen ? Cyan : CyanDim);
            Graphics.drawString(font, Name, TabX + 6, Sy + 28, Chosen ? CyanBright : CyanDim, false);
            TabX += Width + 4;
        }
        int Cx = Sx + 6;
        int Cy = Sy + 44;
        int Height = contentHeight() - 4;
        int Total = Lines.size() * 10;
        float Max = Math.max(0, Total - Height);
        Scroll = Mth.clamp(Scroll, 0.0f, Max);
        Graphics.enableScissor(Cx, Cy, Cx + contentWidth(), Cy + Height);
        int Drawn = Cy - Math.round(Scroll);
        for (Line Each : Lines) {
            if (Drawn > Cy - 10 && Drawn < Cy + Height) {
                Graphics.drawString(font, Each.Text(), Cx, Drawn, Each.Colour(), false);
            }
            Drawn += 10;
        }
        Graphics.disableScissor();
        if (Max > 0) {
            int BarHeight = Math.max(12, Math.round(Height * Height / (float) Total));
            int BarY = Cy + Math.round((Height - BarHeight) * (Scroll / Max));
            Graphics.fill(Ex - 6, Cy, Ex - 4, Cy + Height, GraphiteLight);
            Graphics.fill(Ex - 6, BarY, Ex - 4, BarY + BarHeight, Cyan);
        }
        if (Ticks / 10 % 2 == 0) {
            Graphics.fill(Cx, Ey - 12, Cx + 5, Ey - 4, Cyan);
        }
        Graphics.drawString(font, key("footer"), Cx + 10, Ey - 12, CyanDim, false);
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        Graphics.fillGradient(0, 0, width, height, 0xC0050608, 0xD0080A10);
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        int Sx = left() + 10;
        int Sy = top() + 10;
        int TabX = Sx + 4;
        for (int Index = 0; Index < Tabs.length; Index++) {
            int Width = font.width(key("tab." + Tabs[Index])) + 12;
            if (MouseX >= TabX && MouseX < TabX + Width && MouseY >= Sy + 26 && MouseY < Sy + 38) {
                select(Index);
                return true;
            }
            TabX += Width + 4;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    private void select(int Index) {
        if (Index != Tab) {
            Tab = Index;
            rebuild();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Scp5001WorldRegistry.TerminalPage.get(), 1.0f, 0.6f));
        }
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double Horizontal, double Vertical) {
        Scroll -= (float) Vertical * 12.0f;
        return true;
    }

    @Override
    public boolean keyPressed(int Key, int Code, int Modifiers) {
        if (Key == GLFW.GLFW_KEY_DOWN || Key == GLFW.GLFW_KEY_PAGE_DOWN) {
            Scroll += Key == GLFW.GLFW_KEY_DOWN ? 10.0f : contentHeight() - 20;
            return true;
        }
        if (Key == GLFW.GLFW_KEY_UP || Key == GLFW.GLFW_KEY_PAGE_UP) {
            Scroll -= Key == GLFW.GLFW_KEY_UP ? 10.0f : contentHeight() - 20;
            return true;
        }
        if (Key == GLFW.GLFW_KEY_TAB || Key == GLFW.GLFW_KEY_RIGHT) {
            select((Tab + 1) % Tabs.length);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_LEFT) {
            select((Tab + Tabs.length - 1) % Tabs.length);
            return true;
        }
        if (Key >= GLFW.GLFW_KEY_1 && Key <= GLFW.GLFW_KEY_3) {
            select(Key - GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(Key, Code, Modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
