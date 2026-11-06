package net.rai.scp_unleashed.scp5001.event.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001ActionPayload;

public class Scp5001GeneratorScreen extends Screen {
    public static final ResourceLocation Texture = Scp5001EventRegistry.location("textures/gui/scp5001_event/console_generator.png");
    public static final int PanelWidth = 208;
    public static final int PanelHeight = 148;
    public static final int ButtonX = 56;
    public static final int ButtonY = 108;
    public static final int Pale = 0xCED6DE;
    public static final int Dim = 0x6E7884;
    public static final int Amber = 0xF8CE78;
    public static final int Cyan = 0x8FE3FF;
    private static final String Prefix = "gui.scp_unleashed.scp5001_event.generator.";
    private final BlockPos Pos;
    private final BlockPos Linked;
    private final int Node;
    private int Left;
    private int Top;
    private int Spin;

    public Scp5001GeneratorScreen(BlockPos Pos, BlockPos Linked, int Node) {
        super(Component.translatable(Prefix + "title", Node));
        this.Pos = Pos;
        this.Linked = Linked;
        this.Node = Node;
    }

    @Override
    protected void init() {
        Left = (width - PanelWidth) / 2;
        Top = (height - PanelHeight) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.player.distanceToSqr(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5) > 64.0) {
            onClose();
        }
        Spin++;
    }

    private static MutableComponent text(String Key, Object... Arguments) {
        return Component.translatable(Prefix + Key, Arguments);
    }

    private boolean hot() {
        return Node > 0 && Scp5001ClientState.overdriven(Linked);
    }

    private boolean armed() {
        return Node > 0 && !hot() && Scp5001ClientState.phase() == Scp5001State.Phase.Rising;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight, 256, 256);
        Component Title = Node > 0 ? text("title", Node) : text("unlinked");
        Graphics.drawString(font, Title, Left + 104 - font.width(Title) / 2, Top + 12, Pale);
        boolean Hot = hot();
        boolean Rising = Scp5001ClientState.phase() == Scp5001State.Phase.Rising;
        int Value = Hot ? Amber : Pale;
        row(Graphics, 0, text("rotor"), Hot ? text("rotor.forced") : text("rotor.idle"), Value);
        row(Graphics, 1, text("sync"), Hot ? text("sync.forced") : text("sync.locked"), Value);
        row(Graphics, 2, text("feed"), Hot ? text("feed.full") : Rising ? text("feed.strained") : text("feed.idle"), Value);
        float Fill = Hot ? 1.0f : 0.22f + 0.04f * (float) Math.sin((Spin + Partial) * 0.2f);
        int Width = Math.round(Fill * 172);
        for (int X = 18; X < 18 + Width; X++) {
            if (X % 4 != 3) {
                Graphics.fill(Left + X, Top + 32, Left + X + 1, Top + 34, 0xFF000000 | (Hot ? Amber : Cyan));
            }
        }
        boolean Armed = armed();
        boolean Hover = Armed && MouseX >= Left + ButtonX && MouseX < Left + ButtonX + 96 && MouseY >= Top + ButtonY && MouseY < Top + ButtonY + 28;
        int V = Hot ? 180 : Armed ? (Hover ? 180 : 150) : 150;
        int U = Hot ? 96 : Armed ? (Hover ? 0 : 96) : 0;
        Graphics.blit(Texture, Left + ButtonX, Top + ButtonY, U, V, 96, 28, 256, 256);
        Component Label = Hot ? text("active") : Armed ? text("overdrive") : text("inhibited");
        boolean Dark = Hot || Armed;
        Graphics.drawString(font, Label, Left + 104 - font.width(Label) / 2, Top + ButtonY + 10, Dark ? 0x221A0A : Dim, !Dark);
    }

    private void row(GuiGraphics Graphics, int Index, Component Label, Component Value, int Colour) {
        int Y = Top + 40 + Index * 18;
        Graphics.drawString(font, Label, Left + 18, Y, Dim);
        Graphics.drawString(font, Value, Left + 192 - font.width(Value), Y, Colour);
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == 0 && armed() && MouseX >= Left + ButtonX && MouseX < Left + ButtonX + 96 && MouseY >= Top + ButtonY && MouseY < Top + ButtonY + 28) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Scp5001EventRegistry.ConsoleBeep.get(), 0.6f));
            PacketDistributor.sendToServer(new Scp5001ActionPayload(Pos, Scp5001ActionPayload.OverdriveAction));
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }
}
