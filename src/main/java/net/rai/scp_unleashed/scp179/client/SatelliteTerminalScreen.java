package net.rai.scp_unleashed.scp179.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp179.SatelliteTerminalBlockEntity;
import net.rai.scp_unleashed.scp179.Scp179Omen;
import net.rai.scp_unleashed.scp179.Scp179Registry;
import net.rai.scp_unleashed.scp179.SkyTarget;
import net.rai.scp_unleashed.scp179.SkyTargets;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SatelliteTerminalScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/satellite_terminal/terminal.png");
    public static final int PanelWidth = 248;
    public static final int PanelHeight = 176;
    public static final int TitleX = 8;
    public static final int TitleY = 6;
    public static final int LampX = 234;
    public static final int LampY = 6;
    public static final int LampSize = 6;
    public static final int LampV = 204;
    public static final int LampOffU = 0;
    public static final int LampGreenU = 6;
    public static final int LampAmberU = 12;
    public static final int LampRedU = 18;
    public static final int UplinkLabelY = 19;
    public static final int UplinkX = 8;
    public static final int UplinkY = 29;
    public static final int UplinkWidth = 70;
    public static final int UplinkHeight = 32;
    public static final int UplinkLines = 3;
    public static final int LineHeight = 9;
    public static final int ListLabelY = 66;
    public static final int ListX = 8;
    public static final int ListY = 76;
    public static final int ListWidth = 70;
    public static final int ListHeight = 62;
    public static final int EntryWidth = 68;
    public static final int EntryHeight = 12;
    public static final int VisibleEntries = 5;
    public static final int EntryV = 180;
    public static final int EntryHoverU = 68;
    public static final int EntrySelectedU = 136;
    public static final int AlertV = 192;
    public static final int AlertFriendU = 68;
    public static final int ScrollBarU = 204;
    public static final int ViewX = 84;
    public static final int ViewY = 18;
    public static final int ViewWidth = 156;
    public static final int ViewHeight = 120;
    public static final int ReadoutX = 8;
    public static final int ReadoutY = 144;
    public static final int ReadoutWidth = 232;
    public static final int ReadoutHeight = 24;
    public static final int TextInset = 4;
    public static final int BlinkCycle = 20;
    public static final int BlinkShown = 14;
    public static final int LabelColour = 0xFF404040;
    public static final int LcdColour = 0xFF6FE08C;
    public static final int LcdDim = 0xFF3E9A58;
    public static final int AmberColour = 0xFFF2B640;
    public static final int RedColour = 0xFFE8473A;
    public static final int EntryText = 0xFFE6E6E6;
    public static final int EntryTextSelected = 0xFFFFFFFF;
    public static final int ViewTextColour = 0xFFB8B0D8;
    public static final int ViewTextBack = 0xC0080610;
    public static final double MaxDistanceSqr = 64.0;
    public static final float TuneVolume = 0.5f;

    private final BlockPos Pos;
    @Nullable
    private ResourceLocation Selected;
    private boolean Picked;
    private int Scroll;
    private int Left;
    private int Top;
    private int Age;

    public SatelliteTerminalScreen(BlockPos Pos, Component Title) {
        super(Title);
        this.Pos = Pos;
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - PanelWidth) / 2;
        Top = (height - PanelHeight) / 2;
    }

    @Nullable
    private SatelliteTerminalBlockEntity terminal() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(Pos) instanceof SatelliteTerminalBlockEntity Terminal ? Terminal : null;
    }

    @Override
    public void tick() {
        super.tick();
        Age++;
        SatelliteTerminalBlockEntity Terminal = terminal();
        if (Terminal == null || minecraft == null || minecraft.player == null || minecraft.player.distanceToSqr(Vec3.atCenterOf(Pos)) > MaxDistanceSqr) {
            onClose();
            return;
        }
        List<ResourceLocation> Locations = Terminal.getLocations();
        if (Selected != null && !Locations.contains(Selected)) {
            Selected = null;
        }
        if (!Picked && Selected == null && !Locations.isEmpty()) {
            Selected = Terminal.getOmen().isActive() && Locations.contains(Scp179Registry.TargetId) ? Scp179Registry.TargetId : Locations.getFirst();
        }
        Scroll = Math.max(0, Math.min(Scroll, Locations.size() - VisibleEntries));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float ticks(float PartialTick) {
        return Age + PartialTick;
    }

    private boolean blink() {
        return Math.floorMod(Age, BlinkCycle) < BlinkShown;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        SatelliteTerminalBlockEntity Terminal = terminal();
        if (Terminal == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight);
        Graphics.drawString(font, title, Left + TitleX, Top + TitleY, LabelColour, false);
        Graphics.blit(Texture, Left + LampX, Top + LampY, lampU(Terminal), LampV, LampSize, LampSize);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.satellite_terminal.uplink"), Left + UplinkX, Top + UplinkLabelY, LabelColour, false);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.satellite_terminal.locations"), Left + ListX, Top + ListLabelY, LabelColour, false);
        uplink(Graphics, Terminal);
        int Hovered = entryAt(Terminal, MouseX, MouseY);
        entries(Graphics, Terminal, Hovered);
        view(Graphics, Terminal, ticks(PartialTick));
        readout(Graphics, Terminal);
        if (Hovered >= 0) {
            tooltip(Graphics, Terminal.getLocations().get(Hovered), MouseX, MouseY);
        }
    }

    private int lampU(SatelliteTerminalBlockEntity Terminal) {
        if (!Terminal.isRunning()) {
            return LampOffU;
        }
        if (!Terminal.hasUplink()) {
            return LampAmberU;
        }
        if (Terminal.getOmen().isActive()) {
            return blink() ? LampRedU : LampOffU;
        }
        return LampGreenU;
    }

    private void uplink(GuiGraphics Graphics, SatelliteTerminalBlockEntity Terminal) {
        int X = Left + UplinkX + TextInset;
        int Y = Top + UplinkY + TextInset;
        if (!Terminal.isRunning()) {
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.satellite_terminal.uplink.offline"), X, Y, LcdDim, false);
            return;
        }
        List<String> Names = Terminal.getSatellites();
        if (Names.isEmpty()) {
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.satellite_terminal.uplink.none"), X, Y, AmberColour, false);
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.satellite_terminal.uplink.none.detail"), X, Y + LineHeight, LcdDim, false);
            return;
        }
        int Shown = Names.size() > UplinkLines ? UplinkLines - 1 : Names.size();
        for (int Line = 0; Line < Shown; Line++) {
            Graphics.drawString(font, clip(Names.get(Line), UplinkWidth - TextInset * 2), X, Y + Line * LineHeight, LcdColour, false);
        }
        if (Terminal.getSatelliteCount() > Shown) {
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.satellite_terminal.uplink.more", Terminal.getSatelliteCount() - Shown), X,
                    Y + Shown * LineHeight, LcdDim, false);
        }
    }

    private String clip(String Text, int Width) {
        if (font.width(Text) <= Width) {
            return Text;
        }
        return font.plainSubstrByWidth(Text, Width - font.width("..")) + "..";
    }

    private void entries(GuiGraphics Graphics, SatelliteTerminalBlockEntity Terminal, int Hovered) {
        List<ResourceLocation> Locations = Terminal.getLocations();
        int X = Left + ListX + 1;
        for (int Row = 0; Row < VisibleEntries; Row++) {
            int Index = Scroll + Row;
            if (Index >= Locations.size()) {
                break;
            }
            ResourceLocation Target = Locations.get(Index);
            int Y = Top + ListY + 1 + Row * EntryHeight;
            boolean Chosen = Target.equals(Selected);
            int U = Chosen ? EntrySelectedU : Index == Hovered ? EntryHoverU : 0;
            int V = EntryV;
            if (Target.equals(Scp179Registry.TargetId) && Terminal.getOmen().isActive() && Terminal.hasUplink() && blink()) {
                U = Terminal.getOmen() == Scp179Omen.Beware ? 0 : AlertFriendU;
                V = AlertV;
            }
            Graphics.blit(Texture, X, Y, U, V, EntryWidth, EntryHeight);
            Component Label = SkyTargets.label(Target);
            Graphics.drawString(font, Label, X + TextInset, Y + 2, Chosen ? EntryTextSelected : EntryText, true);
        }
        if (Locations.size() > VisibleEntries) {
            int Track = ListHeight - 4;
            int Bar = Math.max(6, Track * VisibleEntries / Locations.size());
            int Offset = (Track - Bar) * Scroll / Math.max(1, Locations.size() - VisibleEntries);
            Graphics.fill(Left + ListX + ListWidth - 3, Top + ListY + 2 + Offset, Left + ListX + ListWidth - 1, Top + ListY + 2 + Offset + Bar, 0xFF8B8B8B);
        }
    }

    private int entryAt(SatelliteTerminalBlockEntity Terminal, double MouseX, double MouseY) {
        int X = Left + ListX + 1;
        int Y = Top + ListY + 1;
        if (MouseX < X || MouseX >= X + EntryWidth || MouseY < Y || MouseY >= Y + VisibleEntries * EntryHeight) {
            return -1;
        }
        int Index = Scroll + (int) ((MouseY - Y) / EntryHeight);
        return Index < Terminal.getLocations().size() ? Index : -1;
    }

    private void view(GuiGraphics Graphics, SatelliteTerminalBlockEntity Terminal, float Ticks) {
        int X = Left + ViewX;
        int Y = Top + ViewY;
        Graphics.enableScissor(X, Y, X + ViewWidth, Y + ViewHeight);
        if (!Terminal.isRunning()) {
            Graphics.fill(X, Y, X + ViewWidth, Y + ViewHeight, 0xFF06070A);
            caption(Graphics, Component.translatable("gui.scp_unleashed.satellite_terminal.view.offline"), X, Y, 0xFF5A5E66);
        } else if (!Terminal.hasUplink()) {
            SpaceScene.noise(Graphics, X, Y, ViewWidth, ViewHeight, Ticks);
            caption(Graphics, Component.translatable("gui.scp_unleashed.satellite_terminal.view.no_uplink"), X, Y, AmberColour);
        } else if (Selected == null) {
            SpaceScene.empty(Graphics, X, Y, ViewWidth, ViewHeight, Ticks);
            caption(Graphics, Component.translatable(Terminal.getLocations().isEmpty() ? "gui.scp_unleashed.satellite_terminal.view.empty"
                    : "gui.scp_unleashed.satellite_terminal.view.select"), X, Y, ViewTextColour);
        } else {
            SkyView Shown = SkyViews.get(Selected);
            if (Shown == null) {
                SpaceScene.noise(Graphics, X, Y, ViewWidth, ViewHeight, Ticks);
                caption(Graphics, Component.translatable("gui.scp_unleashed.satellite_terminal.view.no_signal"), X, Y, ViewTextColour);
            } else {
                Shown.render(Graphics, X, Y, ViewWidth, ViewHeight, Terminal, Ticks);
            }
        }
        Graphics.disableScissor();
    }

    private void caption(GuiGraphics Graphics, Component Text, int X, int Y, int Colour) {
        int Wide = font.width(Text);
        int Cx = X + (ViewWidth - Wide) / 2;
        int Cy = Y + (ViewHeight - font.lineHeight) / 2;
        Graphics.fill(Cx - 4, Cy - 3, Cx + Wide + 3, Cy + font.lineHeight + 2, ViewTextBack);
        Graphics.drawString(font, Text, Cx, Cy, Colour, false);
    }

    private void readout(GuiGraphics Graphics, SatelliteTerminalBlockEntity Terminal) {
        int X = Left + ReadoutX + TextInset;
        int Y = Top + ReadoutY + TextInset;
        Component First;
        Component Second;
        int FirstColour = LcdColour;
        if (!Terminal.isRunning()) {
            First = Component.translatable("gui.scp_unleashed.satellite_terminal.status.offline");
            Second = Component.translatable("gui.scp_unleashed.satellite_terminal.status.offline.detail", SatelliteTerminalBlockEntity.Draw);
            FirstColour = RedColour;
        } else if (!Terminal.hasUplink()) {
            First = Component.translatable("gui.scp_unleashed.satellite_terminal.status.no_uplink");
            Second = Component.translatable("gui.scp_unleashed.satellite_terminal.status.no_uplink.detail");
            FirstColour = AmberColour;
        } else if (Selected == null) {
            First = Component.translatable("gui.scp_unleashed.satellite_terminal.status.ready");
            Second = Component.translatable(Terminal.getLocations().isEmpty() ? "gui.scp_unleashed.satellite_terminal.status.empty"
                    : "gui.scp_unleashed.satellite_terminal.status.select");
        } else {
            SkyTarget Known = SkyTargets.get(Selected);
            String Via = Terminal.getSatellites().isEmpty() ? "" : Terminal.getSatellites().getFirst();
            if (Known == null) {
                First = SkyTargets.label(Selected);
                Second = Component.translatable("gui.scp_unleashed.satellite_terminal.status.no_signal");
            } else if (Selected.equals(Scp179Registry.TargetId) && Terminal.getOmen().isActive()) {
                Scp179Omen Omen = Terminal.getOmen();
                First = Component.translatable("gui.scp_unleashed.satellite_terminal.status.pointing", Known.label(), Omen.word().copy().withStyle(ChatFormatting.BOLD));
                FirstColour = Omen.getColour();
                Second = Component.translatable("gui.scp_unleashed.satellite_terminal.status.tracking", Known.region(), Via);
            } else {
                First = Component.translatable("gui.scp_unleashed.satellite_terminal.status.target", Known.label(), Known.name());
                Second = Component.translatable("gui.scp_unleashed.satellite_terminal.status.tracking", Known.region(), Via);
            }
        }
        Graphics.drawString(font, First, X, Y, FirstColour, false);
        Graphics.drawString(font, Second, X, Y + LineHeight + 1, LcdDim, false);
    }

    private void tooltip(GuiGraphics Graphics, ResourceLocation Target, int MouseX, int MouseY) {
        SkyTarget Known = SkyTargets.get(Target);
        List<Component> Lines = new ArrayList<>();
        if (Known == null) {
            Lines.add(Component.literal(Target.toString()));
            Lines.add(Component.translatable("gui.scp_unleashed.satellite_terminal.status.no_signal").withStyle(ChatFormatting.GRAY));
        } else {
            Lines.add(Known.label().copy().append(" ").append(Known.name()));
            Lines.add(Known.region().copy().withStyle(ChatFormatting.GRAY));
        }
        Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        SatelliteTerminalBlockEntity Terminal = terminal();
        if (Terminal != null && Button == 0) {
            int Index = entryAt(Terminal, MouseX, MouseY);
            if (Index >= 0) {
                ResourceLocation Target = Terminal.getLocations().get(Index);
                Picked = true;
                if (!Target.equals(Selected)) {
                    Selected = Target;
                    tune();
                }
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        SatelliteTerminalBlockEntity Terminal = terminal();
        if (Terminal != null && ScrollY != 0.0) {
            int Limit = Math.max(0, Terminal.getLocations().size() - VisibleEntries);
            Scroll = Math.max(0, Math.min(Limit, Scroll - (int) Math.signum(ScrollY)));
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    private void tune() {
        Minecraft Game = Minecraft.getInstance();
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp179Registry.TuneLayers) {
            Game.getSoundManager().play(SimpleSoundInstance.forUI(Layer.get(), 1.0f, TuneVolume));
        }
    }
}
