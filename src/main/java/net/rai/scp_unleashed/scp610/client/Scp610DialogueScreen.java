package net.rai.scp_unleashed.scp610.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp610.Scp610Sounds;
import net.rai.scp_unleashed.scp610.network.Scp610ReplyPayload;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scp610DialogueScreen extends Screen {
    public static final ResourceLocation PanelTexture = texture("dialogue_panel");
    public static final ResourceLocation VeinTexture = texture("dialogue_veins");
    public static final ResourceLocation PortraitTexture = texture("dialogue_portrait");
    public static final ResourceLocation ButtonTexture = texture("dialogue_button");
    public static final int PanelSize = 64;
    public static final int Corner = 16;
    public static final int PanelMax = 440;
    public static final int ScreenMargin = 8;
    public static final int Inset = 14;
    public static final int PortraitSize = 56;
    public static final int PortraitFrames = 8;
    public static final int PortraitRows = 4;
    public static final int PortraitGap = 8;
    public static final int LabelPitch = 13;
    public static final int LinePitch = 10;
    public static final int ParagraphGap = 4;
    public static final int ChoiceGap = 6;
    public static final int ButtonWidth = 96;
    public static final int ButtonHeight = 18;
    public static final int ButtonCap = 8;
    public static final int ButtonPitch = 20;
    public static final int ButtonText = 18;
    public static final int BottomGap = 10;
    public static final int TextColour = 0xFFDED0BC;
    public static final int TermsColour = 0xFFD99A88;
    public static final int LabelColour = 0xFFC46860;
    public static final int OptionColour = 0xFFC4B6A0;
    public static final int HoverColour = 0xFFF0E4D2;
    public static final int NumberColour = 0xFF8C5A52;
    public static final int ShadeTop = 0x30100406;
    public static final int ShadeBottom = 0xB0100406;
    public static final float CharsPerSecond = 42.0f;
    public static final float CommaPause = 0.12f;
    public static final float StopPause = 0.28f;
    public static final int TickEvery = 3;
    public static final long FrameMillis = 170L;
    public static final long BlinkEvery = 4200L;
    public static final int[] BlinkFrames = {6, 7, 7, 6};
    private final BlockPos Pos;
    private final List<String> Rows = new ArrayList<>();
    private final List<Integer> RowColours = new ArrayList<>();
    private final List<Integer> RowGaps = new ArrayList<>();
    private String Mood;
    private List<String> Lines;
    private List<String> Options;
    private int Total;
    private int Shown;
    private float Clock;
    private long LastFrame;
    private long Opened;
    private boolean Greeted;

    public Scp610DialogueScreen(BlockPos Pos, String Mood, List<String> Lines, List<String> Options) {
        super(Component.translatable("gui.scp_unleashed.scp610.dialogue"));
        this.Pos = Pos.immutable();
        this.Mood = Mood;
        this.Lines = List.copyOf(Lines);
        this.Options = List.copyOf(Options);
    }

    private static ResourceLocation texture(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp610/" + Name + ".png");
    }

    public boolean at(BlockPos Where) {
        return Pos.equals(Where);
    }

    public void continueWith(String NewMood, List<String> NewLines, List<String> NewOptions) {
        Mood = NewMood;
        Lines = List.copyOf(NewLines);
        Options = List.copyOf(NewOptions);
        Shown = 0;
        Clock = 0.0f;
        LastFrame = Util.getMillis();
        layout();
    }

    @Override
    protected void init() {
        layout();
        LastFrame = Util.getMillis();
        if (!Greeted) {
            Greeted = true;
            Opened = LastFrame;
            play(Scp610Sounds.DialogueOpen.get(), 1.0f, 0.6f);
        }
    }

    public static Component text(String Line) {
        String[] Parts = Line.split("\\|");
        Object[] Args = Arrays.copyOfRange(Parts, 1, Parts.length);
        return Component.translatable(Parts[0], Args);
    }

    public int panelWidth() {
        return Math.min(PanelMax, width - ScreenMargin * 2);
    }

    public int textWidth() {
        return panelWidth() - Inset * 2 - PortraitSize - PortraitGap;
    }

    private void layout() {
        Rows.clear();
        RowColours.clear();
        RowGaps.clear();
        Total = 0;
        int Wrap = textWidth();
        for (int Index = 0; Index < Lines.size(); Index++) {
            boolean FirstRow = true;
            for (FormattedText Part : font.getSplitter().splitLines(text(Lines.get(Index)), Wrap, Style.EMPTY)) {
                String Row = Part.getString();
                Rows.add(Row);
                RowColours.add(Index == 0 ? TextColour : TermsColour);
                RowGaps.add(FirstRow && Index > 0 ? ParagraphGap : 0);
                Total += Row.length();
                FirstRow = false;
            }
        }
        Shown = Math.min(Shown, Total);
    }

    private int textHeight() {
        int Height = LabelPitch;
        for (int Index = 0; Index < Rows.size(); Index++) {
            Height += LinePitch + RowGaps.get(Index);
        }
        return Height;
    }

    private int panelHeight() {
        int Body = textHeight() + ChoiceGap + Options.size() * ButtonPitch - (ButtonPitch - ButtonHeight);
        return Math.max(PortraitSize, Body) + Inset * 2;
    }

    private int left() {
        return (width - panelWidth()) / 2;
    }

    private int top() {
        return Math.max(ScreenMargin, height - panelHeight() - BottomGap);
    }

    private int textLeft() {
        return left() + Inset + PortraitSize + PortraitGap;
    }

    private int choicesTop() {
        return top() + Inset + textHeight() + ChoiceGap;
    }

    private boolean typing() {
        return Shown < Total;
    }

    private void advance() {
        long Now = Util.getMillis();
        float Elapsed = Math.min(0.25f, (Now - LastFrame) / 1000.0f);
        LastFrame = Now;
        if (!typing()) {
            return;
        }
        Clock += Elapsed;
        String Flat = String.join("", Rows);
        while (Shown < Total) {
            char Next = Flat.charAt(Shown);
            float Cost = 1.0f / CharsPerSecond + (Next == '.' || Next == '?' || Next == '!' ? StopPause : Next == ',' ? CommaPause : 0.0f);
            if (Clock < Cost) {
                break;
            }
            Clock -= Cost;
            Shown++;
            if (!Character.isWhitespace(Next) && Shown % TickEvery == 0) {
                play(Scp610Sounds.DialogueTick.get(), 0.9f + (Shown * 37 % 7) * 0.05f, 0.35f);
            }
        }
    }

    private static void play(SoundEvent Sound, float Pitch, float Volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, Volume));
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        Graphics.fillGradient(0, 0, width, height, ShadeTop, ShadeBottom);
    }

    public int moodRow() {
        return switch (Mood) {
            case "due" -> 1;
            case "broken" -> 2;
            case "hurt" -> 3;
            default -> 0;
        };
    }

    public float beatsPerMinute() {
        return switch (moodRow()) {
            case 1 -> 78.0f;
            case 2 -> 96.0f;
            case 3 -> 120.0f;
            default -> 48.0f;
        };
    }

    public float pulse(long Now) {
        float Beat = (Now * beatsPerMinute() / 60000.0f) % 1.0f;
        float Lub = Math.max(0.0f, 1.0f - Math.abs(Beat - 0.08f) / 0.08f);
        float Dub = Math.max(0.0f, 1.0f - Math.abs(Beat - 0.28f) / 0.1f) * 0.6f;
        return Math.max(Lub, Dub);
    }

    public int portraitFrame(long Now) {
        long Since = Now - Opened;
        int Blink = (int) (Since % BlinkEvery / 60L);
        if (Since > BlinkEvery && Blink < BlinkFrames.length) {
            return BlinkFrames[Blink];
        }
        long Speed = moodRow() == 3 ? FrameMillis / 2 : FrameMillis;
        return (int) (Since / Speed % 6L);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        advance();
        renderBackground(Graphics, MouseX, MouseY, Partial);
        long Now = Util.getMillis();
        int Left = left();
        int Top = top();
        int Wide = panelWidth();
        int Tall = panelHeight();
        blend();
        nineSlice(Graphics, PanelTexture, Left, Top, Wide, Tall);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 0.3f + 0.6f * pulse(Now));
        nineSlice(Graphics, VeinTexture, Left, Top, Wide, Tall);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        Graphics.blit(PortraitTexture, Left + Inset, Top + Inset, portraitFrame(Now) * PortraitSize, moodRow() * PortraitSize, PortraitSize, PortraitSize,
                PortraitSize * PortraitFrames, PortraitSize * PortraitRows);
        int X = textLeft();
        int Y = Top + Inset;
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.scp610.dialogue.speaker"), X, Y, LabelColour, false);
        Y += LabelPitch;
        int Budget = Shown;
        for (int Index = 0; Index < Rows.size(); Index++) {
            Y += RowGaps.get(Index);
            String Row = Rows.get(Index);
            String Cut = Row.substring(0, Mth.clamp(Budget, 0, Row.length()));
            Budget -= Row.length();
            if (!Cut.isEmpty()) {
                Graphics.drawString(font, Cut, X, Y, RowColours.get(Index), false);
            }
            Y += LinePitch;
        }
        if (typing()) {
            return;
        }
        int ChoiceY = choicesTop();
        int ChoiceWidth = textWidth();
        for (int Index = 0; Index < Options.size(); Index++) {
            int ButtonY = ChoiceY + Index * ButtonPitch;
            boolean Over = MouseX >= X && MouseX < X + ChoiceWidth && MouseY >= ButtonY && MouseY < ButtonY + ButtonHeight;
            blend();
            button(Graphics, X, ButtonY, ChoiceWidth, Over);
            Graphics.drawString(font, Integer.toString(Index + 1), X + 7, ButtonY + 5, Over ? HoverColour : NumberColour, false);
            Component Label = Component.translatable("dialogue.scp_unleashed.scp610.option." + Options.get(Index));
            Graphics.drawString(font, Label, X + ButtonText + (Over ? 1 : 0), ButtonY + 5, Over ? HoverColour : OptionColour, false);
        }
    }

    private static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private void nineSlice(GuiGraphics Graphics, ResourceLocation Texture, int X, int Y, int Wide, int Tall) {
        int Middle = PanelSize - Corner * 2;
        int InnerWide = Wide - Corner * 2;
        int InnerTall = Tall - Corner * 2;
        piece(Graphics, Texture, X, Y, 0, 0, Corner, Corner);
        piece(Graphics, Texture, X + Wide - Corner, Y, PanelSize - Corner, 0, Corner, Corner);
        piece(Graphics, Texture, X, Y + Tall - Corner, 0, PanelSize - Corner, Corner, Corner);
        piece(Graphics, Texture, X + Wide - Corner, Y + Tall - Corner, PanelSize - Corner, PanelSize - Corner, Corner, Corner);
        for (int Dx = 0; Dx < InnerWide; Dx += Middle) {
            int Span = Math.min(Middle, InnerWide - Dx);
            piece(Graphics, Texture, X + Corner + Dx, Y, Corner, 0, Span, Corner);
            piece(Graphics, Texture, X + Corner + Dx, Y + Tall - Corner, Corner, PanelSize - Corner, Span, Corner);
        }
        for (int Dy = 0; Dy < InnerTall; Dy += Middle) {
            int Span = Math.min(Middle, InnerTall - Dy);
            piece(Graphics, Texture, X, Y + Corner + Dy, 0, Corner, Corner, Span);
            piece(Graphics, Texture, X + Wide - Corner, Y + Corner + Dy, PanelSize - Corner, Corner, Corner, Span);
            for (int Dx = 0; Dx < InnerWide; Dx += Middle) {
                piece(Graphics, Texture, X + Corner + Dx, Y + Corner + Dy, Corner, Corner, Math.min(Middle, InnerWide - Dx), Span);
            }
        }
    }

    private static void piece(GuiGraphics Graphics, ResourceLocation Texture, int X, int Y, int U, int V, int Wide, int Tall) {
        if (Wide > 0 && Tall > 0) {
            Graphics.blit(Texture, X, Y, U, V, Wide, Tall, PanelSize, PanelSize);
        }
    }

    private static void button(GuiGraphics Graphics, int X, int Y, int Wide, boolean Over) {
        int V = Over ? ButtonHeight : 0;
        int Middle = ButtonWidth - ButtonCap * 2;
        int Inner = Wide - ButtonCap * 2;
        Graphics.blit(ButtonTexture, X, Y, 0, V, ButtonCap, ButtonHeight, ButtonWidth, ButtonHeight * 2);
        for (int Dx = 0; Dx < Inner; Dx += Middle) {
            Graphics.blit(ButtonTexture, X + ButtonCap + Dx, Y, ButtonCap, V, Math.min(Middle, Inner - Dx), ButtonHeight, ButtonWidth, ButtonHeight * 2);
        }
        Graphics.blit(ButtonTexture, X + Wide - ButtonCap, Y, ButtonWidth - ButtonCap, V, ButtonCap, ButtonHeight, ButtonWidth, ButtonHeight * 2);
    }

    private void skip() {
        Shown = Total;
        Clock = 0.0f;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (typing()) {
            skip();
            return true;
        }
        int X = textLeft();
        int ChoiceY = choicesTop();
        for (int Index = 0; Index < Options.size(); Index++) {
            int ButtonY = ChoiceY + Index * ButtonPitch;
            if (MouseX >= X && MouseX < X + textWidth() && MouseY >= ButtonY && MouseY < ButtonY + ButtonHeight) {
                choose(Options.get(Index));
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean keyPressed(int Key, int Scan, int Modifiers) {
        if (typing() && (Key == GLFW.GLFW_KEY_SPACE || Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER)) {
            skip();
            return true;
        }
        int Index = Key - GLFW.GLFW_KEY_1;
        if (!typing() && Index >= 0 && Index < Math.min(9, Options.size())) {
            choose(Options.get(Index));
            return true;
        }
        return super.keyPressed(Key, Scan, Modifiers);
    }

    private void choose(String Option) {
        play(Scp610Sounds.DialogueChoose.get(), 1.0f, 0.5f);
        PacketDistributor.sendToServer(new Scp610ReplyPayload(Pos, Option));
        if ("leave".equals(Option)) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
