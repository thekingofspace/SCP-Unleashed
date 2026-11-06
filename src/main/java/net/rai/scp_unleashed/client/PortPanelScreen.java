package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.NetworkObjectBlock;
import net.rai.scp_unleashed.block.RedstonePortBlock;
import net.rai.scp_unleashed.block.entity.PortHolder;
import net.rai.scp_unleashed.network.SetPortPayload;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.wire.WireChannels;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class PortPanelScreen extends Screen {
    private static final int PanelWidth = 128;
    private static final int[] Places = {1000, 100, 10, 1};
    private static final int ColumnPitch = 22;
    private static final int DigitX = 25;
    private static final int DigitY = 27;
    private static final int DigitWidth = 12;
    private static final int DigitHeight = 18;
    private static final int GhostU = 120;
    private static final int ButtonX = 21;
    private static final int ButtonY = 56;
    private static final int ButtonWidth = 20;
    private static final int ButtonHeight = 16;
    private static final int HoveredU = 20;
    private static final int PressedU = 40;
    private static final int LampSize = 6;
    private static final int TitleY = 6;
    private static final int SignalLampY = 85;
    private static final int SignalStatusY = 84;
    private static final int PowerLampX = 14;
    private static final int SignalLampX = 66;
    private static final int PowerTextX = 23;
    private static final int SignalTextX = 75;
    private static final int PowerOffU = 60;
    private static final int PowerOnU = 66;
    private static final int SignalOffU = 72;
    private static final int SignalOnU = 78;
    private static final int[] SelectorX = {14, 48, 82};
    private static final int SelectorY = 78;
    private static final int SelectorWidth = 32;
    private static final int SelectorHeight = 14;
    private static final int SelectorOffU = 60;
    private static final int SelectorHoverU = 92;
    private static final int SelectorOnU = 124;
    private static final int RangeLampY = 104;
    private static final int RangeStatusY = 103;
    private static final int RangePowerOffU = 156;
    private static final int RangePowerOnU = 162;
    private static final String[] SelectorKeys = {"gui.scp_unleashed.port_panel.port", "gui.scp_unleashed.port_panel.from", "gui.scp_unleashed.port_panel.to"};
    private static final int TitleColour = 0xFFE0E0E0;
    private static final int StatusColour = 0xFFC8C8C8;
    private static final int SelectedColour = 0xFFFFFFFF;
    private static final double MaxDistanceSqr = 64.0;

    private final BlockPos Pos;
    private final Style Panel;
    private final int[] Values;
    private int Selected;
    private int Pressed = -1;
    private int Left;
    private int Top;

    public PortPanelScreen(BlockPos Pos, Style Panel, Component Title, int[] Values) {
        super(Title);
        this.Pos = Pos;
        this.Panel = Panel;
        this.Values = Values.clone();
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - PanelWidth) / 2;
        Top = (height - Panel.PanelHeight) / 2;
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null || minecraft.level == null
                || !(minecraft.level.getBlockEntity(Pos) instanceof PortHolder)
                || minecraft.player.distanceToSqr(Vec3.atCenterOf(Pos)) > MaxDistanceSqr) {
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
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(Panel.Texture, Left, Top, Panel.PanelU, 0, PanelWidth, Panel.PanelHeight);
        int Hovered = buttonAt(MouseX, MouseY);
        for (int Index = 0; Index < Places.length; Index++) {
            int Column = Index * ColumnPitch;
            Graphics.blit(Panel.Texture, Left + DigitX + Column, Top + DigitY, GhostU, Panel.DigitV, DigitWidth, DigitHeight);
            Graphics.blit(Panel.Texture, Left + DigitX + Column, Top + DigitY, digit(Index) * DigitWidth, Panel.DigitV, DigitWidth, DigitHeight);
            int ButtonU = Pressed == Index ? PressedU : Hovered == Index ? HoveredU : 0;
            Graphics.blit(Panel.Texture, Left + ButtonX + Column, Top + ButtonY, ButtonU, Panel.SpriteV, ButtonWidth, ButtonHeight);
        }
        BlockState State = minecraft.level.getBlockState(Pos);
        boolean Powered = State.hasProperty(NetworkObjectBlock.Active) && State.getValue(NetworkObjectBlock.Active);
        if (Values.length > 1) {
            int HoveredSelector = selectorAt(MouseX, MouseY);
            for (int Slot = 0; Slot < SelectorX.length && Slot < Values.length; Slot++) {
                int X = Left + SelectorX[Slot];
                int Y = Top + SelectorY;
                int SelectorU = Slot == Selected ? SelectorOnU : Slot == HoveredSelector ? SelectorHoverU : SelectorOffU;
                Graphics.blit(Panel.Texture, X, Y, SelectorU, Panel.SpriteV, SelectorWidth, SelectorHeight);
                Component Label = Component.translatable(SelectorKeys[Slot]);
                Graphics.drawString(font, Label, X + (SelectorWidth - font.width(Label)) / 2, Y + 3, Slot == Selected ? SelectedColour : StatusColour, false);
            }
            Graphics.blit(Panel.Texture, Left + PowerLampX, Top + RangeLampY, Powered ? RangePowerOnU : RangePowerOffU, Panel.SpriteV, LampSize, LampSize);
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.port_panel.range", padded(Values[0]), padded(Math.min(Values[1], Values[2])),
                    padded(Math.max(Values[1], Values[2]))), Left + PowerTextX, Top + RangeStatusY, StatusColour, false);
        } else {
            int Signal = State.hasProperty(RedstonePortBlock.Power) ? State.getValue(RedstonePortBlock.Power) : 0;
            Graphics.blit(Panel.Texture, Left + PowerLampX, Top + SignalLampY, Powered ? PowerOnU : PowerOffU, Panel.SpriteV, LampSize, LampSize);
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.port_panel.power"), Left + PowerTextX, Top + SignalStatusY, StatusColour, false);
            if (Panel.SignalKey != null) {
                Graphics.blit(Panel.Texture, Left + SignalLampX, Top + SignalLampY, Signal > 0 ? SignalOnU : SignalOffU, Panel.SpriteV, LampSize, LampSize);
                Graphics.drawString(font, Component.translatable(Panel.SignalKey, Signal), Left + SignalTextX, Top + SignalStatusY, StatusColour, false);
            }
        }
        RenderSystem.disableBlend();
        Graphics.drawString(font, title, Left + (PanelWidth - font.width(title)) / 2, Top + TitleY, TitleColour, false);
        if (Hovered >= 0 && Pressed < 0) {
            Graphics.renderTooltip(font, Component.translatable("gui.scp_unleashed.port_panel.hint"), MouseX, MouseY);
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        int Index = buttonAt(MouseX, MouseY);
        if (Index >= 0 && (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT || Button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            Pressed = Index;
            play(ModSounds.PanelPress.get(), Button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1.0f : 0.92f, 0.3f);
            changeDigit(Index, Button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1 : -1);
            return true;
        }
        int Slot = selectorAt(MouseX, MouseY);
        if (Slot >= 0 && Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (Slot != Selected) {
                Selected = Slot;
                play(ModSounds.PanelSelect.get(), 1.0f, 0.3f);
            }
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (Pressed >= 0) {
            play(ModSounds.PanelRelease.get(), 1.0f, 0.3f);
        }
        Pressed = -1;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        int Index = columnAt(MouseX, MouseY);
        if (Index >= 0 && ScrollY != 0.0) {
            play(ModSounds.PanelTick.get(), ScrollY > 0.0 ? 1.05f : 0.95f, 0.3f);
            changeDigit(Index, ScrollY > 0.0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        int Typed = Key >= GLFW.GLFW_KEY_0 && Key <= GLFW.GLFW_KEY_9 ? Key - GLFW.GLFW_KEY_0
                : Key >= GLFW.GLFW_KEY_KP_0 && Key <= GLFW.GLFW_KEY_KP_9 ? Key - GLFW.GLFW_KEY_KP_0 : -1;
        if (Typed >= 0) {
            play(ModSounds.PanelPress.get(), 1.0f, 0.3f);
            setValue(Values[Selected] % 1000 * 10 + Typed);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_BACKSPACE) {
            play(ModSounds.PanelRelease.get(), 0.9f, 0.3f);
            setValue(Values[Selected] / 10);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_TAB && Values.length > 1) {
            Selected = (Selected + 1) % Values.length;
            play(ModSounds.PanelSelect.get(), 1.0f, 0.3f);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER || (minecraft != null && minecraft.options.keyInventory.matches(Key, ScanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    private void play(SoundEvent Sound, float Pitch, float Volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, Volume));
        }
    }

    private static String padded(int Value) {
        return String.format("%04d", Value);
    }

    private int digit(int Index) {
        return Values[Selected] / Places[Index] % 10;
    }

    private void changeDigit(int Index, int Delta) {
        int Current = digit(Index);
        setValue(Values[Selected] + (Math.floorMod(Current + Delta, 10) - Current) * Places[Index]);
    }

    private void setValue(int Requested) {
        int Clamped = WireChannels.clampPort(Requested);
        if (Clamped != Values[Selected]) {
            Values[Selected] = Clamped;
            PacketDistributor.sendToServer(new SetPortPayload(Pos, Selected, Clamped));
        }
    }

    private int buttonAt(double MouseX, double MouseY) {
        for (int Index = 0; Index < Places.length; Index++) {
            int X = Left + ButtonX + Index * ColumnPitch;
            int Y = Top + ButtonY;
            if (MouseX >= X && MouseX < X + ButtonWidth && MouseY >= Y && MouseY < Y + ButtonHeight) {
                return Index;
            }
        }
        return -1;
    }

    private int columnAt(double MouseX, double MouseY) {
        for (int Index = 0; Index < Places.length; Index++) {
            int X = Left + ButtonX + Index * ColumnPitch;
            if (MouseX >= X && MouseX < X + ButtonWidth && MouseY >= Top + DigitY - 2 && MouseY < Top + ButtonY + ButtonHeight) {
                return Index;
            }
        }
        return -1;
    }

    private int selectorAt(double MouseX, double MouseY) {
        if (Values.length < 2) {
            return -1;
        }
        for (int Slot = 0; Slot < SelectorX.length && Slot < Values.length; Slot++) {
            int X = Left + SelectorX[Slot];
            int Y = Top + SelectorY;
            if (MouseX >= X && MouseX < X + SelectorWidth && MouseY >= Y && MouseY < Y + SelectorHeight) {
                return Slot;
            }
        }
        return -1;
    }

    public enum Style {
        Sender("redstone_port", 0, 104, 104, 140, "gui.scp_unleashed.port_panel.in"),
        Receiver("redstone_port", 128, 104, 122, 140, "gui.scp_unleashed.port_panel.out"),
        Converter("redstone_converter", 0, 104, 104, 140, "gui.scp_unleashed.port_panel.out"),
        Range("port_range", 0, 128, 128, 146, null),
        Speaker("speaker_panel", 0, 104, 104, 140, null);

        private final ResourceLocation Texture;
        private final int PanelU;
        private final int PanelHeight;
        private final int DigitV;
        private final int SpriteV;
        @Nullable
        private final String SignalKey;

        Style(String TextureName, int PanelU, int PanelHeight, int DigitV, int SpriteV, @Nullable String SignalKey) {
            this.Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/" + TextureName + ".png");
            this.PanelU = PanelU;
            this.PanelHeight = PanelHeight;
            this.DigitV = DigitV;
            this.SpriteV = SpriteV;
            this.SignalKey = SignalKey;
        }

        public static Style of(String KindName) {
            return switch (KindName) {
                case "receiver" -> Receiver;
                case "converter" -> Converter;
                case "port_range" -> Range;
                case "speaker" -> Speaker;
                default -> Sender;
            };
        }
    }
}
