package net.rai.scp_unleashed.scp140.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp140.Scp140Registry;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;
import net.rai.scp_unleashed.scp140.network.Scp140ClosedPayload;
import net.rai.scp_unleashed.scp140.network.Scp140SeenPayload;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scp140BookScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp140/book.png");
    public static final ResourceLocation Initials = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp140/initials.png");
    public static final int TextureWidth = 512;
    public static final int TextureHeight = 256;
    public static final int InitialsWidth = 260;
    public static final int InitialsHeight = 40;
    public static final int InitialColumns = 13;
    public static final int ArrowWidth = 18;
    public static final int ArrowHeight = 11;
    public static final int ArrowY = 177;
    public static final int PrevArrowX = 16;
    public static final int NextArrowX = Scp140PageLayout.PanelWidth - 16 - ArrowWidth;
    public static final int NextSpriteX = 324;
    public static final int NextHoverSpriteX = 344;
    public static final int NextSpriteY = 0;
    public static final int PrevSpriteY = 14;
    public static final int RuleU = 324;
    public static final int RuleV = 30;
    public static final int RuleWidth = 54;
    public static final int RuleHeight = 5;
    public static final int NumberY = 178;
    public static final float SeepSpread = 1.7f;
    public static final float SeepDuration = 0.8f;
    public static final float BleedRest = 0.24f;
    public static final float BleedWet = 0.65f;
    public static final float MinAlpha = 0.02f;
    public static final int DipMinimum = 9;
    public static final int DipRange = 8;
    public static final float DipDepth = 0.85f;
    public static final int InkDark = 0x560A10;
    public static final int InkLight = 0x8C1E1C;
    public static final int InkFresh = 0xCC2E26;
    public static final int BleedColour = 0xAA5440;
    public static final int TitleInk = 0x4A080E;
    public static final int FooterInk = 0x705434;
    public static final int GhostColour = 0xB05646;
    public static final long QuillGap = 380L;
    private static final int[][] Blots = {{324, 40, 4, 3}, {330, 40, 3, 3}, {335, 40, 4, 2}, {341, 40, 3, 2}};

    private final Map<Integer, Long> SeepStarted = new HashMap<>();
    private Scp140Script Script;
    private int Page;
    private int Left;
    private int Top;
    private boolean Greeted;
    private long LastQuill;
    private int SpreadPage = -1;
    private int SpreadRevision = -1;
    private Scp140PageLayout.Spread Spread;
    private List<Glyph> TitleGlyphs = List.of();
    private List<Glyph> BodyGlyphs = List.of();
    private int[] BodyColours = new int[0];

    public Scp140BookScreen() {
        super(Component.translatable("gui.scp_unleashed.scp140.title"));
        Page = firstPage();
    }

    public static int firstPage() {
        int Written = Scp140ChronicleView.written();
        for (int Candidate = 1; Candidate <= Written; Candidate++) {
            if (!Scp140ChronicleView.wasSeen(Candidate)) {
                return Candidate;
            }
        }
        return Math.max(1, Math.min(Scp140Pages.Total, Written));
    }

    @Override
    protected void init() {
        super.init();
        Script = new Scp140Script(font);
        Left = (width - Scp140PageLayout.PanelWidth) / 2;
        Top = (height - Scp140PageLayout.PanelHeight) / 2;
        SpreadPage = -1;
        if (!Greeted) {
            Greeted = true;
            sound(Scp140Registry.OpenLayers, 1.0f, 0.8f);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(Scp140ClosedPayload.Instance);
        sound(Scp140Registry.CloseLayers, 1.0f, 0.7f);
        super.onClose();
    }

    private static void sound(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch, float Volume) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Layer.get(), Pitch, Volume));
        }
    }

    private void turn(int Target) {
        int Wanted = Mth.clamp(Target, 1, Scp140Pages.Total);
        if (Wanted != Page) {
            Page = Wanted;
            sound(Scp140Registry.TurnLayers, 0.92f + (float) Math.random() * 0.16f, 0.7f);
        }
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        switch (Key) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_A -> {
                turn(Page - 1);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN, GLFW.GLFW_KEY_D -> {
                turn(Page + 1);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                turn(1);
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                turn(Math.max(1, Scp140ChronicleView.written()));
                return true;
            }
            default -> {
                return super.keyPressed(Key, ScanCode, Modifiers);
            }
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0) {
            if (Page > 1 && over(MouseX, MouseY, PrevArrowX, ArrowY, ArrowWidth, ArrowHeight)) {
                turn(Page - 1);
                return true;
            }
            if (Page < Scp140Pages.Total && over(MouseX, MouseY, NextArrowX, ArrowY, ArrowWidth, ArrowHeight)) {
                turn(Page + 1);
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        if (ScrollY > 0.0) {
            turn(Page - 1);
            return true;
        }
        if (ScrollY < 0.0) {
            turn(Page + 1);
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    private boolean over(double MouseX, double MouseY, int X, int Y, int Width, int Height) {
        return MouseX >= Left + X && MouseX < Left + X + Width && MouseY >= Top + Y && MouseY < Top + Y + Height;
    }

    @Override
    public void tick() {
        super.tick();
        Long Started = SeepStarted.get(Page);
        if (Started == null) {
            return;
        }
        long Now = Util.getMillis();
        float Seconds = (Now - Started) / 1000.0f;
        if (Seconds < SeepSpread + SeepDuration * 0.6f && Now - LastQuill >= QuillGap) {
            LastQuill = Now;
            sound(Scp140Registry.QuillLayers, 0.9f + (float) Math.random() * 0.25f, 0.35f);
        }
    }

    private static String title(int Number) {
        return Scp140Script.clean(Component.translatable("gui.scp_unleashed.scp140.page." + Number + ".title").getString());
    }

    private static String body(int Number) {
        return Scp140Script.clean(Component.translatable("gui.scp_unleashed.scp140.page." + Number + ".text").getString());
    }

    private void ensureSpread() {
        int Revision = Scp140ChronicleView.revision();
        if (SpreadPage == Page && SpreadRevision == Revision) {
            return;
        }
        SpreadPage = Page;
        SpreadRevision = Revision;
        boolean Written = Scp140ChronicleView.isWritten(Page);
        boolean Ghost = !Written && Page == Scp140ChronicleView.written() + 1;
        if (!Written && !Ghost) {
            Spread = null;
            TitleGlyphs = List.of();
            BodyGlyphs = List.of();
            BodyColours = new int[0];
            return;
        }
        String Remains = Written ? Scp140Remains.line(Page, Scp140ChronicleView.page(Page)) : "";
        Spread = Scp140PageLayout.layout(Script, title(Page), body(Page), Remains);
        TitleGlyphs = glyphs(Spread.Titles());
        List<Scp140PageLayout.Line> Lines = new ArrayList<>(Spread.Body());
        Lines.addAll(Spread.Remains());
        BodyGlyphs = glyphs(Lines);
        BodyColours = new int[BodyGlyphs.size()];
        for (int Index = 0; Index < BodyColours.length; Index++) {
            BodyColours[Index] = dipColour(Page, Index);
        }
    }

    private List<Glyph> glyphs(List<Scp140PageLayout.Line> Lines) {
        List<Glyph> Found = new ArrayList<>();
        for (Scp140PageLayout.Line Entry : Lines) {
            int Cursor = Entry.X();
            String Text = Entry.Text();
            for (int Index = 0; Index < Text.length(); Index++) {
                char Character = Text.charAt(Index);
                Found.add(new Glyph(Character, Cursor, Entry.Y()));
                Cursor += Script.advance(Character);
            }
        }
        return Found;
    }

    public static int hash(int First, int Second) {
        long Value = ((First * 73856093L) ^ (Second * 19349663L) ^ 0x5BD1E995L) & 0xFFFFFFFFL;
        Value = (((Value >>> 15) ^ Value) * 0x2C1B3C6DL) & 0xFFFFFFFFL;
        Value = (((Value >>> 12) ^ Value) * 0x297A2D39L) & 0xFFFFFFFFL;
        return (int) (((Value >>> 15) ^ Value) & 0x7FFFFFFFL);
    }

    public static int mix(int From, int To, float Amount) {
        float Clamped = Mth.clamp(Amount, 0.0f, 1.0f);
        int Red = Math.round(Mth.lerp(Clamped, From >> 16 & 255, To >> 16 & 255));
        int Green = Math.round(Mth.lerp(Clamped, From >> 8 & 255, To >> 8 & 255));
        int Blue = Math.round(Mth.lerp(Clamped, From & 255, To & 255));
        return Red << 16 | Green << 8 | Blue;
    }

    public static int dipColour(int Number, int Index) {
        int Start = 0;
        int Dip = 0;
        while (true) {
            int Length = DipMinimum + hash(Number, Dip) % DipRange;
            if (Index < Start + Length) {
                float Fraction = (float) (Index - Start) / Length;
                return mix(InkDark, InkLight, (float) Math.pow(Fraction, 1.4) * DipDepth);
            }
            Start += Length;
            Dip++;
        }
    }

    private static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    private static float[] seep(int Order, int Count, float Seconds, int Number) {
        float Jitter = (hash(Number, Order + 977) % 1000) / 1000.0f;
        float Start = (float) Order / Math.max(1, Count) * SeepSpread + Jitter * 0.18f;
        float Local = (Seconds - Start) / SeepDuration;
        if (Local <= 0.0f) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        float Core = smooth((Local - 0.08f) / 0.5f);
        float Fresh = 1.0f - smooth((Local - 0.2f) / 0.8f);
        float Bleed = Local < 1.0f ? BleedRest + (BleedWet - BleedRest) * (smooth(Local / 0.3f) - smooth((Local - 0.3f) / 0.7f)) : BleedRest;
        return new float[]{Core, Fresh, Bleed};
    }

    private static int argb(int Rgb, float Alpha) {
        return Mth.clamp(Math.round(Alpha * 255.0f), 0, 255) << 24 | Rgb & 0xFFFFFF;
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        renderTransparentBackground(Graphics);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        ensureSpread();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(Texture, Left, Top, 0, 0, Scp140PageLayout.PanelWidth, Scp140PageLayout.PanelHeight, TextureWidth, TextureHeight);
        boolean Written = Scp140ChronicleView.isWritten(Page);
        if (Written) {
            drawWritten(Graphics);
        } else if (Spread != null) {
            drawGhost(Graphics);
        }
        drawArrows(Graphics, MouseX, MouseY);
        drawFooter(Graphics, Written);
        if (overNumber(MouseX, MouseY)) {
            Graphics.renderTooltip(font, Component.translatable("gui.scp_unleashed.scp140.page_of", Page, Scp140Pages.Total), MouseX, MouseY);
        }
    }

    private float seconds() {
        Long Started = SeepStarted.get(Page);
        if (Started == null) {
            if (!Scp140ChronicleView.wasSeen(Page)) {
                long Now = Util.getMillis();
                SeepStarted.put(Page, Now);
                Scp140ChronicleView.markSeen(Page);
                PacketDistributor.sendToServer(new Scp140SeenPayload(Page));
                sound(Scp140Registry.SeepLayers, 0.9f, 0.6f);
                LastQuill = Now;
                return 0.0f;
            }
            return 1000.0f;
        }
        return (Util.getMillis() - Started) / 1000.0f;
    }

    private void drawWritten(GuiGraphics Graphics) {
        if (Spread == null) {
            return;
        }
        float Seconds = seconds();
        int Count = TitleGlyphs.size() + BodyGlyphs.size() + 4;
        int[] Order = {0};
        float[] Mark = seep(TitleGlyphs.size(), Count, Seconds, Page);
        float Ornament = Mark[0];
        if (Ornament > MinAlpha) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Graphics.setColor(1.0f, 1.0f, 1.0f, Ornament);
            int RuleX = Scp140PageLayout.LeftX + (Scp140PageLayout.TextWidth - RuleWidth) / 2;
            Graphics.blit(Texture, Left + RuleX, Top + Spread.RuleY(), RuleU, RuleV, RuleWidth, RuleHeight, TextureWidth, TextureHeight);
            if (Spread.isIlluminated()) {
                int Index = Spread.Initial() - 'A';
                int U = Index % InitialColumns * Scp140PageLayout.InitialBox;
                int V = Index / InitialColumns * Scp140PageLayout.InitialBox;
                Graphics.blit(Initials, Left + Spread.InitialX(), Top + Spread.InitialY(), U, V, Scp140PageLayout.InitialBox, Scp140PageLayout.InitialBox, InitialsWidth, InitialsHeight);
            }
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        Graphics.drawManaged(() -> {
            for (Glyph Entry : TitleGlyphs) {
                float[] State = seep(Order[0], Count, Seconds, Page);
                Order[0]++;
                glyph(Graphics, Entry, mix(TitleInk, InkFresh, State[1]), State[0], State[2]);
            }
            Order[0] += 4;
            for (int Index = 0; Index < BodyGlyphs.size(); Index++) {
                float[] State = seep(Order[0], Count, Seconds, Page);
                Order[0]++;
                glyph(Graphics, BodyGlyphs.get(Index), mix(BodyColours[Index], InkFresh, State[1]), State[0], State[2]);
            }
        });
    }

    private void glyph(GuiGraphics Graphics, Glyph Entry, int Colour, float Core, float Bleed) {
        if (Entry.Character() == ' ') {
            return;
        }
        int X = Left + Entry.X();
        int Y = Top + Entry.Y();
        if (Bleed > MinAlpha) {
            Graphics.drawString(font, Scp140Script.bleed(Entry.Character()), X - 1, Y, argb(BleedColour, Bleed), false);
        }
        if (Core > MinAlpha) {
            Graphics.drawString(font, Scp140Script.ink(Entry.Character()), X, Y, argb(Colour, Core), false);
        }
    }

    private void drawGhost(GuiGraphics Graphics) {
        float Progress = Scp140ChronicleView.progress();
        int Shown = (int) (BodyGlyphs.size() * Progress);
        if (Shown <= 0) {
            return;
        }
        float Strength = 0.1f + 0.16f * Progress;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor((GhostColour >> 16 & 255) / 255.0f, (GhostColour >> 8 & 255) / 255.0f, (GhostColour & 255) / 255.0f, Strength);
        for (int Index = 0; Index < Shown; Index++) {
            Glyph Entry = BodyGlyphs.get(Index);
            if (Entry.Character() == ' ' || hash(Page, Index) % 3 == 0) {
                continue;
            }
            int[] Blot = Blots[hash(Page, Index + 31) % Blots.length];
            Graphics.blit(Texture, Left + Entry.X(), Top + Entry.Y() + 3, Blot[0], Blot[1], Blot[2], Blot[3], TextureWidth, TextureHeight);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawArrows(GuiGraphics Graphics, int MouseX, int MouseY) {
        if (Page > 1) {
            boolean Hover = over(MouseX, MouseY, PrevArrowX, ArrowY, ArrowWidth, ArrowHeight);
            Graphics.blit(Texture, Left + PrevArrowX, Top + ArrowY, Hover ? NextHoverSpriteX : NextSpriteX, PrevSpriteY, ArrowWidth, ArrowHeight, TextureWidth, TextureHeight);
        }
        if (Page < Scp140Pages.Total) {
            boolean Hover = over(MouseX, MouseY, NextArrowX, ArrowY, ArrowWidth, ArrowHeight);
            Graphics.blit(Texture, Left + NextArrowX, Top + ArrowY, Hover ? NextHoverSpriteX : NextSpriteX, NextSpriteY, ArrowWidth, ArrowHeight, TextureWidth, TextureHeight);
        }
    }

    public static String roman(int Number) {
        int[] Values = {10, 9, 5, 4, 1};
        String[] Letters = {"X", "IX", "V", "IV", "I"};
        StringBuilder Built = new StringBuilder();
        int Remaining = Number;
        for (int Index = 0; Index < Values.length; Index++) {
            while (Remaining >= Values[Index]) {
                Built.append(Letters[Index]);
                Remaining -= Values[Index];
            }
        }
        return Built.toString();
    }

    private int numberX() {
        return Scp140PageLayout.LeftX + (Scp140PageLayout.TextWidth - Script.width(roman(Page))) / 2;
    }

    private boolean overNumber(double MouseX, double MouseY) {
        return Script != null && over(MouseX, MouseY, numberX() - 2, NumberY - 1, Script.width(roman(Page)) + 4, 10);
    }

    private void drawFooter(GuiGraphics Graphics, boolean Written) {
        String Numeral = roman(Page);
        Graphics.drawManaged(() -> {
            plain(Graphics, Numeral, Left + numberX(), Top + NumberY);
            if (Written && Scp140ChronicleView.page(Page) != null && Scp140ChronicleView.page(Page).Day() > 0L) {
                String Day = Scp140Script.clean(Component.translatable("gui.scp_unleashed.scp140.written_day", Scp140ChronicleView.page(Page).Day()).getString());
                plain(Graphics, Day, Left + Scp140PageLayout.RightX + (Scp140PageLayout.TextWidth - Script.width(Day)) / 2, Top + NumberY);
            }
        });
    }

    private void plain(GuiGraphics Graphics, String Text, int X, int Y) {
        int Cursor = X;
        for (int Index = 0; Index < Text.length(); Index++) {
            char Character = Text.charAt(Index);
            if (Character != ' ') {
                Graphics.drawString(font, Scp140Script.ink(Character), Cursor, Y, argb(FooterInk, 1.0f), false);
            }
            Cursor += Script.advance(Character);
        }
    }

    private record Glyph(char Character, int X, int Y) {
    }
}
