package net.rai.scp_unleashed.gateway.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.gateway.ContainedGatewayBlockEntity;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayRegistry;
import net.rai.scp_unleashed.gateway.network.GatewayEnterPayload;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ContentWarningScreen extends Screen {
    public static final ResourceLocation Sheet = GatewayRegistry.location("textures/gui/gateway/warning.png");
    public static final int SheetWidth = 512;
    public static final int SheetHeight = 256;
    public static final int PanelWidth = 280;
    public static final int PanelHeight = 184;
    public static final int ButtonWidth = 104;
    public static final int ButtonHeight = 22;
    public static final int ButtonV = 192;
    public static final int EnterU = 0;
    public static final int BackU = 216;
    public static final int ButtonY = 150;
    public static final int EnterX = 28;
    public static final int BackX = 148;
    public static final int TextX = 20;
    public static final int TextTop = 52;
    public static final int TextWidth = 240;
    public static final int TextRows = 8;
    public static final int LinePitch = 11;
    public static final int TitleY = 22;
    public static final int NameY = 36;
    public static final int TitleColour = 0xFFF2D27A;
    public static final int NameColour = 0xFF9FB0C8;
    public static final int TextColour = 0xFFD8DCE4;
    public static final int ButtonText = 0xFFF0F2F6;
    public static final int ArmTicks = 30;
    public static final double PushBack = 0.42;
    private final BlockPos Gate;
    private final GatewayDestination Destination;
    private final List<FormattedCharSequence> Lines = new ArrayList<>();
    private int Age;
    private boolean Answered;

    public ContentWarningScreen(BlockPos Gate, GatewayDestination Destination) {
        super(Destination.warningTitle());
        this.Gate = Gate;
        this.Destination = Destination;
    }

    @Override
    protected void init() {
        Lines.clear();
        for (Component Line : Destination.warningLines()) {
            for (FormattedText Part : font.getSplitter().splitLines(Line, TextWidth, Style.EMPTY)) {
                if (Lines.size() < TextRows) {
                    Lines.add(Language.getInstance().getVisualOrder(Part));
                }
            }
            if (Lines.size() < TextRows) {
                Lines.add(FormattedCharSequence.EMPTY);
            }
        }
        while (!Lines.isEmpty() && Lines.getLast() == FormattedCharSequence.EMPTY) {
            Lines.removeLast();
        }
    }

    @Override
    public void tick() {
        Age++;
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Client.player == null || !Client.player.isAlive()) {
            onClose();
            return;
        }
        if (!(Client.level.getBlockEntity(Gate) instanceof ContainedGatewayBlockEntity Found) || !Found.isOpen()) {
            onClose();
        }
    }

    private int left() {
        return (width - PanelWidth) / 2;
    }

    private int top() {
        return (height - PanelHeight) / 2;
    }

    private boolean armed() {
        return Age >= ArmTicks;
    }

    private boolean over(double MouseX, double MouseY, int X, int Y) {
        return MouseX >= X && MouseX < X + ButtonWidth && MouseY >= Y && MouseY < Y + ButtonHeight;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int Left = left();
            int Top = top();
            if (over(MouseX, MouseY, Left + EnterX, Top + ButtonY) && armed()) {
                enter();
                return true;
            }
            if (over(MouseX, MouseY, Left + BackX, Top + ButtonY)) {
                turnBack();
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean keyPressed(int Key, int Scan, int Modifiers) {
        if (Key == GLFW.GLFW_KEY_ESCAPE) {
            turnBack();
            return true;
        }
        return super.keyPressed(Key, Scan, Modifiers);
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 0.6f));
    }

    private void enter() {
        if (Answered) {
            return;
        }
        Answered = true;
        click();
        PacketDistributor.sendToServer(new GatewayEnterPayload(Gate));
        onClose();
    }

    private void turnBack() {
        if (Answered) {
            return;
        }
        Answered = true;
        click();
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        if (User != null && Client.level != null && Client.level.getBlockEntity(Gate) instanceof ContainedGatewayBlockEntity Found) {
            Vec3 Middle = Found.middle();
            Vec3 Away = new Vec3(User.getX() - Middle.x, 0.0, User.getZ() - Middle.z);
            if (Away.lengthSqr() < 1.0E-4) {
                Away = new Vec3(Found.front().getStepX(), 0.0, Found.front().getStepZ());
            }
            Vec3 Push = Away.normalize().scale(PushBack);
            User.setDeltaMovement(Push.x, User.getDeltaMovement().y, Push.z);
        }
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        Graphics.fill(0, 0, width, height, 0xB0080A0E);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        renderBackground(Graphics, MouseX, MouseY, PartialTick);
        int Left = left();
        int Top = top();
        Graphics.blit(Sheet, Left, Top, 0, 0, PanelWidth, PanelHeight, SheetWidth, SheetHeight);
        Component Title = Destination.warningTitle();
        Graphics.drawString(font, Title, Left + (PanelWidth - font.width(Title)) / 2, Top + TitleY, TitleColour, false);
        Component Name = Destination.displayName();
        Graphics.drawString(font, Name, Left + (PanelWidth - font.width(Name)) / 2, Top + NameY, NameColour, false);
        for (int Row = 0; Row < Lines.size(); Row++) {
            Graphics.drawString(font, Lines.get(Row), Left + TextX, Top + TextTop + Row * LinePitch, TextColour, false);
        }
        drawButton(Graphics, Left + EnterX, Top + ButtonY, EnterU, Component.translatable("gui.scp_unleashed.gateway.enter"), MouseX, MouseY, armed());
        drawButton(Graphics, Left + BackX, Top + ButtonY, BackU, Component.translatable("gui.scp_unleashed.gateway.turn_back"), MouseX, MouseY, true);
    }

    private void drawButton(GuiGraphics Graphics, int X, int Y, int U, Component Label, int MouseX, int MouseY, boolean Live) {
        int State = !Live ? 2 : over(MouseX, MouseY, X, Y) ? 1 : 0;
        Graphics.blit(Sheet, X, Y, U, ButtonV, ButtonWidth, ButtonHeight, SheetWidth, SheetHeight);
        if (State > 0) {
            Graphics.blit(Sheet, X, Y, U + ButtonWidth + 4, ButtonV + (State == 2 ? ButtonHeight + 4 : 0), ButtonWidth, ButtonHeight, SheetWidth, SheetHeight);
        }
        int Colour = Live ? ButtonText : 0xFF7A808C;
        Graphics.drawString(font, Label, X + (ButtonWidth - font.width(Label)) / 2, Y + (ButtonHeight - 8) / 2, Colour, false);
    }
}
