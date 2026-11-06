package net.rai.scp_unleashed.stranger.client;

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
import net.rai.scp_unleashed.lang.LangIndex;
import net.rai.scp_unleashed.stranger.StrangerEntity;
import net.rai.scp_unleashed.stranger.StrangerRegistry;
import net.rai.scp_unleashed.stranger.StrangerVariant;
import net.rai.scp_unleashed.stranger.network.StrangerReplyPayload;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class StrangerDialogueScreen extends Screen {
    public static final ResourceLocation Sheet = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/stranger/dialogue.png");
    public static final int SheetWidth = 512;
    public static final int SheetHeight = 256;
    public static final int BoxWidth = 320;
    public static final int BoxHeight = 84;
    public static final int BottomMargin = 12;
    public static final int PortraitX = 10;
    public static final int PortraitY = 10;
    public static final int PortraitSize = 64;
    public static final int PlateU = 0;
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
    public static final int TextColour = 0xFFD6DAE4;
    public static final int PlateColour = 0xFFE2E6EE;
    public static final float CharsPerTick = 1.25f;
    public static final int SentencePause = 7;
    public static final int CommaPause = 3;
    private final int EntityId;
    @Nullable
    private final LocalPlayer Listener;
    private final StrangerVariant Variant;
    private final String Prefix;
    private final List<Integer> PageKeys;
    private final int Pages;
    private final RandomSource Random = RandomSource.create();
    private final List<String> Lines = new ArrayList<>();
    private int Page;
    private float Shown;
    private int Pause;
    private int TotalChars;
    private int Spoken;
    private boolean Completed;
    private boolean Replied;

    public StrangerDialogueScreen(int EntityId, StrangerVariant Variant, String Prefix, int Pages) {
        super(Variant.displayName());
        this.EntityId = EntityId;
        this.Listener = Minecraft.getInstance().player;
        this.Variant = Variant;
        this.Prefix = Prefix;
        this.PageKeys = LangIndex.current().indicesOr(Prefix, Math.max(1, Pages));
        this.Pages = this.PageKeys.size();
    }

    @Override
    protected void init() {
        layoutPage();
    }

    private void layoutPage() {
        Lines.clear();
        String Text = Variant.page(Prefix, PageKeys.get(Page)).getString();
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

    private boolean isAbandoned(Minecraft Client) {
        if (Client.level == null || Client.player == null || Client.player != Listener || !Client.player.isAlive()) {
            return true;
        }
        return !(Client.level.getEntity(EntityId) instanceof StrangerEntity Figure) || Figure.isRemoved() || Figure.isLeaving();
    }

    @Override
    public void tick() {
        if (minecraft != null && isAbandoned(minecraft)) {
            onClose();
            return;
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
        float Pitch = Variant.VoicePitch() * (0.92f + Random.nextFloat() * 0.16f);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(StrangerRegistry.Blip.get(), Pitch, 0.32f));
    }

    private void advance() {
        if (!pageDone()) {
            Shown = TotalChars;
            Pause = 0;
            return;
        }
        if (Page < Pages - 1) {
            Page++;
            Shown = 0.0f;
            Pause = 4;
            Spoken = 0;
            layoutPage();
            return;
        }
        Completed = true;
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
            boolean Finished = Completed || Page >= Pages - 1 && pageDone();
            if (minecraft != null && minecraft.getConnection() != null) {
                PacketDistributor.sendToServer(new StrangerReplyPayload(EntityId, Finished));
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
        Graphics.blit(Variant.Portrait(), Left + PortraitX, Top + PortraitY, 0, 0, PortraitSize, PortraitSize, PortraitSize * 2, PortraitSize);
        Graphics.blit(Sheet, Left + PlateX, Top + PlateY, PlateU, PlateV, PlateWidth, PlateHeight, SheetWidth, SheetHeight);
        Component Plate = Variant.plate();
        Graphics.drawString(font, Plate, Left + PlateX + (PlateWidth - font.width(Plate)) / 2, Top + PlateY + 4, PlateColour, false);
        int Visible = visibleChars(PartialTick);
        for (int Row = 0; Row < Lines.size() && Visible > 0; Row++) {
            String Line = Lines.get(Row);
            String Part = Line.substring(0, Math.min(Line.length(), Visible));
            Graphics.drawString(font, Part, Left + TextX, Top + TextY + Row * LinePitch, TextColour, false);
            Visible -= Line.length();
        }
        int PipsLeft = Left + BoxWidth - 22 - Pages * (PipSize + 2);
        for (int Index = 0; Index < Pages; Index++) {
            Graphics.blit(Sheet, PipsLeft + Index * (PipSize + 2), Top + BoxHeight - 13, PipU + (Index == Page ? 0 : PipSize + 1), PipV, PipSize, PipSize, SheetWidth, SheetHeight);
        }
        if (pageDone()) {
            boolean Last = Page >= Pages - 1;
            Graphics.blit(Sheet, Left + BoxWidth - 18, Top + BoxHeight - 14, Last ? EndU : ArrowU, ArrowV, ArrowWidth, ArrowHeight, SheetWidth, SheetHeight);
        }
    }
}
