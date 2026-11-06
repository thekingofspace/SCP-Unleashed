package net.rai.scp_unleashed.scp5001.event.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001ActionPayload;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001StatePayload;

import java.util.Locale;

public class Scp5001ConsoleScreen extends Screen {
    public static final ResourceLocation Texture = Scp5001EventRegistry.location("textures/gui/scp5001_event/console_main.png");
    public static final int PanelWidth = 256;
    public static final int PanelHeight = 200;
    public static final int Cyan = 0x8FE3FF;
    public static final int Pale = 0xCED6DE;
    public static final int Dim = 0x6E7884;
    public static final int Violet = 0x9C8CF0;
    public static final int Amber = 0xF8CE78;
    public static final int Red = 0xF06E60;
    public static final int CoverX = 165;
    public static final int CoverY = 64;
    public static final int SwitchX = 181;
    public static final int SwitchY = 72;
    public static final int ResetX = 170;
    public static final int ResetY = 161;
    public static final double Reach = 8.0;
    private static final String Prefix = "gui.scp_unleashed.scp5001_event.console.";
    private final BlockPos Pos;
    private boolean CoverOpen;
    private int Left;
    private int Top;

    public Scp5001ConsoleScreen(BlockPos Pos) {
        super(Component.translatable(Prefix + "title"));
        this.Pos = Pos;
    }

    @Override
    protected void init() {
        Left = (width - PanelWidth) / 2;
        Top = (height - PanelHeight) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.player.distanceToSqr(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5) > Reach * Reach) {
            onClose();
        }
        if (!engaged()) {
            CoverOpen = true;
        }
    }

    private static boolean engaged() {
        return Scp5001ClientState.phase() == Scp5001State.Phase.Dormant && !Scp5001ClientState.cooling();
    }

    private static net.minecraft.network.chat.MutableComponent text(String Key, Object... Arguments) {
        return Component.translatable(Prefix + Key, Arguments);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight, 256, 256);
        Scp5001State.Phase Phase = Scp5001ClientState.phase();
        Scp5001StatePayload State = Scp5001ClientState.latest();
        centred(Graphics, text("title"), Left + 128, Top + 12, Pale);
        Graphics.drawString(font, text("vessel"), Left + 26, Top + 29, Cyan);
        float Lift = Scp5001ClientState.lift();
        row(Graphics, 0, text("field"), field(Phase, Lift));
        row(Graphics, 1, text("altitude"), Component.literal(String.format(Locale.ROOT, "%.1f%%", Lift * 100.0f)).withColor(Pale));
        row(Graphics, 2, text("pillars"), pillars(Phase, State));
        row(Graphics, 3, text("bus"), Component.literal((State == null ? 0 : State.Overdriven()) + "/" + (State == null ? 0 : State.Generators())).withColor(Pale));
        row(Graphics, 4, text("resonance"), resonance(Phase, Lift));
        int Filled = Math.round(Lift * 104);
        for (int Y = 145 - Filled; Y < 145; Y++) {
            if (Y % 3 != 0) {
                Graphics.fill(Left + 15, Top + Y, Left + 21, Top + Y + 1, 0xFF000000 | (Y < 70 ? Violet : Cyan));
            }
        }
        centred(Graphics, text("interlocks"), Left + 189, Top + 29, Violet);
        boolean Engaged = engaged();
        Component Status;
        Component Hint = Component.empty();
        if (Engaged) {
            Status = text("engaged").withColor(Cyan);
        } else if (Phase == Scp5001State.Phase.Dormant) {
            long Left0 = State == null ? 0 : Math.max(0, State.CooldownEnd() - Scp5001ClientState.now()) / 20;
            Status = text("cycling").withColor(Amber);
            Hint = text("recovery", String.format(Locale.ROOT, "%d:%02d", Left0 / 60, Left0 % 60)).withColor(Dim);
        } else {
            Status = text("disengaged").withColor(Red);
            Hint = text("fault").withColor(Dim);
        }
        centred(Graphics, Status, Left + 189, Top + 46, 0xFFFFFF);
        boolean Open = CoverOpen || !Engaged;
        sprite(Graphics, Open ? 48 : 0, 200, 48, 40, CoverX, CoverY);
        if (Open) {
            sprite(Graphics, Engaged ? 96 : 112, 200, 16, 24, SwitchX, SwitchY);
        }
        centred(Graphics, Hint, Left + 189, Top + 112, Dim);
        Component[] Log = log(Phase, State, Lift);
        Graphics.drawString(font, Log[0], Left + 16, Top + 161, Pale);
        Graphics.drawString(font, Log[1], Left + 16, Top + 172, Dim);
        boolean Reset = Phase == Scp5001State.Phase.Purging;
        boolean Hover = Reset && inside(MouseX, MouseY, ResetX, ResetY, 72, 24);
        if (Hover) {
            Graphics.setColor(0.85f, 1.0f, 1.0f, 1.0f);
        }
        sprite(Graphics, 128, Reset ? 226 : 200, 72, 24, ResetX, ResetY);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        Component Label = text("reset");
        int LabelX = Left + ResetX + 36 - font.width(Label) / 2;
        Graphics.drawString(font, Label, LabelX, Top + ResetY + 8, Reset ? 0x281C0C : Dim, !Reset);
    }

    private void row(GuiGraphics Graphics, int Index, Component Label, Component Value) {
        int Y = Top + 44 + Index * 20;
        Graphics.drawString(font, Label, Left + 27, Y, Dim);
        Graphics.drawString(font, Value, Left + 124 - font.width(Value), Y + 9, Pale);
    }

    private void centred(GuiGraphics Graphics, Component Line, int X, int Y, int Colour) {
        Graphics.drawString(font, Line, X - font.width(Line) / 2, Y, Colour);
    }

    private void sprite(GuiGraphics Graphics, int U, int V, int W, int H, int X, int Y) {
        Graphics.blit(Texture, Left + X, Top + Y, U, V, W, H, 256, 256);
    }

    private boolean inside(double MouseX, double MouseY, int X, int Y, int W, int H) {
        return MouseX >= Left + X && MouseX < Left + X + W && MouseY >= Top + Y && MouseY < Top + Y + H;
    }

    private static Component field(Scp5001State.Phase Phase, float Lift) {
        return switch (Phase) {
            case Dormant -> text("field.stable").withColor(Cyan);
            case Unlocked -> text("field.unstable").withColor(Amber);
            case Rising -> Lift < 0.5f ? text("field.degrading").withColor(Amber) : text("field.failing").withColor(Red);
            case Overdrive, Purging -> text("field.pinned").withColor(Violet);
            case Resetting -> text("field.restoring").withColor(Cyan);
            default -> text("field.lost").withColor(Red);
        };
    }

    private static Component pillars(Scp5001State.Phase Phase, Scp5001StatePayload State) {
        int Count = State == null ? 0 : State.Rods();
        if (Count == 0) {
            return text("pillars.none").withColor(Dim);
        }
        if (State.RodsSpent()) {
            return text("pillars.spent").withColor(Amber);
        }
        if (Phase == Scp5001State.Phase.Rising || Phase == Scp5001State.Phase.Overdrive || Phase == Scp5001State.Phase.Purging) {
            return Scp5001ClientState.rodExtension(0.0f) >= 1.0f ? text("pillars.latched", Count, Count).withColor(Pale) : text("pillars.extending").withColor(Amber);
        }
        return text("pillars.armed", Count, Count).withColor(Pale);
    }

    private static Component resonance(Scp5001State.Phase Phase, float Lift) {
        return switch (Phase) {
            case Dormant -> text("resonance.nominal").withColor(Pale);
            case Unlocked -> text("resonance.elevated").withColor(Amber);
            case Rising, Overdrive -> Lift > 0.6f ? text("resonance.critical").withColor(Red) : text("resonance.elevated").withColor(Amber);
            case Purging, Resetting -> text("resonance.falling").withColor(Pale);
            default -> text("resonance.lost").withColor(Red);
        };
    }

    private static Component[] log(Scp5001State.Phase Phase, Scp5001StatePayload State, float Lift) {
        String First;
        String Second;
        switch (Phase) {
            case Dormant -> {
                First = Scp5001ClientState.cooling() ? "log.cycling" : "log.nominal";
                Second = "log.rest";
            }
            case Unlocked -> {
                First = "log.interlock";
                Second = "log.alarm";
            }
            case Rising -> {
                First = "log.deviation";
                Second = State != null && State.Overdriven() > 0 ? "log.bus" : "log.injection";
            }
            case Overdrive -> {
                First = "log.pinned";
                Second = "log.biomass";
            }
            case Purging -> {
                First = "log.cleared";
                Second = "log.authority";
            }
            case Resetting -> {
                First = "log.reset";
                Second = "log.descent";
            }
            default -> {
                First = "log.lost";
                Second = "log.silent";
            }
        }
        return new Component[]{text(First, String.format(Locale.ROOT, "%.1f", Lift * 100.0f)), text(Second)};
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0) {
            boolean Engaged = engaged();
            if (Engaged && !CoverOpen && inside(MouseX, MouseY, CoverX, CoverY, 48, 40)) {
                CoverOpen = true;
                click(Scp5001EventRegistry.Cover.get(), 1.0f);
                PacketDistributor.sendToServer(new Scp5001ActionPayload(Pos, Scp5001ActionPayload.CoverAction));
                return true;
            }
            if (Engaged && CoverOpen && inside(MouseX, MouseY, SwitchX - 4, SwitchY - 2, 24, 28)) {
                click(Scp5001EventRegistry.Switch.get(), 1.0f);
                PacketDistributor.sendToServer(new Scp5001ActionPayload(Pos, Scp5001ActionPayload.ToggleAction));
                return true;
            }
            if (Engaged && CoverOpen && inside(MouseX, MouseY, CoverX, CoverY - 2, 48, 8)) {
                CoverOpen = false;
                click(Scp5001EventRegistry.Cover.get(), 0.8f);
                return true;
            }
            if (Scp5001ClientState.phase() == Scp5001State.Phase.Purging && inside(MouseX, MouseY, ResetX, ResetY, 72, 24)) {
                click(Scp5001EventRegistry.ConsoleBeep.get(), 0.7f);
                PacketDistributor.sendToServer(new Scp5001ActionPayload(Pos, Scp5001ActionPayload.ResetAction));
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    private static void click(net.minecraft.sounds.SoundEvent Sound, float Pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch));
    }
}
