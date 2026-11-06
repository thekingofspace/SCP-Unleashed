package net.rai.scp_unleashed.z0001.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0001.Z0001BookItem;
import net.rai.scp_unleashed.z0001.Z0001Ink;
import net.rai.scp_unleashed.z0001.Z0001Outcome;
import net.rai.scp_unleashed.z0001.Z0001Page;
import net.rai.scp_unleashed.z0001.Z0001Registry;
import net.rai.scp_unleashed.z0001.network.Z0001WritePayload;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Z0001BookScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/z0001/book.png");
    public static final ResourceLocation ScriptFont = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp140_script");
    public static final Style Script = Style.EMPTY.withFont(ScriptFont);
    public static final String Prefix = "gui.scp_unleashed.z0001_book.";
    public static final int TextureWidth = 512;
    public static final int TextureHeight = 256;
    public static final int PanelWidth = 284;
    public static final int PanelHeight = 184;
    public static final int RightPageX = 144;
    public static final int RightPageWidth = 130;
    public static final int TextLeft = 152;
    public static final int TextRight = 266;
    public static final int RuleTop = 30;
    public static final int RulePitch = 12;
    public static final int RuleCount = 12;
    public static final int NameRule = 5;
    public static final int HintRule = 8;
    public static final int ButtonRule = 11;
    public static final int GlyphRise = 8;
    public static final int QuillU = 300;
    public static final int QuillHoverU = 312;
    public static final int QuillV = 0;
    public static final int QuillSize = 11;
    public static final int BeadU = 300;
    public static final int BeadV = 14;
    public static final int BeadSize = 4;
    public static final int BeadCount = 4;
    public static final int CaretU = 300;
    public static final int CaretV = 20;
    public static final int CaretWidth = 2;
    public static final int CaretHeight = 9;
    public static final int ButtonGap = 2;
    public static final int DraftInk = 0x7A6E60;
    public static final int WetInk = 0x1E1A2C;
    public static final int DryInk = 0x110E12;
    public static final int BleedInk = 0x3C3044;
    public static final int FadeInk = 0x9A8E7A;
    public static final int HintInk = 0x5E5044;
    public static final int ButtonInk = 0x54483E;
    public static final int ButtonHoverInk = 0x15110F;
    public static final float DraftAlpha = 0.5f;
    public static final float BleedAlpha = 0.45f;
    public static final float DisabledAlpha = 0.32f;
    public static final long GlyphGap = 95L;
    public static final long GlyphInk = 170L;
    public static final long ScratchGap = 115L;
    public static final long SoakTime = 900L;
    public static final long FadeTime = 1600L;
    public static final long ReplyTimeout = 3000L;
    public static final long HintDelay = 1100L;
    public static final long HintFade = 450L;
    public static final long CaretBlink = 530L;

    private final InteractionHand Hand;
    private int Left;
    private int Top;
    private boolean Greeted;
    private Phase Stage = Phase.Blank;
    private String Text = "";
    private long WriteStart;
    private long OutcomeAt;
    private long LastScratch;
    private Z0001Outcome Result;
    private String ResultName = "";
    private Z0001Outcome Applied;
    private String Afterwards = "";
    private String HintKey;
    private long HintAt;

    public Z0001BookScreen(InteractionHand Hand) {
        super(Component.translatable(Prefix + "title"));
        this.Hand = Hand;
    }

    private enum Phase {
        Blank,
        Writing,
        Settled,
        Fading
    }

    public static int ruleY(int Index) {
        return RuleTop + Index * RulePitch;
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - PanelWidth) / 2;
        Top = (height - PanelHeight) / 2;
        if (!Greeted) {
            Greeted = true;
            sound(Z0001Registry.OpenLayers, 1.0f, 0.75f);
            sound(Z0001Registry.TurnLayers, 0.95f, 0.5f);
            Z0001Page Page = page();
            if (Page != null && spent()) {
                settle(Page.Name(), false);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        sound(Z0001Registry.CloseLayers, 1.0f, 0.7f);
        super.onClose();
    }

    private ItemStack held() {
        LocalPlayer User = Minecraft.getInstance().player;
        return User == null ? ItemStack.EMPTY : User.getItemInHand(Hand);
    }

    private Z0001Page page() {
        ItemStack Stack = held();
        return Z0001BookItem.is(Stack) ? Stack.get(Z0001Registry.Page.get()) : null;
    }

    private boolean spent() {
        Minecraft Game = Minecraft.getInstance();
        return Game.level != null && Z0001Ink.isSpent(held(), Z0001Ink.today(Game.level));
    }

    private static void sound(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch, float Volume) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Layer.get(), Pitch, Volume));
        }
    }

    private void settle(String Name, boolean Fresh) {
        Stage = Phase.Settled;
        Text = Name;
        Applied = Z0001Outcome.Taken;
        OutcomeAt = Fresh ? Util.getMillis() : Util.getMillis() - SoakTime;
        hint(Z0001Outcome.Spent.hintKey(), Fresh ? HintDelay : 0L);
    }

    private void hint(String Key, long Delay) {
        HintKey = Key;
        HintAt = Util.getMillis() + Delay;
    }

    private long writtenAt() {
        return WriteStart + Math.max(0, Text.length() - 1) * GlyphGap + GlyphInk;
    }

    public void accept(Z0001Outcome Outcome, String Name) {
        if (Stage == Phase.Writing && Result == null) {
            Result = Outcome;
            ResultName = Name;
        }
    }

    private void apply(Z0001Outcome Outcome, String Name) {
        long Now = Util.getMillis();
        Applied = Outcome;
        OutcomeAt = Now;
        if (Outcome == Z0001Outcome.Taken) {
            if (!Name.isEmpty() && Name.equalsIgnoreCase(Text)) {
                Text = Name;
            }
            Stage = Phase.Settled;
            sound(Z0001Registry.TakeLayers, 0.9f, 0.7f);
            hint(Z0001Outcome.Spent.hintKey(), HintDelay);
            return;
        }
        Stage = Phase.Fading;
        Afterwards = Outcome == Z0001Outcome.Spent ? Name : "";
        sound(Z0001Registry.FadeLayers, 1.0f, 0.6f);
        hint(Outcome.hintKey(), 250L);
    }

    @Override
    public void tick() {
        super.tick();
        if (!Z0001BookItem.is(held())) {
            onClose();
            return;
        }
        long Now = Util.getMillis();
        switch (Stage) {
            case Blank -> {
                Z0001Page Page = page();
                if (Page != null && spent()) {
                    settle(Page.Name(), false);
                }
            }
            case Writing -> {
                long Done = writtenAt();
                if (Now < Done && Now - LastScratch >= ScratchGap) {
                    LastScratch = Now;
                    sound(Z0001Registry.QuillLayers, 0.9f + (float) Math.random() * 0.22f, 0.4f);
                }
                if (Now >= Done && Result != null) {
                    apply(Result, ResultName);
                } else if (Now >= Done + ReplyTimeout) {
                    apply(Z0001Outcome.NoTake, "");
                }
            }
            case Settled -> {
                if (Now - OutcomeAt > SoakTime && !spent()) {
                    Stage = Phase.Blank;
                    Text = "";
                    HintKey = null;
                    sound(Z0001Registry.TurnLayers, 1.05f, 0.45f);
                }
            }
            case Fading -> {
                if (Now - OutcomeAt >= FadeTime) {
                    if (!Afterwards.isEmpty()) {
                        settle(Afterwards, false);
                    } else {
                        Stage = Phase.Blank;
                        Text = "";
                    }
                }
            }
        }
    }

    private void write() {
        if (Stage != Phase.Blank || Text.isEmpty()) {
            return;
        }
        Stage = Phase.Writing;
        WriteStart = Util.getMillis();
        LastScratch = WriteStart;
        Result = null;
        ResultName = "";
        HintKey = null;
        sound(Z0001Registry.QuillLayers, 1.0f, 0.55f);
        PacketDistributor.sendToServer(new Z0001WritePayload(Hand, Text));
    }

    @Override
    public boolean charTyped(char Code, int Modifiers) {
        if (Stage != Phase.Blank) {
            return false;
        }
        if (Code < Z0001Ink.FirstPrintable || Code > Z0001Ink.LastPrintable) {
            return false;
        }
        if (Text.length() < Z0001Page.MaxName) {
            Text = Text + Code;
            if (HintKey != null && Util.getMillis() >= HintAt) {
                HintKey = null;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        if (Stage == Phase.Blank) {
            if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER) {
                write();
                return true;
            }
            if (Key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!Text.isEmpty()) {
                    Text = Screen.hasControlDown() ? "" : Text.substring(0, Text.length() - 1);
                }
                return true;
            }
            if (Screen.isPaste(Key)) {
                paste(Minecraft.getInstance().keyboardHandler.getClipboard());
                return true;
            }
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    private void paste(String Clip) {
        StringBuilder Built = new StringBuilder(Text);
        for (int Index = 0; Index < Clip.length() && Built.length() < Z0001Page.MaxName; Index++) {
            char Code = Clip.charAt(Index);
            if (Code >= Z0001Ink.FirstPrintable && Code <= Z0001Ink.LastPrintable) {
                Built.append(Code);
            }
        }
        Text = Built.toString();
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0 && Stage == Phase.Blank && !Text.isEmpty() && overButton(MouseX, MouseY)) {
            write();
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    private int buttonWidth() {
        return QuillSize + ButtonGap + width(Component.translatable(Prefix + "write").getString());
    }

    private int buttonX() {
        return TextRight - buttonWidth();
    }

    private int buttonY() {
        return ruleY(ButtonRule) - GlyphRise - 1;
    }

    private boolean overButton(double MouseX, double MouseY) {
        int X = Left + buttonX() - 2;
        int Y = Top + buttonY() - 1;
        return MouseX >= X && MouseX < X + buttonWidth() + 4 && MouseY >= Y && MouseY < Y + QuillSize + 2;
    }

    private int advance(char Character) {
        return font.width(FormattedText.of(String.valueOf(Character), Script));
    }

    private int width(String Value) {
        int Total = 0;
        for (int Index = 0; Index < Value.length(); Index++) {
            Total += advance(Value.charAt(Index));
        }
        return Total;
    }

    private static FormattedCharSequence glyph(char Character) {
        return FormattedCharSequence.forward(String.valueOf(Character), Script);
    }

    private static int argb(int Rgb, float Alpha) {
        return Mth.clamp(Math.round(Alpha * 255.0f), 0, 255) << 24 | Rgb & 0xFFFFFF;
    }

    private static int mix(int From, int To, float Amount) {
        float Clamped = Mth.clamp(Amount, 0.0f, 1.0f);
        int Red = Math.round(Mth.lerp(Clamped, From >> 16 & 255, To >> 16 & 255));
        int Green = Math.round(Mth.lerp(Clamped, From >> 8 & 255, To >> 8 & 255));
        int Blue = Math.round(Mth.lerp(Clamped, From & 255, To & 255));
        return Red << 16 | Green << 8 | Blue;
    }

    private static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        renderTransparentBackground(Graphics);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight, TextureWidth, TextureHeight);
        long Now = Util.getMillis();
        drawName(Graphics, Now);
        drawHint(Graphics, Now);
        if (Stage == Phase.Blank) {
            drawButton(Graphics, MouseX, MouseY);
        }
    }

    private void drawName(GuiGraphics Graphics, long Now) {
        if (Text.isEmpty()) {
            if (Stage == Phase.Blank) {
                drawCaret(Graphics, Now, TextLeft);
            }
            return;
        }
        int Baseline = ruleY(NameRule) - GlyphRise;
        List<int[]> Places = new ArrayList<>();
        int Cursor = TextLeft;
        for (int Index = 0; Index < Text.length(); Index++) {
            Places.add(new int[]{Cursor, Baseline});
            Cursor += advance(Text.charAt(Index));
        }
        int End = Cursor;
        Graphics.drawManaged(() -> {
            for (int Index = 0; Index < Text.length(); Index++) {
                char Character = Text.charAt(Index);
                if (Character == ' ') {
                    continue;
                }
                int X = Left + Places.get(Index)[0];
                int Y = Top + Places.get(Index)[1];
                switch (Stage) {
                    case Blank -> Graphics.drawString(font, glyph(Character), X, Y, argb(DraftInk, DraftAlpha), false);
                    case Writing -> {
                        float Ink = smooth((Now - WriteStart - Index * GlyphGap) / (float) GlyphInk);
                        if (Ink < 1.0f) {
                            Graphics.drawString(font, glyph(Character), X, Y, argb(DraftInk, DraftAlpha * (1.0f - Ink)), false);
                        }
                        if (Ink > 0.02f) {
                            Graphics.drawString(font, glyph(Character), X - 1, Y + 1, argb(BleedInk, BleedAlpha * Ink * 0.5f), false);
                            Graphics.drawString(font, glyph(Character), X, Y, argb(WetInk, Ink), false);
                        }
                    }
                    case Settled -> {
                        float Soak = Mth.clamp((Now - OutcomeAt) / (float) SoakTime, 0.0f, 1.0f);
                        float Bleed = BleedAlpha * (1.0f - Soak) + 0.12f;
                        Graphics.drawString(font, glyph(Character), X - 1, Y + 1, argb(BleedInk, Bleed), false);
                        Graphics.drawString(font, glyph(Character), X, Y, argb(mix(WetInk, DryInk, Soak), 1.0f), false);
                    }
                    case Fading -> {
                        float Gone = smooth((Now - OutcomeAt) / (float) FadeTime);
                        float Stagger = smooth((Now - OutcomeAt - Index * 40L) / (float) FadeTime);
                        int Colour = mix(WetInk, FadeInk, Gone * 1.4f);
                        Graphics.drawString(font, glyph(Character), X, Y, argb(Colour, 1.0f - Stagger), false);
                    }
                }
            }
        });
        if (Stage == Phase.Fading && Applied != Z0001Outcome.Faded) {
            drawBeads(Graphics, Now, Places);
        }
        if (Stage == Phase.Blank) {
            drawCaret(Graphics, Now, End);
        }
    }

    private void drawBeads(GuiGraphics Graphics, long Now, List<int[]> Places) {
        float Time = (Now - OutcomeAt) / (float) FadeTime;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int Index = 0; Index < Places.size(); Index++) {
            if (Text.charAt(Index) == ' ' || (Index * 7 + 3) % 3 == 0) {
                continue;
            }
            float Rise = smooth(Time * 2.2f - Index * 0.05f);
            float Dry = smooth((Time - 0.45f) * 2.0f);
            float Alpha = Rise * (1.0f - Dry);
            if (Alpha <= 0.02f) {
                continue;
            }
            int Sprite = (Index * 5 + Text.charAt(Index)) % BeadCount;
            int X = Left + Places.get(Index)[0] + 1;
            int Y = Top + Places.get(Index)[1] + GlyphRise - BeadSize + 1 + Math.round(Rise * 1.0f);
            Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
            Graphics.blit(Texture, X, Y, BeadU + Sprite * BeadSize, BeadV, BeadSize, BeadSize, TextureWidth, TextureHeight);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawCaret(GuiGraphics Graphics, long Now, int X) {
        if ((Now / CaretBlink) % 2L != 0L || Text.length() >= Z0001Page.MaxName) {
            return;
        }
        Graphics.blit(Texture, Left + X + 1, Top + ruleY(NameRule) - CaretHeight, CaretU, CaretV, CaretWidth, CaretHeight, TextureWidth, TextureHeight);
    }

    private void drawHint(GuiGraphics Graphics, long Now) {
        if (HintKey == null || Now < HintAt) {
            return;
        }
        float Alpha = smooth((Now - HintAt) / (float) HintFade) * 0.9f;
        List<String> Lines = wrap(Component.translatable(HintKey).getString(), TextRight - TextLeft);
        int Middle = (TextLeft + TextRight) / 2;
        Graphics.drawManaged(() -> {
            for (int Row = 0; Row < Lines.size() && Row < 2; Row++) {
                String Line = Lines.get(Row);
                int X = Left + Middle - width(Line) / 2;
                int Y = Top + ruleY(HintRule + Row) - GlyphRise;
                plain(Graphics, Line, X, Y, argb(HintInk, Alpha));
            }
        });
    }

    private void plain(GuiGraphics Graphics, String Line, int X, int Y, int Colour) {
        int Cursor = X;
        for (int Index = 0; Index < Line.length(); Index++) {
            char Character = Line.charAt(Index);
            if (Character != ' ') {
                Graphics.drawString(font, glyph(Character), Cursor, Y, Colour, false);
            }
            Cursor += advance(Character);
        }
    }

    private List<String> wrap(String Value, int Limit) {
        List<String> Lines = new ArrayList<>();
        String Current = "";
        for (String Word : Value.split(" ")) {
            String Trial = Current.isEmpty() ? Word : Current + " " + Word;
            if (width(Trial) <= Limit || Current.isEmpty()) {
                Current = Trial;
            } else {
                Lines.add(Current);
                Current = Word;
            }
        }
        if (!Current.isEmpty()) {
            Lines.add(Current);
        }
        return Lines;
    }

    private void drawButton(GuiGraphics Graphics, int MouseX, int MouseY) {
        boolean Ready = !Text.isEmpty();
        boolean Hover = Ready && overButton(MouseX, MouseY);
        float Alpha = Ready ? 1.0f : DisabledAlpha;
        int X = Left + buttonX();
        int Y = Top + buttonY();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
        Graphics.blit(Texture, X, Y, Hover ? QuillHoverU : QuillU, QuillV, QuillSize, QuillSize, TextureWidth, TextureHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        String Label = Component.translatable(Prefix + "write").getString();
        int Colour = argb(Hover ? ButtonHoverInk : ButtonInk, Alpha * (Hover ? 1.0f : 0.85f));
        Graphics.drawManaged(() -> plain(Graphics, Label, X + QuillSize + ButtonGap, Y + 1, Colour));
    }
}
