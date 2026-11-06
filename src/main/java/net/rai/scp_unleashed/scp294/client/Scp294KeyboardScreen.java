package net.rai.scp_unleashed.scp294.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp294.Scp294MachineBlockEntity;
import net.rai.scp_unleashed.scp294.Scp294Menu;
import net.rai.scp_unleashed.scp294.Scp294Names;
import net.rai.scp_unleashed.scp294.Scp294Registry;
import net.rai.scp_unleashed.scp294.network.Scp294OrderPayload;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Scp294KeyboardScreen extends AbstractContainerScreen<Scp294Menu> {
    public static final ResourceLocation Texture = Scp294Registry.location("textures/gui/scp294/keyboard.png");
    public static final int TextureWidth = 256;
    public static final int TextureHeight = 272;
    public static final int PanelWidth = 232;
    public static final int PanelHeight = 152;
    public static final int KeySize = 16;
    public static final int KeyPitch = 18;
    public static final int KeyU = 0;
    public static final int KeyV = 156;
    public static final int ClearWidth = 28;
    public static final int ClearU = 54;
    public static final int ClearV = 156;
    public static final int DispenseWidth = 42;
    public static final int DispenseHeight = 34;
    public static final int DispenseU = 0;
    public static final int DispenseV = 174;
    public static final int SpaceWidth = 136;
    public static final int SpaceU = 0;
    public static final int SpaceV = 210;
    public static final int NuggetU = 144;
    public static final int NuggetV = 156;
    public static final int NuggetSize = 8;
    public static final int BackspaceX = 206;
    public static final int BackspaceY = 54;
    public static final int ClearX = 148;
    public static final int ClearY = 108;
    public static final int DispenseX = 182;
    public static final int DispenseY = 90;
    public static final int SpaceX = 48;
    public static final int SpaceY = 126;
    public static final int ReadoutX = 8;
    public static final int ReadoutY = 8;
    public static final int ReadoutRight = 224;
    public static final int ReadoutBottom = 44;
    public static final int TextX = 13;
    public static final int TextY = 13;
    public static final int StatusY = 31;
    public static final int PressTicks = 4;
    public static final int LocalTicks = 40;
    public static final String[] KeyRows = {"1234567890-", "QWERTYUIOP'", "ASDFGHJKL", "ZXCVBNM"};
    public static final int[] RowX = {8, 12, 16, 22};
    public static final int[] RowY = {54, 72, 90, 108};
    public static final int LcdGreen = 0xFF7CF59A;
    public static final int LcdDim = 0xFF2F6B40;
    public static final int LcdAmber = 0xFFFFC05A;
    public static final int LcdRed = 0xFFFF6A52;
    public static final int Legend = 0xFFD7DBE2;
    public static final int LegendPressed = 0xFFAEB4BE;
    public static final int LegendDark = 0xFF3A2208;
    public static final int LegendDim = 0xFF6E747E;
    public static final String Prefix = "gui.scp_unleashed.scp294.";
    public static final String Back = "BACK";
    public static final String Clear = "CLEAR";
    public static final String Dispense = "DISPENSE";
    public static final String Space = "SPACE";

    private final StringBuilder Typed = new StringBuilder();
    private final List<KeyCap> Keys = new ArrayList<>();
    @Nullable
    private String Pressed;
    private int PressLeft;
    private boolean MouseHeld;
    @Nullable
    private Component LocalNotice;
    private int LocalColour;
    private int LocalLeft;
    private int Ticks;

    public record KeyCap(String Id, int X, int Y, int Width, int Height) {
        public boolean contains(double MouseX, double MouseY, int Left, int Top) {
            return MouseX >= Left + X && MouseX < Left + X + Width && MouseY >= Top + Y && MouseY < Top + Y + Height;
        }
    }

    public Scp294KeyboardScreen(Scp294Menu Menu, Inventory Pockets, Component Title) {
        super(Menu, Pockets, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        for (int Row = 0; Row < KeyRows.length; Row++) {
            for (int Index = 0; Index < KeyRows[Row].length(); Index++) {
                Keys.add(new KeyCap(String.valueOf(KeyRows[Row].charAt(Index)), RowX[Row] + Index * KeyPitch, RowY[Row], KeySize, KeySize));
            }
        }
        Keys.add(new KeyCap(Back, BackspaceX, BackspaceY, KeySize, KeySize));
        Keys.add(new KeyCap(Clear, ClearX, ClearY, ClearWidth, KeySize));
        Keys.add(new KeyCap(Dispense, DispenseX, DispenseY, DispenseWidth, DispenseHeight));
        Keys.add(new KeyCap(Space, SpaceX, SpaceY, SpaceWidth, KeySize));
    }

    public String getTyped() {
        return Typed.toString();
    }

    @Nullable
    private Scp294MachineBlockEntity machine() {
        return minecraft == null || minecraft.player == null ? null : menu.machine(minecraft.player);
    }

    private boolean busy() {
        Scp294MachineBlockEntity Machine = machine();
        return Machine != null && Machine.isBusy();
    }

    private boolean free() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasInfiniteMaterials();
    }

    private int nuggets() {
        return minecraft == null || minecraft.player == null ? 0 : Scp294MachineBlockEntity.countNuggets(minecraft.player);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        if (PressLeft > 0 && --PressLeft == 0 && !MouseHeld) {
            Pressed = null;
        }
        if (LocalLeft > 0 && --LocalLeft == 0) {
            LocalNotice = null;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, TextureWidth, TextureHeight);
        KeyCap Hovered = keyAt(MouseX, MouseY);
        for (KeyCap Cap : Keys) {
            drawKey(Graphics, Cap, Cap == Hovered);
        }
        readout(Graphics);
    }

    private void drawKey(GuiGraphics Graphics, KeyCap Cap, boolean Hovered) {
        boolean Down = Cap.Id().equals(Pressed);
        int State = Down ? 2 : Hovered ? 1 : 0;
        int X = leftPos + Cap.X();
        int Y = topPos + Cap.Y();
        switch (Cap.Id()) {
            case Clear -> Graphics.blit(Texture, X, Y, ClearU + State * 30, ClearV, ClearWidth, KeySize, TextureWidth, TextureHeight);
            case Dispense -> {
                int Column = busy() && !Down ? 3 : State;
                Graphics.blit(Texture, X, Y, DispenseU + Column * 44, DispenseV, DispenseWidth, DispenseHeight, TextureWidth, TextureHeight);
            }
            case Space -> Graphics.blit(Texture, X, Y, SpaceU, SpaceV + State * 18, SpaceWidth, KeySize, TextureWidth, TextureHeight);
            default -> Graphics.blit(Texture, X, Y, KeyU + State * 18, KeyV, KeySize, KeySize, TextureWidth, TextureHeight);
        }
        String Label = switch (Cap.Id()) {
            case Back -> "←";
            case Clear -> Component.translatable(Prefix + "clear").getString();
            case Dispense -> Component.translatable(Prefix + "enter").getString();
            case Space -> "";
            default -> Cap.Id();
        };
        if (Label.isEmpty()) {
            return;
        }
        int Colour = Cap.Id().equals(Dispense) ? (busy() ? LegendDim : LegendDark) : Down ? LegendPressed : Legend;
        int Drop = Down ? 2 : 0;
        int LabelX = X + (Cap.Width() - font.width(Label) + 1) / 2;
        int LabelY = Y + (Cap.Height() - 2 - 8) / 2 + Drop;
        Graphics.drawString(font, Label, LabelX, LabelY, Colour, false);
    }

    private void readout(GuiGraphics Graphics) {
        int X = leftPos + TextX;
        int Y = topPos + TextY;
        boolean Blink = Ticks / 6 % 2 == 0;
        String Shown = Typed.toString().toUpperCase(Locale.ROOT);
        if (Shown.isEmpty()) {
            Graphics.drawString(font, "> ", X, Y, LcdGreen, false);
            if (Blink) {
                Graphics.drawString(font, "_", X + font.width("> "), Y, LcdGreen, false);
            }
        } else {
            String Line = "> " + Shown;
            Graphics.drawString(font, Line, X, Y, LcdGreen, false);
            if (Blink && Typed.length() < Scp294Names.MaxTyped) {
                Graphics.drawString(font, "_", X + font.width(Line), Y, LcdGreen, false);
            }
        }
        Component Status;
        int Colour;
        Scp294MachineBlockEntity Machine = machine();
        int Notice = Machine == null ? Scp294MachineBlockEntity.NoticeNone : Machine.getNotice();
        if (LocalNotice != null) {
            Status = LocalNotice;
            Colour = LocalColour;
        } else if (Machine != null && Machine.isBusy()) {
            String Dots = ".".repeat(Ticks / 5 % 4);
            Status = Component.translatable(Prefix + "status.dispensing").append(Dots);
            Colour = LcdAmber;
        } else if (Notice == Scp294MachineBlockEntity.NoticeOutOfRange) {
            Status = Component.translatable(Prefix + "status.out_of_range");
            Colour = Blink ? LcdRed : 0xFF8A3020;
        } else if (Notice == Scp294MachineBlockEntity.NoticeNoCoin) {
            Status = Component.translatable(Prefix + "status.no_coin");
            Colour = LcdRed;
        } else if (Notice == Scp294MachineBlockEntity.NoticeBusy) {
            Status = Component.translatable(Prefix + "status.busy");
            Colour = LcdAmber;
        } else if (Notice == Scp294MachineBlockEntity.NoticeEnjoy) {
            Status = Component.translatable(Prefix + "status.enjoy");
            Colour = LcdGreen;
        } else if (!free() && nuggets() <= 0) {
            Status = Component.translatable(Prefix + "status.insert");
            Colour = Blink ? LcdAmber : 0xFF8A6A30;
        } else if (Typed.isEmpty()) {
            Status = Component.translatable(Prefix + "status.type");
            Colour = Blink ? LcdGreen : LcdDim;
        } else {
            Status = Component.translatable(Prefix + "status.ready");
            Colour = LcdGreen;
        }
        Graphics.drawString(font, Status, X, topPos + StatusY, Colour, false);
        String Coins = free() ? Component.translatable(Prefix + "free").getString() : "x" + nuggets();
        int CoinsX = leftPos + ReadoutRight - 5 - font.width(Coins);
        Graphics.drawString(font, Coins, CoinsX, topPos + StatusY, free() || nuggets() > 0 ? LcdGreen : LcdDim, false);
        Graphics.blit(Texture, CoinsX - NuggetSize - 2, topPos + StatusY, NuggetU, NuggetV, NuggetSize, NuggetSize, TextureWidth, TextureHeight);
    }

    private boolean overCoins(double MouseX, double MouseY) {
        int Right = leftPos + ReadoutRight - 4;
        return MouseX >= Right - 48 && MouseX < Right && MouseY >= topPos + StatusY - 2 && MouseY < topPos + StatusY + 10;
    }

    @Nullable
    private KeyCap keyAt(double MouseX, double MouseY) {
        for (KeyCap Cap : Keys) {
            if (Cap.contains(MouseX, MouseY, leftPos, topPos)) {
                return Cap;
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        KeyCap Hovered = keyAt(MouseX, MouseY);
        List<Component> Lines = null;
        if (Hovered != null && Hovered.Id().equals(Dispense)) {
            Lines = List.of(Component.translatable(Prefix + "dispense.tooltip"),
                    Component.translatable(Prefix + (busy() ? "dispense.busy" : free() ? "dispense.free" : "dispense.cost")).withStyle(ChatFormatting.GRAY));
        } else if (Hovered != null && Hovered.Id().equals(Clear)) {
            Lines = List.of(Component.translatable(Prefix + "clear.tooltip"));
        } else if (Hovered != null && Hovered.Id().equals(Back)) {
            Lines = List.of(Component.translatable(Prefix + "backspace.tooltip"));
        } else if (Hovered == null && overCoins(MouseX, MouseY)) {
            Lines = List.of(Component.translatable(Prefix + "coins.tooltip", nuggets()));
        }
        if (Lines != null) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            KeyCap Cap = keyAt(MouseX, MouseY);
            if (Cap != null) {
                MouseHeld = true;
                press(Cap.Id());
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        MouseHeld = false;
        if (PressLeft <= 0) {
            Pressed = null;
        }
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean keyPressed(int KeyCode, int ScanCode, int Modifiers) {
        if (KeyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(KeyCode, ScanCode, Modifiers);
        }
        if (KeyCode == GLFW.GLFW_KEY_BACKSPACE) {
            press(Back);
            return true;
        }
        if (KeyCode == GLFW.GLFW_KEY_ENTER || KeyCode == GLFW.GLFW_KEY_KP_ENTER) {
            press(Dispense);
            return true;
        }
        if (KeyCode == GLFW.GLFW_KEY_DELETE) {
            press(Clear);
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char Letter, int Modifiers) {
        if (!Scp294Names.allowed(Letter)) {
            return false;
        }
        String Id = Letter == ' ' ? Space : String.valueOf(Character.toUpperCase(Letter));
        press(Id);
        return true;
    }

    private void press(String Id) {
        Pressed = Id;
        PressLeft = PressTicks;
        switch (Id) {
            case Back -> {
                if (!Typed.isEmpty()) {
                    Typed.setLength(Typed.length() - 1);
                    click(1.5f);
                } else {
                    click(0.8f);
                }
            }
            case Clear -> {
                Typed.setLength(0);
                click(1.2f);
            }
            case Dispense -> dispense();
            case Space -> append(' ');
            default -> append(Id.charAt(0));
        }
    }

    private void append(char Letter) {
        if (Typed.length() >= Scp294Names.MaxTyped) {
            click(0.6f);
            return;
        }
        if (Letter == ' ' && (Typed.isEmpty() || Typed.charAt(Typed.length() - 1) == ' ')) {
            click(0.9f);
            return;
        }
        Typed.append(Character.toLowerCase(Letter));
        click(1.55f + (Letter * 7 % 9) * 0.04f);
    }

    private void dispense() {
        String Order = Typed.toString().trim();
        if (Order.isEmpty()) {
            local(Component.translatable(Prefix + "status.type"), LcdAmber);
            play(Scp294Registry.Refuse.get(), 1.4f, 0.35f);
            return;
        }
        if (busy()) {
            local(Component.translatable(Prefix + "status.busy"), LcdAmber);
            play(Scp294Registry.Refuse.get(), 1.25f, 0.35f);
            return;
        }
        click(1.1f);
        LocalNotice = null;
        LocalLeft = 0;
        PacketDistributor.sendToServer(new Scp294OrderPayload(Order));
    }

    private void local(Component Text, int Colour) {
        LocalNotice = Text;
        LocalColour = Colour;
        LocalLeft = LocalTicks;
    }

    private void click(float Pitch) {
        play(Scp294Registry.Key.get(), Pitch, 0.6f);
    }

    private void play(SoundEvent Sound, float Pitch, float Volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, Volume));
        }
    }
}
