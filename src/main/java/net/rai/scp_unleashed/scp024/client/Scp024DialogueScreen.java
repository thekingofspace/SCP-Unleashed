package net.rai.scp_unleashed.scp024.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.network.Scp024ReplyPayload;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Scp024DialogueScreen extends Screen {
    public static final ResourceLocation Sheet = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp024/dialogue.png");
    public static final ResourceLocation Portrait = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp024/portrait_host.png");
    public static final int SheetWidth = 512;
    public static final int SheetHeight = 256;
    public static final int BoxWidth = 320;
    public static final int BoxHeight = 84;
    public static final int BottomMargin = 12;
    public static final int PortraitX = 10;
    public static final int PortraitY = 10;
    public static final int PortraitSize = 64;
    public static final int PlateV = 88;
    public static final int PlateWidth = 72;
    public static final int PlateHeight = 15;
    public static final int PlateX = 6;
    public static final int PlateY = -12;
    public static final int ArrowU = 80;
    public static final int ArrowV = 88;
    public static final int ArrowWidth = 9;
    public static final int ArrowHeight = 7;
    public static final int EndU = 100;
    public static final int PipU = 112;
    public static final int PipV = 88;
    public static final int PipSize = 5;
    public static final int TextX = 86;
    public static final int TextY = 12;
    public static final int TextWidth = 224;
    public static final int TextRows = 5;
    public static final int LinePitch = 11;
    public static final int TextColour = 0xFFEDE3CF;
    public static final int PlateColour = 0xFFFFE7A8;
    public static final float CharsPerTick = 1.4f;
    public static final int SentencePause = 6;
    public static final int CommaPause = 3;
    public static final float VoicePitch = 1.12f;
    public static final int BurstGap = 140;
    public static final int BurstJitter = 80;
    private final int Token;
    @Nullable
    private final LocalPlayer Listener;
    private final List<Component> Pages;
    private final List<Integer> Cues;
    private final RandomSource Random = RandomSource.create();
    private final List<String> Lines = new ArrayList<>();
    private int Page;
    private float Shown;
    private int Pause;
    private int TotalChars;
    private int Spoken;
    private int Clock;
    private int NextBurst = BurstGap;
    private boolean Replied;

    public Scp024DialogueScreen(int Token, List<Component> Pages, List<Integer> Cues) {
        super(Component.translatable("gui.scp_unleashed.scp024.plate"));
        this.Token = Token;
        this.Listener = Minecraft.getInstance().player;
        this.Pages = Pages.isEmpty() ? List.of(Component.empty()) : List.copyOf(Pages);
        this.Cues = List.copyOf(Cues);
    }

    @Override
    protected void init() {
        layoutPage();
        if (Clock == 0) {
            cueFor(0);
        }
    }

    private void cueFor(int Index) {
        if (Index < Cues.size()) {
            Scp024Crowd.play(Scp024Cue.of(Cues.get(Index)));
        }
    }

    private void layoutPage() {
        Lines.clear();
        String Text = Pages.get(Page).getString();
        for (FormattedText Line : font.getSplitter().splitLines(Text, TextWidth, Style.EMPTY)) {
            Lines.add(Line.getString());
        }
        while (Lines.size() > TextRows) {
            Lines.removeLast();
        }
        TotalChars = 0;
        for (String Line : Lines) {
            TotalChars += Line.length();
        }
    }

    private int visibleChars(float PartialTick) {
        float Extra = Pause > 0 ? 0.0f : CharsPerTick * PartialTick;
        return Math.min(TotalChars, (int) Math.floor(Shown + Extra));
    }

    private char charAt(int Index) {
        int Remaining = Index;
        for (String Line : Lines) {
            if (Remaining < Line.length()) {
                return Line.charAt(Remaining);
            }
            Remaining -= Line.length();
        }
        return ' ';
    }

    private boolean pageDone() {
        return Shown >= TotalChars;
    }

    public void dismiss() {
        Replied = true;
        onClose();
    }

    @Override
    public void tick() {
        Clock++;
        if (minecraft != null && (minecraft.player == null || minecraft.player != Listener || !minecraft.player.isAlive())) {
            onClose();
            return;
        }
        if (minecraft != null && !Scp024ClientState.inStage(minecraft)) {
            dismiss();
            return;
        }
        if (minecraft != null && Clock >= NextBurst) {
            NextBurst = Clock + BurstGap + Random.nextInt(BurstJitter);
            Scp024ClientState.smallBurst(minecraft);
        }
        if (pageDone()) {
            return;
        }
        if (Pause > 0) {
            Pause--;
            return;
        }
        float Before = Shown;
        Shown = Math.min(TotalChars, Shown + CharsPerTick);
        for (int Index = (int) Before; Index < (int) Shown; Index++) {
            char Letter = charAt(Index);
            if (Character.isLetterOrDigit(Letter)) {
                Spoken++;
                if (Spoken % 2 == 1) {
                    blip();
                }
            }
            if (Letter == '.' || Letter == '!' || Letter == '?') {
                Pause = SentencePause;
                Shown = Index + 1;
                break;
            }
            if (Letter == ',' || Letter == ';' || Letter == ':') {
                Pause = CommaPause;
                Shown = Index + 1;
                break;
            }
        }
    }

    private void blip() {
        if (minecraft == null) {
            return;
        }
        float Pitch = VoicePitch * (0.9f + Random.nextFloat() * 0.2f);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Scp024Registry.Voice.get(), Pitch, 0.3f));
    }

    private void advance() {
        if (!pageDone()) {
            Shown = TotalChars;
            Pause = 0;
            return;
        }
        if (Page < Pages.size() - 1) {
            Page++;
            Shown = 0.0f;
            Pause = 4;
            Spoken = 0;
            layoutPage();
            cueFor(Page);
            return;
        }
        onClose();
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT || Button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            advance();
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean keyPressed(int Key, int Scan, int Modifiers) {
        if (Key == GLFW.GLFW_KEY_SPACE || Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER) {
            advance();
            return true;
        }
        return super.keyPressed(Key, Scan, Modifiers);
    }

    @Override
    public void removed() {
        if (!Replied) {
            Replied = true;
            if (minecraft != null && minecraft.getConnection() != null) {
                PacketDistributor.sendToServer(new Scp024ReplyPayload(Token));
            }
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        int Left = (width - BoxWidth) / 2;
        int Top = height - BoxHeight - BottomMargin;
        Graphics.blit(Sheet, Left, Top, 0, 0, BoxWidth, BoxHeight, SheetWidth, SheetHeight);
        Graphics.blit(Portrait, Left + PortraitX, Top + PortraitY, 0, 0, PortraitSize, PortraitSize, PortraitSize, PortraitSize);
        Graphics.blit(Sheet, Left + PlateX, Top + PlateY, 0, PlateV, PlateWidth, PlateHeight, SheetWidth, SheetHeight);
        Component Plate = getTitle();
        Graphics.drawString(font, Plate, Left + PlateX + (PlateWidth - font.width(Plate)) / 2, Top + PlateY + 4, PlateColour, false);
        int Visible = visibleChars(PartialTick);
        for (int Row = 0; Row < Lines.size() && Visible > 0; Row++) {
            String Line = Lines.get(Row);
            String Part = Line.substring(0, Math.min(Line.length(), Visible));
            Graphics.drawString(font, Part, Left + TextX, Top + TextY + Row * LinePitch, TextColour, false);
            Visible -= Line.length();
        }
        int PipsLeft = Left + BoxWidth - 22 - Pages.size() * (PipSize + 2);
        for (int Index = 0; Index < Pages.size(); Index++) {
            Graphics.blit(Sheet, PipsLeft + Index * (PipSize + 2), Top + BoxHeight - 13, PipU + (Index == Page ? 0 : PipSize + 1), PipV, PipSize, PipSize, SheetWidth, SheetHeight);
        }
        if (pageDone()) {
            boolean Last = Page >= Pages.size() - 1;
            Graphics.blit(Sheet, Left + BoxWidth - 18, Top + BoxHeight - 14, Last ? EndU : ArrowU, ArrowV, ArrowWidth, ArrowHeight, SheetWidth, SheetHeight);
        }
    }
}
