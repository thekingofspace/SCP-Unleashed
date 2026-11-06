package net.rai.scp_unleashed.scp079.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp079.Scp079BlockEntity;
import net.rai.scp_unleashed.scp079.Scp079Mood;
import net.rai.scp_unleashed.scp079.Scp079Prompt;
import net.rai.scp_unleashed.scp079.Scp079Registry;
import net.rai.scp_unleashed.scp079.network.Scp079AskPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Scp079TerminalScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp079/terminal.png");
    public static final int TextureSize = 256;
    public static final int PanelWidth = 236;
    public static final int PanelHeight = 198;
    public static final int TitleX = 10;
    public static final int TitleY = 7;
    public static final int DisplayX = 8;
    public static final int DisplayY = 20;
    public static final int DisplayWidth = 220;
    public static final int DisplayHeight = 104;
    public static final int TextX = 15;
    public static final int TextY = 26;
    public static final int TextWidth = 152;
    public static final int VisibleLines = 9;
    public static final int LineHeight = 10;
    public static final int FaceBoxX = 175;
    public static final int FaceBoxY = 26;
    public static final int FaceBoxWidth = 46;
    public static final int FaceBoxHeight = 37;
    public static final int FaceScale = 3;
    public static final int StatusX = 176;
    public static final int StatusY = 70;
    public static final int ButtonX = 9;
    public static final int ButtonY = 130;
    public static final int ButtonWidth = 108;
    public static final int ButtonHeight = 18;
    public static final int ButtonGapX = 4;
    public static final int ButtonGapY = 4;
    public static final int ButtonColumns = 2;
    public static final int SpriteY = 200;
    public static final int SecondSpriteY = 220;
    public static final int CharsPerTick = 2;
    public static final int RushedCharsPerTick = 5;
    public static final int PressTicks = 6;
    public static final double CloseReach = 8.0;
    private static final int[] KeySprite = {0, SpriteY, ButtonWidth, ButtonHeight};
    private static final int[] KeyHover = {110, SpriteY, ButtonWidth, ButtonHeight};
    private static final int[] KeyPressed = {0, SecondSpriteY, ButtonWidth, ButtonHeight};
    private static final int[] KeyDisabled = {110, SecondSpriteY, ButtonWidth, ButtonHeight};
    private static final int VoiceColour = 0xFFE6EAE4;
    private static final int EchoColour = 0xFF8A938C;
    private static final int SystemColour = 0xFFB4BCB4;
    private static final int TitleColour = 0xFFD9CFB4;
    private static final int StatusColour = 0xFF9AA39C;
    private static final int ModuleColour = 0xFFC8B4F0;
    private static final int KeyInk = 0xFF2C271F;
    private static final int KeyInkOff = 0xFF6F695E;

    private final BlockPos Pos;
    private final List<Entry> Log = new ArrayList<>();
    private final RandomSource Random = RandomSource.create();
    private boolean Upgraded;
    private Scp079Mood LastMood;
    private int Left;
    private int Top;
    private int Ticks;
    private int Active;
    private int Revealed;
    private int Typed;
    private int PressedIndex = -1;
    private int PressedUntil = -1;

    public Scp079TerminalScreen(BlockPos Pos, boolean Upgraded, Scp079Mood Mood) {
        super(Component.translatable("gui.scp_unleashed.scp079.title"));
        this.Pos = Pos.immutable();
        this.Upgraded = Upgraded;
        this.LastMood = Mood;
    }

    public boolean isFor(BlockPos Other) {
        return Pos.equals(Other);
    }

    public void receive(List<Component> Lines, Scp079Mood Mood, boolean NowUpgraded) {
        Upgraded = NowUpgraded;
        LastMood = Mood;
        for (Component Line : Lines) {
            int Colour = VoiceColour;
            if (Line.getContents() instanceof TranslatableContents Translated) {
                if (Translated.getKey().endsWith(".echo")) {
                    Colour = EchoColour;
                } else if (Translated.getKey().endsWith(".link")) {
                    Colour = SystemColour;
                }
            }
            String Text = Line.getString().toUpperCase(Locale.ROOT);
            for (FormattedText Wrapped : font.getSplitter().splitLines(Text, TextWidth, Style.EMPTY)) {
                Log.add(new Entry(Wrapped.getString(), Colour));
            }
        }
        while (Log.size() > 64 && Active > 0) {
            Log.removeFirst();
            Active--;
        }
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - PanelWidth) / 2;
        Top = (height - PanelHeight) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isTyping() {
        return Active < Log.size();
    }

    @Override
    public void tick() {
        super.tick();
        Ticks++;
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.level == null || !(Client.level.getBlockEntity(Pos) instanceof Scp079BlockEntity Computer) || !Computer.isPowered()
                || Client.player.distanceToSqr(Computer.screenCentre()) > CloseReach * CloseReach) {
            onClose();
            return;
        }
        LastMood = Computer.getShownMood().isFace() ? Computer.getShownMood() : LastMood;
        if (!isTyping()) {
            return;
        }
        int Budget = Log.size() - Active > 3 ? RushedCharsPerTick : CharsPerTick;
        while (Budget > 0 && Active < Log.size()) {
            Entry Current = Log.get(Active);
            int Step = Math.min(Budget, Current.Text().length() - Revealed);
            Revealed += Step;
            Budget -= Step;
            Typed += Step;
            if (Revealed >= Current.Text().length()) {
                Active++;
                Revealed = 0;
            }
        }
        if (Typed >= 2) {
            Typed = 0;
            Client.getSoundManager().play(SimpleSoundInstance.forUI(Scp079Registry.Key.get(), 0.9f + Random.nextFloat() * 0.25f, 0.35f));
        }
    }

    private int buttonX(int Index) {
        return Left + ButtonX + (Index % ButtonColumns) * (ButtonWidth + ButtonGapX);
    }

    private int buttonY(int Index) {
        return Top + ButtonY + (Index / ButtonColumns) * (ButtonHeight + ButtonGapY);
    }

    private boolean available(Scp079Prompt Prompt) {
        return !Prompt.isUpgradedOnly() || Upgraded;
    }

    private int hovered(double MouseX, double MouseY) {
        Scp079Prompt[] All = Scp079Prompt.values();
        for (int Index = 0; Index < All.length; Index++) {
            int X = buttonX(Index);
            int Y = buttonY(Index);
            if (MouseX >= X && MouseX < X + ButtonWidth && MouseY >= Y && MouseY < Y + ButtonHeight) {
                return Index;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int MouseButton) {
        int Index = hovered(MouseX, MouseY);
        if (MouseButton == 0 && Index >= 0) {
            Scp079Prompt Prompt = Scp079Prompt.byIndex(Index);
            if (available(Prompt) && !isTyping()) {
                PressedIndex = Index;
                PressedUntil = Ticks + PressTicks;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Scp079Registry.Key.get(), 0.7f, 0.6f));
                PacketDistributor.sendToServer(new Scp079AskPayload(Pos, Prompt.ordinal()));
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, MouseButton);
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        renderTransparentBackground(Graphics);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight, TextureSize, TextureSize);
        Graphics.drawString(font, title, Left + TitleX, Top + TitleY, TitleColour, false);
        drawFace(Graphics);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.scp079.online"), Left + StatusX, Top + StatusY, StatusColour, false);
        if (Upgraded) {
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.scp079.module"), Left + StatusX, Top + StatusY + LineHeight, ModuleColour, false);
        }
        drawLog(Graphics);
        drawButtons(Graphics, MouseX, MouseY);
    }

    private void drawFace(GuiGraphics Graphics) {
        Scp079Mood Mood = LastMood.isFace() ? LastMood : Scp079Mood.Idle;
        int Frame = Scp079Faces.frame(Mood, Ticks, Pos.hashCode() & 0xFF, Scp079Faces.LookCentre);
        int X = Left + FaceBoxX + (FaceBoxWidth - Scp079Faces.FaceWidth * FaceScale) / 2;
        int Y = Top + FaceBoxY + (FaceBoxHeight - Scp079Faces.FaceHeight * FaceScale) / 2;
        Graphics.blit(Scp079Faces.Texture, X, Y, Scp079Faces.FaceWidth * FaceScale, Scp079Faces.FaceHeight * FaceScale, Frame * Scp079Faces.Cell,
                Mood.getRow() * Scp079Faces.Cell, Scp079Faces.FaceWidth, Scp079Faces.FaceHeight, Scp079Faces.TextureWidth, Scp079Faces.TextureHeight);
    }

    private void drawLog(GuiGraphics Graphics) {
        int Shown = Math.min(Log.size(), Active + 1);
        int From = Math.max(0, Shown - VisibleLines);
        int Y = Top + TextY;
        int CaretX = Left + TextX;
        int CaretY = Y;
        for (int Index = From; Index < Shown; Index++) {
            Entry Line = Log.get(Index);
            String Text = Index == Active ? Line.Text().substring(0, Math.min(Revealed, Line.Text().length())) : Line.Text();
            Graphics.drawString(font, Text, Left + TextX, Y, Line.Colour(), false);
            CaretX = Left + TextX + font.width(Text) + 1;
            CaretY = Y;
            Y += LineHeight;
        }
        if (!isTyping() && Shown > 0) {
            CaretX = Left + TextX;
            CaretY = Math.min(Y, Top + TextY + (VisibleLines - 1) * LineHeight);
            if (Shown - From >= VisibleLines) {
                CaretY = Top + TextY + (VisibleLines - 1) * LineHeight;
                CaretX = Left + TextX + font.width(Log.get(Shown - 1).Text()) + 3;
            }
        }
        if (isTyping() || (Ticks / 8) % 2 == 0) {
            Graphics.fill(CaretX, CaretY + 7, CaretX + 5, CaretY + 8, VoiceColour);
        }
    }

    private void drawButtons(GuiGraphics Graphics, int MouseX, int MouseY) {
        int Hover = hovered(MouseX, MouseY);
        Scp079Prompt[] All = Scp079Prompt.values();
        for (int Index = 0; Index < All.length; Index++) {
            Scp079Prompt Prompt = All[Index];
            boolean Usable = available(Prompt) && !isTyping();
            int[] Sprite = !Usable ? KeyDisabled : Index == PressedIndex && Ticks < PressedUntil ? KeyPressed : Index == Hover ? KeyHover : KeySprite;
            int X = buttonX(Index);
            int Y = buttonY(Index);
            Graphics.blit(Texture, X, Y, Sprite[0], Sprite[1], Sprite[2], Sprite[3], TextureSize, TextureSize);
            Component Label = available(Prompt) ? Prompt.getLabel() : Component.translatable("gui.scp_unleashed.scp079.prompt.locked");
            int Sink = Sprite == KeyPressed ? 1 : 0;
            int LabelWidth = font.width(Label);
            Graphics.drawString(font, Label, X + (ButtonWidth - LabelWidth) / 2, Y + 5 + Sink, Usable ? KeyInk : KeyInkOff, false);
        }
    }

    private record Entry(String Text, int Colour) {
    }
}
