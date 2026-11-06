package net.rai.scp_unleashed.scp001scarlet.king.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;
import net.rai.scp_unleashed.scp001scarlet.king.network.ScarletAnswerPayload;
import org.lwjgl.glfw.GLFW;

public class ScarletConfirmScreen extends Screen {
    public static final ResourceLocation Sheet = ScarletKingRegistry.location("textures/gui/scarlet_king/confirm.png");
    public static final int PanelWidth = 248;
    public static final int PanelHeight = 168;
    public static final int ButtonWidth = 100;
    public static final int ButtonHeight = 20;
    public static final int ConfirmX = 18;
    public static final int CancelX = 130;
    public static final int ButtonY = 130;
    public static final int HoldTicks = 30;
    public static final int TitleColour = 0xFFF6E8C8;
    public static final int TextColour = 0xFFD8C8BE;
    public static final int WarnColour = 0xFFC8786A;
    public static final int Lines = 4;
    private final int Nonce;
    private final boolean Build;
    private boolean Answered;
    private boolean Holding;
    private int Held;

    public ScarletConfirmScreen(int Nonce, boolean Build) {
        super(Component.translatable("screen.scp_unleashed.scarlet_king.title"));
        this.Nonce = Nonce;
        this.Build = Build;
    }

    private int left() {
        return (width - PanelWidth) / 2;
    }

    private int top() {
        return (height - PanelHeight) / 2;
    }

    private boolean over(double MouseX, double MouseY, int X) {
        return MouseX >= left() + X && MouseX < left() + X + ButtonWidth && MouseY >= top() + ButtonY && MouseY < top() + ButtonY + ButtonHeight;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        int Left = left();
        int Top = top();
        Graphics.blit(Sheet, Left, Top, 0, 0, PanelWidth, PanelHeight, 256, 256);
        Graphics.drawCenteredString(font, title, Left + PanelWidth / 2, Top + 26, TitleColour);
        for (int Index = 0; Index < Lines; Index++) {
            Component Line = Component.translatable("screen.scp_unleashed.scarlet_king.line" + Index);
            Graphics.drawCenteredString(font, Line, Left + PanelWidth / 2, Top + 48 + Index * 13, Index == Lines - 1 ? WarnColour : TextColour);
        }
        if (Build) {
            Graphics.drawCenteredString(font, Component.translatable("screen.scp_unleashed.scarlet_king.taken"), Left + PanelWidth / 2, Top + 48 + Lines * 13 + 3, WarnColour);
        }
        boolean OverConfirm = over(MouseX, MouseY, ConfirmX);
        boolean OverCancel = over(MouseX, MouseY, CancelX);
        Graphics.blit(Sheet, Left + ConfirmX, Top + ButtonY, OverConfirm || Holding ? 104 : 0, 176, ButtonWidth, ButtonHeight, 256, 256);
        Graphics.blit(Sheet, Left + CancelX, Top + ButtonY, OverCancel ? 104 : 0, 200, ButtonWidth, ButtonHeight, 256, 256);
        float Share = Math.min(1.0f, (Held + (Holding ? Partial : 0.0f)) / HoldTicks);
        int Fill = Math.round(Share * 96);
        if (Fill > 0) {
            Graphics.blit(Sheet, Left + ConfirmX + 2, Top + ButtonY + 14, 0, 224, Fill, 4, 256, 256);
        }
        Graphics.drawCenteredString(font, Component.translatable("screen.scp_unleashed.scarlet_king.confirm"), Left + ConfirmX + ButtonWidth / 2, Top + ButtonY + 5, 0xFFFFECD2);
        Graphics.drawCenteredString(font, Component.translatable("screen.scp_unleashed.scarlet_king.cancel"), Left + CancelX + ButtonWidth / 2, Top + ButtonY + 5, 0xFFDCD6C8);
    }

    @Override
    public void tick() {
        super.tick();
        if (!Holding) {
            Held = Math.max(0, Held - 2);
            return;
        }
        Held++;
        if (Held % 6 == 0 && minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ScarletKingRegistry.PodBeat.get(), 0.6f + Held / (float) HoldTicks, 0.8f));
        }
        if (Held >= HoldTicks) {
            answer(true);
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT && over(MouseX, MouseY, ConfirmX)) {
            Holding = true;
            return true;
        }
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT && over(MouseX, MouseY, CancelX)) {
            if (minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            }
            answer(false);
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Holding = false;
        }
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean keyPressed(int Key, int Scan, int Modifiers) {
        if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER) {
            Holding = true;
            return true;
        }
        return super.keyPressed(Key, Scan, Modifiers);
    }

    @Override
    public boolean keyReleased(int Key, int Scan, int Modifiers) {
        if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER) {
            Holding = false;
            return true;
        }
        return super.keyReleased(Key, Scan, Modifiers);
    }

    private void answer(boolean Yes) {
        if (Answered) {
            return;
        }
        Answered = true;
        PacketDistributor.sendToServer(new ScarletAnswerPayload(Nonce, Yes));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void onClose() {
        answer(false);
    }

    @Override
    public void removed() {
        if (!Answered) {
            Answered = true;
            PacketDistributor.sendToServer(new ScarletAnswerPayload(Nonce, false));
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
