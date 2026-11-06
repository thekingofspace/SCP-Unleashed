package net.rai.scp_unleashed.scp105.photo096.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp105.Scp105Registry;
import net.rai.scp_unleashed.scp105.photo096.Scp105Photo096Registry;

import java.util.List;

public class Scp105Photo096Screen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp105/photo_096.png");
    public static final String Prefix = "gui.scp_unleashed.scp105_photograph_096.";
    public static final int PhotoWidth = 60;
    public static final int PhotoHeight = 72;
    public static final int ImageLeft = 4;
    public static final int ImageTop = 4;
    public static final int ImageSize = 52;
    public static final int StripTop = 57;
    public static final int StripHeight = 14;
    public static final int StripMargin = 4;
    public static final int SideRoom = 16;
    public static final int HintRoom = 28;
    public static final int Backdrop = 0xE00A0A0E;
    public static final int Ink = 0x4A4852;
    public static final int HintInk = 0x8A8A92;
    public static final int FlashTicks = 8;
    public static final int RattleTicks = 6;
    public static final int HintDelay = 40;
    public static final int HintFade = 20;
    public static final int[] ShutterTicks = {0, 2, 5};
    public static final int StingTick = 7;
    public static final float TextStep = 3.5f;

    private int Age;
    private boolean Greeted;

    public Scp105Photo096Screen() {
        super(Component.translatable(Prefix + "title"));
    }

    public static int photoScale(int Width, int Height) {
        return Math.max(1, Math.min((Width - SideRoom) / PhotoWidth, (Height - HintRoom) / PhotoHeight));
    }

    public static int textScale(int Scale) {
        return Math.max(1, Math.round(Scale / TextStep));
    }

    @Override
    protected void init() {
        if (!Greeted) {
            Greeted = true;
            shutter(0);
        }
    }

    @Override
    public void tick() {
        Age++;
        for (int Index = 1; Index < ShutterTicks.length; Index++) {
            if (Age == ShutterTicks[Index]) {
                shutter(Index);
            }
        }
        if (Age == StingTick) {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp105Photo096Registry.StingLayers) {
                play(Layer, 1.0f, 0.9f);
            }
        }
    }

    private void shutter(int Index) {
        float Pitch = 1.0f - Index * 0.07f;
        float Volume = 0.9f - Index * 0.2f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp105Registry.ShutterLayers) {
            play(Layer, Pitch, Volume);
        }
        play(Scp105Photo096Registry.Rattle, 1.0f + Index * 0.06f, 0.8f);
    }

    private void play(DeferredHolder<SoundEvent, SoundEvent> Sound, float Pitch, float Volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound.get(), Pitch, Volume));
        }
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        Graphics.fill(0, 0, width, height, Backdrop);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        int Scale = photoScale(width, height);
        int Wide = PhotoWidth * Scale;
        int Tall = PhotoHeight * Scale;
        int Left = (width - Wide) / 2;
        int Top = (height - HintRoom - Tall) / 2 + HintRoom / 4;
        if (Age < RattleTicks) {
            int Shake = Math.max(1, Scale / 2);
            Left += (Age % 2 == 0 ? Shake : -Shake);
            Top += (Age % 3 == 1 ? Shake : 0);
        }
        Graphics.blit(Texture, Left, Top, Wide, Tall, 0.0f, 0.0f, PhotoWidth, PhotoHeight, PhotoWidth, PhotoHeight);
        float Time = Age + Partial;
        if (Time < FlashTicks) {
            float Glare = 1.0f - Time / FlashTicks;
            int Alpha = Mth.clamp(Math.round(Glare * Glare * 255.0f), 0, 255);
            Graphics.fill(Left + ImageLeft * Scale, Top + ImageTop * Scale, Left + (ImageLeft + ImageSize) * Scale, Top + (ImageTop + ImageSize) * Scale, Alpha << 24 | 0xF4F4F0);
        }
        caption(Graphics, Left, Top, Scale);
        if (Time > HintDelay) {
            float Share = Mth.clamp((Time - HintDelay) / HintFade, 0.0f, 1.0f);
            int Alpha = Math.max(4, Math.round(Share * 200.0f));
            Component Hint = Component.translatable(Prefix + "close");
            Graphics.drawCenteredString(font, Hint, width / 2, height - HintRoom / 2 - font.lineHeight / 2, Alpha << 24 | HintInk);
        }
    }

    private void caption(GuiGraphics Graphics, int Left, int Top, int Scale) {
        int Size = textScale(Scale);
        int Room = (PhotoWidth - StripMargin * 2) * Scale / Size;
        List<FormattedCharSequence> Lines = font.split(Component.translatable(Prefix + "caption"), Room);
        int Block = Lines.size() * font.lineHeight * Size;
        int Middle = Top + (StripTop + StripHeight / 2) * Scale;
        int Y = Middle - Block / 2;
        Graphics.pose().pushPose();
        Graphics.pose().translate(Left + PhotoWidth * Scale / 2, Y, 0.0f);
        Graphics.pose().scale(Size, Size, 1.0f);
        int Row = 0;
        for (FormattedCharSequence Line : Lines) {
            Graphics.drawString(font, Line, -font.width(Line) / 2, Row * font.lineHeight, 0xFF000000 | Ink, false);
            Row++;
        }
        Graphics.pose().popPose();
    }

    @Override
    public boolean keyPressed(int Key, int Scan, int Modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(Key, Scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(Key, Scan, Modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
