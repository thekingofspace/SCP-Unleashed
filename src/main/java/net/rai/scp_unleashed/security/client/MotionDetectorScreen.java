package net.rai.scp_unleashed.security.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.network.SetPortPayload;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.security.DetectorConfigPayload;
import net.rai.scp_unleashed.security.MotionDetectorBlock;
import net.rai.scp_unleashed.security.MotionDetectorBlockEntity;
import net.rai.scp_unleashed.security.SecurityRegistry;
import net.rai.scp_unleashed.wire.WireChannels;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MotionDetectorScreen extends Screen {
    public static final ResourceLocation Texture = SecurityRegistry.location("textures/gui/security/motion_detector.png");
    public static final ResourceLocation DigitTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/redstone_port.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 142;
    public static final int TitleY = 7;
    public static final int[] Places = {1000, 100, 10, 1};
    public static final int DigitX = 49;
    public static final int DigitY = 24;
    public static final int DigitWidth = 12;
    public static final int DigitHeight = 18;
    public static final int DigitV = 104;
    public static final int GhostU = 120;
    public static final int ColumnPitch = 22;
    public static final int ArrowX = 45;
    public static final int ArrowY = 46;
    public static final int ArrowWidth = 20;
    public static final int ArrowHeight = 16;
    public static final int ArrowV = 140;
    public static final int RowLabelX = 12;
    public static final int RangeY = 70;
    public static final int SignalY = 87;
    public static final int FilterY = 104;
    public static final int ControlHeight = 13;
    public static final int MinusX = 70;
    public static final int PlusX = 116;
    public static final int StepWidth = 14;
    public static final int ValueX = 86;
    public static final int ValueWidth = 28;
    public static final int[] ModeX = {70, 112};
    public static final int[] ModeWidth = {40, 40};
    public static final int[] FilterX = {70, 96, 144};
    public static final int[] FilterWidth = {24, 46, 26};
    public static final int ButtonU = 0;
    public static final int ButtonV = 144;
    public static final int ButtonSprite = 16;
    public static final int Cap = 3;
    public static final int LampU = 48;
    public static final int LampV = 144;
    public static final int LampSize = 6;
    public static final int StatusY = 125;
    public static final int LampX = 12;
    public static final int TitleColour = 0xFFE0E0E0;
    public static final int LabelColour = 0xFFB8BEC6;
    public static final int ValueColour = 0xFF7CF08C;
    public static final int SelectedColour = 0xFFFFFFFF;
    public static final int IdleColour = 0xFF9CA4AC;
    public static final double MaxDistanceSqr = 64.0;
    public static final String[] ModeKeys = {"gui.scp_unleashed.motion_detector.mode.pulse", "gui.scp_unleashed.motion_detector.mode.hold"};
    public static final String[] FilterKeys = {"gui.scp_unleashed.motion_detector.filter.all", "gui.scp_unleashed.motion_detector.filter.players",
            "gui.scp_unleashed.motion_detector.filter.mobs"};

    private final BlockPos Pos;
    private int Port;
    private int Pressed = -1;
    private int Left;
    private int Top;

    public MotionDetectorScreen(BlockPos Pos, Component Title, int Port) {
        super(Title);
        this.Pos = Pos;
        this.Port = Port;
    }

    @Nullable
    private MotionDetectorBlockEntity detector() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(Pos) instanceof MotionDetectorBlockEntity Detector ? Detector : null;
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - PanelWidth) / 2;
        Top = (height - PanelHeight) / 2;
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null || detector() == null || minecraft.player.distanceToSqr(Vec3.atCenterOf(Pos)) > MaxDistanceSqr) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        MotionDetectorBlockEntity Detector = detector();
        if (Detector == null || minecraft == null || minecraft.level == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight);
        double GuiX = MouseX - Left;
        double GuiY = MouseY - Top;
        int HoveredArrow = arrowAt(GuiX, GuiY);
        for (int Index = 0; Index < Places.length; Index++) {
            int Column = Index * ColumnPitch;
            Graphics.blit(DigitTexture, Left + DigitX + Column, Top + DigitY, GhostU, DigitV, DigitWidth, DigitHeight);
            Graphics.blit(DigitTexture, Left + DigitX + Column, Top + DigitY, Port / Places[Index] % 10 * DigitWidth, DigitV, DigitWidth, DigitHeight);
            int U = Pressed == Index ? 40 : HoveredArrow == Index ? 20 : 0;
            Graphics.blit(DigitTexture, Left + ArrowX + Column, Top + ArrowY, U, ArrowV, ArrowWidth, ArrowHeight);
        }
        button(Graphics, MinusX, RangeY, StepWidth, inside(GuiX, GuiY, MinusX, RangeY, StepWidth, ControlHeight) ? 1 : 0);
        button(Graphics, PlusX, RangeY, StepWidth, inside(GuiX, GuiY, PlusX, RangeY, StepWidth, ControlHeight) ? 1 : 0);
        for (int Index = 0; Index < ModeX.length; Index++) {
            button(Graphics, ModeX[Index], SignalY, ModeWidth[Index], Detector.getMode() == Index ? 2 : inside(GuiX, GuiY, ModeX[Index], SignalY, ModeWidth[Index], ControlHeight) ? 1 : 0);
        }
        for (int Index = 0; Index < FilterX.length; Index++) {
            button(Graphics, FilterX[Index], FilterY, FilterWidth[Index],
                    Detector.getFilter() == Index ? 2 : inside(GuiX, GuiY, FilterX[Index], FilterY, FilterWidth[Index], ControlHeight) ? 1 : 0);
        }
        BlockState State = minecraft.level.getBlockState(Pos);
        boolean Tripped = State.hasProperty(MotionDetectorBlock.Tripped) && State.getValue(MotionDetectorBlock.Tripped);
        boolean Wired = State.hasProperty(MotionDetectorBlock.Connected) && State.getValue(MotionDetectorBlock.Connected);
        boolean Blink = Tripped && minecraft.level.getGameTime() / 4 % 2 == 0;
        Graphics.blit(Texture, Left + LampX, Top + StatusY + 1, LampU + (Blink ? 1 : Tripped ? 0 : 2) * LampSize, LampV, LampSize, LampSize);
        RenderSystem.disableBlend();
        Graphics.drawString(font, title, Left + (PanelWidth - font.width(title)) / 2, Top + TitleY, TitleColour, false);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.motion_detector.range"), Left + RowLabelX, Top + RangeY + 3, LabelColour, false);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.motion_detector.signal"), Left + RowLabelX, Top + SignalY + 3, LabelColour, false);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.motion_detector.detects"), Left + RowLabelX, Top + FilterY + 3, LabelColour, false);
        centred(Graphics, Component.literal("-"), MinusX, RangeY, StepWidth, SelectedColour);
        centred(Graphics, Component.literal("+"), PlusX, RangeY, StepWidth, SelectedColour);
        centred(Graphics, Component.translatable("gui.scp_unleashed.motion_detector.metres", Detector.getRange()), ValueX, RangeY, ValueWidth, ValueColour);
        for (int Index = 0; Index < ModeX.length; Index++) {
            centred(Graphics, Component.translatable(ModeKeys[Index]), ModeX[Index], SignalY, ModeWidth[Index], Detector.getMode() == Index ? SelectedColour : IdleColour);
        }
        for (int Index = 0; Index < FilterX.length; Index++) {
            centred(Graphics, Component.translatable(FilterKeys[Index]), FilterX[Index], FilterY, FilterWidth[Index], Detector.getFilter() == Index ? SelectedColour : IdleColour);
        }
        Component Status = Component.translatable(Tripped ? "gui.scp_unleashed.motion_detector.status.tripped" : "gui.scp_unleashed.motion_detector.status.watching");
        Graphics.drawString(font, Status, Left + LampX + LampSize + 4, Top + StatusY, Tripped ? 0xFFF06C5C : LabelColour, false);
        Component Wire = Component.translatable(Wired ? "gui.scp_unleashed.motion_detector.wired" : "gui.scp_unleashed.motion_detector.unwired");
        Graphics.drawString(font, Wire, Left + PanelWidth - 12 - font.width(Wire), Top + StatusY, Wired ? LabelColour : 0xFFE0A040, false);
        List<Component> Lines = tooltipAt(GuiX, GuiY, Detector, Wired);
        if (!Lines.isEmpty() && Pressed < 0) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private void centred(GuiGraphics Graphics, Component Text, int X, int Y, int Width, int Colour) {
        Graphics.drawString(font, Text, Left + X + (Width - font.width(Text) + 1) / 2, Top + Y + 3, Colour, false);
    }

    private void button(GuiGraphics Graphics, int X, int Y, int Width, int State) {
        int U = ButtonU + State * ButtonSprite;
        Graphics.blit(Texture, Left + X, Top + Y, U, ButtonV, Cap, ControlHeight);
        for (int Column = Cap; Column < Width - Cap; Column++) {
            Graphics.blit(Texture, Left + X + Column, Top + Y, U + Cap + (Column - Cap) % (ButtonSprite - 2 * Cap), ButtonV, 1, ControlHeight);
        }
        Graphics.blit(Texture, Left + X + Width - Cap, Top + Y, U + ButtonSprite - Cap, ButtonV, Cap, ControlHeight);
    }

    private List<Component> tooltipAt(double GuiX, double GuiY, MotionDetectorBlockEntity Detector, boolean Wired) {
        List<Component> Lines = new ArrayList<>();
        if (arrowAt(GuiX, GuiY) >= 0 || inside(GuiX, GuiY, DigitX, DigitY, 3 * ColumnPitch + DigitWidth, DigitHeight)) {
            Lines.add(Component.translatable("gui.scp_unleashed.motion_detector.port", String.format("%04d", Port)));
            Lines.add(Component.translatable("gui.scp_unleashed.port_panel.hint").withStyle(ChatFormatting.GRAY));
        } else if (inside(GuiX, GuiY, MinusX, RangeY, PlusX + StepWidth - MinusX, ControlHeight)) {
            Lines.add(Component.translatable("gui.scp_unleashed.motion_detector.range.hint", MotionDetectorBlockEntity.MinRange, MotionDetectorBlockEntity.MaxRange));
        } else if (inside(GuiX, GuiY, ModeX[0], SignalY, ModeX[1] + ModeWidth[1] - ModeX[0], ControlHeight)) {
            Lines.add(Component.translatable(Detector.getMode() == MotionDetectorBlockEntity.ModeHold ? "gui.scp_unleashed.motion_detector.mode.hold.hint"
                    : "gui.scp_unleashed.motion_detector.mode.pulse.hint"));
        } else if (inside(GuiX, GuiY, FilterX[0], FilterY, FilterX[2] + FilterWidth[2] - FilterX[0], ControlHeight)) {
            Lines.add(Component.translatable("gui.scp_unleashed.motion_detector.filter.hint"));
        } else if (inside(GuiX, GuiY, PanelWidth / 2, StatusY - 2, PanelWidth / 2 - 8, 12) && !Wired) {
            Lines.add(Component.translatable("gui.scp_unleashed.motion_detector.unwired.hint").withStyle(ChatFormatting.GRAY));
        }
        return Lines;
    }

    private static boolean inside(double GuiX, double GuiY, int X, int Y, int Width, int Height) {
        return GuiX >= X && GuiX < X + Width && GuiY >= Y && GuiY < Y + Height;
    }

    private int arrowAt(double GuiX, double GuiY) {
        for (int Index = 0; Index < Places.length; Index++) {
            if (inside(GuiX, GuiY, ArrowX + Index * ColumnPitch, ArrowY, ArrowWidth, ArrowHeight)) {
                return Index;
            }
        }
        return -1;
    }

    private int columnAt(double GuiX, double GuiY) {
        for (int Index = 0; Index < Places.length; Index++) {
            if (inside(GuiX, GuiY, ArrowX + Index * ColumnPitch, DigitY - 2, ArrowWidth, ArrowY + ArrowHeight - DigitY + 2)) {
                return Index;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        double GuiX = MouseX - Left;
        double GuiY = MouseY - Top;
        MotionDetectorBlockEntity Detector = detector();
        int Arrow = arrowAt(GuiX, GuiY);
        if (Arrow >= 0 && (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT || Button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            Pressed = Arrow;
            play(ModSounds.PanelPress.get(), Button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1.0f : 0.92f);
            changeDigit(Arrow, Button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1 : -1);
            return true;
        }
        if (Detector != null && Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (inside(GuiX, GuiY, MinusX, RangeY, StepWidth, ControlHeight)) {
                setting(MotionDetectorBlockEntity.SettingRange, Detector.getRange() - 1);
                play(ModSounds.PanelTick.get(), 0.95f);
                return true;
            }
            if (inside(GuiX, GuiY, PlusX, RangeY, StepWidth, ControlHeight)) {
                setting(MotionDetectorBlockEntity.SettingRange, Detector.getRange() + 1);
                play(ModSounds.PanelTick.get(), 1.05f);
                return true;
            }
            for (int Index = 0; Index < ModeX.length; Index++) {
                if (inside(GuiX, GuiY, ModeX[Index], SignalY, ModeWidth[Index], ControlHeight)) {
                    setting(MotionDetectorBlockEntity.SettingMode, Index);
                    play(ModSounds.PanelSelect.get(), 1.0f);
                    return true;
                }
            }
            for (int Index = 0; Index < FilterX.length; Index++) {
                if (inside(GuiX, GuiY, FilterX[Index], FilterY, FilterWidth[Index], ControlHeight)) {
                    setting(MotionDetectorBlockEntity.SettingFilter, Index);
                    play(ModSounds.PanelSelect.get(), 1.0f);
                    return true;
                }
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (Pressed >= 0) {
            play(ModSounds.PanelRelease.get(), 1.0f);
        }
        Pressed = -1;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        double GuiX = MouseX - Left;
        double GuiY = MouseY - Top;
        int Index = columnAt(GuiX, GuiY);
        if (Index >= 0 && ScrollY != 0.0) {
            play(ModSounds.PanelTick.get(), ScrollY > 0.0 ? 1.05f : 0.95f);
            changeDigit(Index, ScrollY > 0.0 ? 1 : -1);
            return true;
        }
        MotionDetectorBlockEntity Detector = detector();
        if (Detector != null && ScrollY != 0.0 && inside(GuiX, GuiY, MinusX, RangeY, PlusX + StepWidth - MinusX, ControlHeight)) {
            setting(MotionDetectorBlockEntity.SettingRange, Detector.getRange() + (ScrollY > 0.0 ? 1 : -1));
            play(ModSounds.PanelTick.get(), ScrollY > 0.0 ? 1.05f : 0.95f);
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        int Typed = Key >= GLFW.GLFW_KEY_0 && Key <= GLFW.GLFW_KEY_9 ? Key - GLFW.GLFW_KEY_0
                : Key >= GLFW.GLFW_KEY_KP_0 && Key <= GLFW.GLFW_KEY_KP_9 ? Key - GLFW.GLFW_KEY_KP_0 : -1;
        if (Typed >= 0) {
            play(ModSounds.PanelPress.get(), 1.0f);
            setPort(Port % 1000 * 10 + Typed);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_BACKSPACE) {
            play(ModSounds.PanelRelease.get(), 0.9f);
            setPort(Port / 10);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER || (minecraft != null && minecraft.options.keyInventory.matches(Key, ScanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    private void changeDigit(int Index, int Delta) {
        int Current = Port / Places[Index] % 10;
        setPort(Port + (Math.floorMod(Current + Delta, 10) - Current) * Places[Index]);
    }

    private void setPort(int Requested) {
        int Clamped = WireChannels.clampPort(Requested);
        if (Clamped != Port) {
            Port = Clamped;
            PacketDistributor.sendToServer(new SetPortPayload(Pos, 0, Clamped));
        }
    }

    private void setting(int Setting, int Value) {
        PacketDistributor.sendToServer(new DetectorConfigPayload(Pos, Setting, Value));
    }

    private void play(SoundEvent Sound, float Pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, 0.3f));
        }
    }
}
