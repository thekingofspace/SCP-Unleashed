package net.rai.scp_unleashed.scp444.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.scp444.Scp444BookItem;
import net.rai.scp_unleashed.scp444.Scp444Pages;
import net.rai.scp_unleashed.scp444.Scp444Registry;
import net.rai.scp_unleashed.scp444.network.Scp444ClosedPayload;
import net.rai.scp_unleashed.scp444.network.Scp444TurnPayload;
import net.rai.scp_unleashed.senses.Senses;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scp444BookScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp444/book.png");
    public static final ResourceLocation PlateTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp444/plates.png");
    public static final int TextureWidth = 256;
    public static final int TextureHeight = 256;
    public static final int PlateTextureWidth = 256;
    public static final int PlateTextureHeight = 128;
    public static final int PlateCell = 64;
    public static final int PlateInset = 4;
    public static final int PlatesPerRow = 4;
    public static final int PanelWidth = 192;
    public static final int PanelHeight = 192;
    public static final int ArrowWidth = 18;
    public static final int ArrowHeight = 10;
    public static final int ArrowY = 166;
    public static final int BackArrowX = 44;
    public static final int NextArrowX = 130;
    public static final int NextSpriteU = 192;
    public static final int BackSpriteU = 212;
    public static final int SpriteV = 0;
    public static final int HoverSpriteV = 12;
    public static final int NumberY = 168;
    public static final int NumberColour = 0x6A2A1C;
    public static final float ShieldedFade = 0.45f;

    private final Map<Integer, Scp444Pages.Layout> Layouts = new HashMap<>();
    private int Page;
    private int Left;
    private int Top;
    private boolean Greeted;
    private boolean Closing;

    public Scp444BookScreen() {
        super(Component.translatable("gui.scp_unleashed.scp444.title"));
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - PanelWidth) / 2;
        Top = Math.max(2, (height - PanelHeight) / 2);
        if (!Greeted) {
            Greeted = true;
            sound(Scp444Registry.OpenLayers, 0.92f + (float) Math.random() * 0.08f, 0.8f);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!Closing) {
            Closing = true;
            PacketDistributor.sendToServer(Scp444ClosedPayload.Instance);
            sound(Scp444Registry.CloseLayers, 0.9f + (float) Math.random() * 0.08f, 0.75f);
        }
        super.onClose();
    }

    @Override
    public void removed() {
        if (!Closing) {
            Closing = true;
            PacketDistributor.sendToServer(Scp444ClosedPayload.Instance);
        }
        super.removed();
    }

    @Override
    public void tick() {
        super.tick();
        LocalPlayer User = Minecraft.getInstance().player;
        if (User == null || !User.isAlive() || Senses.isBlindfolded(User)
                || !(Scp444BookItem.is(User.getItemInHand(InteractionHand.MAIN_HAND)) || Scp444BookItem.is(User.getItemInHand(InteractionHand.OFF_HAND)))) {
            onClose();
        }
    }

    private static void sound(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch, float Volume) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Layer.get(), Pitch, Volume));
        }
    }

    public int page() {
        return Page;
    }

    private void turn(int Target) {
        int Wanted = Mth.clamp(Target, 0, Scp444Pages.PageCount - 1);
        if (Wanted == Page) {
            return;
        }
        Page = Wanted;
        sound(Scp444Registry.TurnLayers, 0.9f + (float) Math.random() * 0.15f, 0.7f);
        PacketDistributor.sendToServer(new Scp444TurnPayload(Page));
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
            default -> {
                return super.keyPressed(Key, ScanCode, Modifiers);
            }
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0) {
            if (Page > 0 && over(MouseX, MouseY, BackArrowX)) {
                turn(Page - 1);
                return true;
            }
            if (Page < Scp444Pages.PageCount - 1 && over(MouseX, MouseY, NextArrowX)) {
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

    private boolean over(double MouseX, double MouseY, int X) {
        return MouseX >= Left + X && MouseX < Left + X + ArrowWidth && MouseY >= Top + ArrowY && MouseY < Top + ArrowY + ArrowHeight;
    }

    private Scp444Pages.Layout layout(int Index) {
        return Layouts.computeIfAbsent(Index, Key -> Scp444Pages.layout(Key, Text -> Scp444Script.width(font, Text, Scp444Pages.MainFont),
                Text -> Scp444Script.width(font, Text, Scp444Pages.SmallFont)));
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        renderTransparentBackground(Graphics);
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight, TextureWidth, TextureHeight);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        LocalPlayer User = Minecraft.getInstance().player;
        float Fade = AntiMemetic.isProtected(User) ? ShieldedFade : 1.0f;
        Scp444Pages.Layout Current = layout(Page);
        RenderSystem.enableBlend();
        for (Scp444Pages.Plate Shown : Current.Plates()) {
            int Index = Math.floorMod(Shown.Index(), Scp444Pages.Plates);
            int U = (Index % PlatesPerRow) * PlateCell + PlateInset;
            int V = (Index / PlatesPerRow) * PlateCell + PlateInset;
            Graphics.setColor(1.0f, 1.0f, 1.0f, Fade);
            Graphics.blit(PlateTexture, Left + Shown.X(), Top + Shown.Y(), U, V, Scp444Pages.PlateSize, Scp444Pages.PlateSize, PlateTextureWidth, PlateTextureHeight);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        for (Scp444Pages.Run Written : Current.Runs()) {
            int Colour = argb(Written.Colour(), Written.Alpha(), Fade);
            if (Written.Scale() == 1) {
                Graphics.drawString(font, Scp444Script.sequence(Written.Text(), Written.Face()), Left + Written.X(), Top + Written.Y(), Colour, false);
                continue;
            }
            PoseStack Pose = Graphics.pose();
            Pose.pushPose();
            Pose.translate(Left + Written.X(), Top + Written.Y(), 0.0f);
            Pose.scale(Written.Scale(), Written.Scale(), 1.0f);
            Graphics.drawString(font, Scp444Script.sequence(Written.Text(), Written.Face()), 0, 0, Colour, false);
            Pose.popPose();
        }
        for (Scp444Pages.Strike Line : Current.Strikes()) {
            Graphics.fill(Left + Line.X(), Top + Line.Y(), Left + Line.X() + Line.Width(), Top + Line.Y() + 1, argb(Line.Colour(), Line.Alpha(), Fade));
        }
        for (Scp444Pages.Note Margin : Current.Notes()) {
            PoseStack Pose = Graphics.pose();
            Pose.pushPose();
            Pose.translate(Left + Margin.X(), Top + Margin.Y(), 0.0f);
            Pose.mulPose(Axis.ZP.rotationDegrees(-90.0f));
            Graphics.drawString(font, Scp444Script.sequence(Margin.Text(), Scp444Pages.SmallFont), 0, 0, argb(Margin.Colour(), Margin.Alpha(), Fade), false);
            Pose.popPose();
        }
        int NumberWidth = Scp444Script.width(font, Current.Number(), Scp444Pages.SmallFont);
        Graphics.drawString(font, Scp444Script.sequence(Current.Number(), Scp444Pages.SmallFont), Left + (PanelWidth - NumberWidth) / 2, Top + NumberY,
                argb(NumberColour, 220, Fade), false);
        if (Page > 0) {
            arrow(Graphics, BackArrowX, BackSpriteU, MouseX, MouseY);
        }
        if (Page < Scp444Pages.PageCount - 1) {
            arrow(Graphics, NextArrowX, NextSpriteU, MouseX, MouseY);
        }
        RenderSystem.disableBlend();
    }

    private void arrow(GuiGraphics Graphics, int X, int U, int MouseX, int MouseY) {
        int V = over(MouseX, MouseY, X) ? HoverSpriteV : SpriteV;
        Graphics.blit(Texture, Left + X, Top + ArrowY, U, V, ArrowWidth, ArrowHeight, TextureWidth, TextureHeight);
    }

    private static int argb(int Colour, int Alpha, float Fade) {
        int Faded = Mth.clamp(Math.round(Alpha * Fade), 8, 255);
        return Faded << 24 | (Colour & 0xFFFFFF);
    }
}
