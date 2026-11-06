package net.rai.scp_unleashed.scp5001.mekhane.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneProtocol;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneValues;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;
import net.rai.scp_unleashed.scp5001.mekhane.block.MekhaneStatusBlockEntity;
import net.rai.scp_unleashed.scp5001.mekhane.network.MekhaneConsolePayload;
import org.jetbrains.annotations.Nullable;

public class MekhaneConsoleScreen extends Screen {
    public static final int PanelWidth = 244;
    public static final int PanelHeight = 150;
    public static final int HoldTicks = 30;
    public static final int Ceramic = 0xFFDCE1E4;
    public static final int CeramicLight = 0xFFEEF1F2;
    public static final int CeramicDark = 0xFFA9B1B8;
    public static final int Graphite = 0xFF15181D;
    public static final int GraphiteMid = 0xFF1F242B;
    public static final int GraphiteLight = 0xFF2A2F37;
    public static final int Seam = 0xFF8FE3FF;
    public static final int SeamDim = 0xFF3C6E80;
    public static final int Gold = 0xFFFFE2A0;
    public static final int GoldDeep = 0xFFC99A4A;
    public static final int Amber = 0xFFE0A040;
    public static final int Violet = 0xFF9C8CF0;
    public static final int Text = 0xFFE9EDEF;
    public static final int Muted = 0xFF7D858F;
    private final BlockPos Pos;
    private boolean Armed;
    private boolean Holding;
    private int Hold;
    private boolean Sent;
    private int Age;
    private int Left;
    private int Top;

    public MekhaneConsoleScreen(BlockPos Pos) {
        super(Component.translatable("gui.scp_unleashed.scp5001_emergency.title"));
        this.Pos = Pos;
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

    @Nullable
    private MekhaneStatusBlockEntity status() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return null;
        }
        return Client.level.getBlockEntity(Pos) instanceof MekhaneStatusBlockEntity Found ? Found : null;
    }

    @Override
    public void tick() {
        Age++;
        MekhaneStatusBlockEntity Status = status();
        Minecraft Client = Minecraft.getInstance();
        if (Status == null || Client.player == null || Client.player.distanceToSqr(Pos.getCenter()) > 64.0) {
            onClose();
            return;
        }
        if (Holding && !Sent && Status.stage() == MekhaneProtocol.Stage.Idle) {
            Hold++;
            if (Hold % 6 == 0) {
                Client.getSoundManager().play(SimpleSoundInstance.forUI(Scp5001MekhaneRegistry.ConsoleBeep.get(), 0.8f + Hold / (float) HoldTicks, 0.5f));
            }
            if (Hold >= HoldTicks) {
                Sent = true;
                Holding = false;
                PacketDistributor.sendToServer(new MekhaneConsolePayload(Pos, MekhaneConsolePayload.Engage));
            }
        }
    }

    private boolean overButton(double X, double Y) {
        return X >= Left + 22 && X < Left + PanelWidth - 22 && Y >= Top + 66 && Y < Top + 96;
    }

    @Override
    public boolean mouseClicked(double X, double Y, int Button) {
        MekhaneStatusBlockEntity Status = status();
        if (Button == 0 && Status != null && Status.stage() == MekhaneProtocol.Stage.Idle && !Sent && overButton(X, Y)) {
            Minecraft Client = Minecraft.getInstance();
            if (!Armed) {
                Armed = true;
                Client.getSoundManager().play(SimpleSoundInstance.forUI(Scp5001MekhaneRegistry.ConsoleBeep.get(), 1.2f, 0.6f));
            } else {
                Holding = true;
                Hold = 0;
            }
            return true;
        }
        return super.mouseClicked(X, Y, Button);
    }

    @Override
    public boolean mouseReleased(double X, double Y, int Button) {
        if (Button == 0 && Holding) {
            Holding = false;
            Hold = 0;
            return true;
        }
        return super.mouseReleased(X, Y, Button);
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        Graphics.fill(0, 0, width, height, 0x9A06080A);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        MekhaneStatusBlockEntity Status = status();
        if (Status == null) {
            return;
        }
        float Time = Age + Partial;
        frame(Graphics, Time);
        MekhaneProtocol.Stage Stage = Status.stage();
        Graphics.drawString(font, title, Left + 14, Top + 11, Gold, false);
        glyphs(Graphics, Left + PanelWidth - 70, Top + 10, Time, Stage != MekhaneProtocol.Stage.Idle);
        Component Line = Component.translatable("gui.scp_unleashed.scp5001_emergency.status." + statusKey(Status));
        Graphics.drawString(font, Line, Left + 14, Top + 30, Stage == MekhaneProtocol.Stage.Idle ? Amber : Seam, false);
        switch (Stage) {
            case Idle -> engage(Graphics, MouseX, MouseY, Time);
            case Engaged -> demand(Graphics, Status, Time);
            default -> answer(Graphics, Stage, Time);
        }
    }

    private static String statusKey(MekhaneStatusBlockEntity Status) {
        return switch (Status.stage()) {
            case Idle -> "standby";
            case Engaged -> "engaged";
            case Summon -> "summon";
            case Duel -> "duel";
            case Clear -> "clear";
        };
    }

    private void frame(GuiGraphics Graphics, float Time) {
        int L = Left;
        int T = Top;
        int R = Left + PanelWidth;
        int B = Top + PanelHeight;
        Graphics.fill(L + 2, T, R - 2, B, Ceramic);
        Graphics.fill(L, T + 2, R, B - 2, Ceramic);
        Graphics.fill(L + 2, T, R - 2, T + 1, CeramicLight);
        Graphics.fill(L, T + 2, L + 1, B - 2, CeramicLight);
        Graphics.fill(L + 2, B - 1, R - 2, B, CeramicDark);
        Graphics.fill(R - 1, T + 2, R, B - 2, CeramicDark);
        Graphics.fill(L + 4, T + 4, R - 4, B - 4, Graphite);
        Graphics.fill(L + 6, T + 6, R - 6, T + 23, GraphiteLight);
        Graphics.fill(L + 6, T + 23, R - 6, T + 24, Seam);
        Graphics.fill(L + 6, T + 24, R - 6, T + 25, SeamDim);
        Graphics.fill(L + 6, B - 14, R - 6, B - 6, GraphiteMid);
        int Sweep = (int) (Time * 2.0f) % (PanelWidth - 16);
        Graphics.fill(L + 8 + Sweep, B - 11, L + 12 + Sweep, B - 9, SeamDim);
        for (int Step = 0; Step < 9; Step++) {
            int X = L + 12 + Step * 26;
            Graphics.fill(X, B - 11, X + 2, B - 9, Step % 3 == 0 ? Violet : Muted);
        }
        Graphics.fill(L + 4, T + 4, L + 5, B - 4, GraphiteLight);
        Graphics.fill(R - 5, T + 4, R - 4, B - 4, GraphiteLight);
    }

    private void glyphs(GuiGraphics Graphics, int X, int Y, float Time, boolean Active) {
        for (int Index = 0; Index < 6; Index++) {
            int Gx = X + Index * 10;
            boolean On = Active ? ((int) (Time / 4.0f) + Index) % 3 != 0 : Index % 2 == 0;
            int Colour = On ? (Active ? Gold : Seam) : SeamDim;
            Graphics.fill(Gx + 2, Y, Gx + 6, Y + 1, Colour);
            Graphics.fill(Gx + 1, Y + 1, Gx + 2, Y + 7, Colour);
            Graphics.fill(Gx + 6, Y + 1, Gx + 7, Y + 7, Colour);
            Graphics.fill(Gx + 2, Y + 7, Gx + 6, Y + 8, Colour);
            if (Index % 2 == 1) {
                Graphics.fill(Gx + 3, Y + 3, Gx + 5, Y + 5, Colour);
            }
        }
    }

    private void engage(GuiGraphics Graphics, int MouseX, int MouseY, float Time) {
        int L = Left + 22;
        int R = Left + PanelWidth - 22;
        int T = Top + 66;
        int B = Top + 96;
        boolean Over = overButton(MouseX, MouseY);
        for (int Step = 0; Step < (R - L) / 8; Step++) {
            int X = L + Step * 8;
            Graphics.fill(X, T - 8, X + 4, T - 6, Armed ? Amber : GraphiteLight);
            Graphics.fill(X + 4, B + 6, X + 8, B + 8, Armed ? Amber : GraphiteLight);
        }
        Graphics.fill(L, T, R, B, Armed ? 0xFF3A2C14 : (Over ? GraphiteLight : GraphiteMid));
        Graphics.fill(L, T, R, T + 1, Armed ? Gold : Seam);
        Graphics.fill(L, B - 1, R, B, Armed ? GoldDeep : SeamDim);
        Graphics.fill(L, T, L + 1, B, Armed ? Gold : Seam);
        Graphics.fill(R - 1, T, R, B, Armed ? GoldDeep : SeamDim);
        if (Armed && Hold > 0) {
            int Fill = (int) ((R - L - 4) * Mth.clamp((Hold + (Holding ? 1.0f : 0.0f)) / HoldTicks, 0.0f, 1.0f));
            Graphics.fill(L + 2, B - 6, L + 2 + Fill, B - 3, Gold);
        }
        Component Label = Component.translatable(Armed ? "gui.scp_unleashed.scp5001_emergency.hold" : "gui.scp_unleashed.scp5001_emergency.engage");
        int Pulse = Armed && ((int) (Time / 6.0f)) % 2 == 0 ? Gold : Text;
        Graphics.drawCenteredString(font, Label, (L + R) / 2, T + 11, Pulse);
        if (Armed) {
            Graphics.drawCenteredString(font, Component.translatable("gui.scp_unleashed.scp5001_emergency.release"), (L + R) / 2, B + 14, Muted);
        }
    }

    private void demand(GuiGraphics Graphics, MekhaneStatusBlockEntity Status, float Time) {
        bar(Graphics, Top + 50, "gui.scp_unleashed.scp5001_emergency.iron", Status.iron(), Status.ironNeed());
        bar(Graphics, Top + 82, "gui.scp_unleashed.scp5001_emergency.electronics", Status.electronics(), Status.electronicsNeed());
        int Colour = ((int) (Time / 10.0f)) % 2 == 0 ? Seam : SeamDim;
        Graphics.drawCenteredString(font, Component.translatable("gui.scp_unleashed.scp5001_emergency.intake"), Left + PanelWidth / 2, Top + 116, Colour);
    }

    private void bar(GuiGraphics Graphics, int Y, String Key, long Have, long Need) {
        int L = Left + 16;
        int R = Left + PanelWidth - 16;
        Graphics.drawString(font, Component.translatable(Key), L, Y, Text, false);
        String Amount = Component.translatable("gui.scp_unleashed.scp5001_emergency.amount", whole(Have), whole(Need)).getString();
        Graphics.drawString(font, Amount, R - font.width(Amount), Y, Muted, false);
        int Bt = Y + 11;
        Graphics.fill(L, Bt, R, Bt + 10, GraphiteLight);
        Graphics.fill(L + 1, Bt + 1, R - 1, Bt + 9, Graphite);
        float Share = Mth.clamp(Have / (float) Math.max(1L, Need), 0.0f, 1.0f);
        int Fill = (int) ((R - L - 4) * Share);
        Graphics.fill(L + 2, Bt + 2, L + 2 + Fill, Bt + 8, Share >= 1.0f ? Gold : Seam);
        Graphics.fill(L + 2, Bt + 2, L + 2 + Fill, Bt + 3, Share >= 1.0f ? 0xFFFFF4D2 : 0xFFEFFCFF);
        for (int Tick = 1; Tick < 8; Tick++) {
            int X = L + 2 + (R - L - 4) * Tick / 8;
            Graphics.fill(X, Bt + 2, X + 1, Bt + 8, 0x66000000);
        }
    }

    private static String whole(long Units) {
        return Long.toString(Units / MekhaneValues.Scale);
    }

    private void answer(GuiGraphics Graphics, MekhaneProtocol.Stage Stage, float Time) {
        String Key = switch (Stage) {
            case Summon -> "gui.scp_unleashed.scp5001_emergency.conduit";
            case Duel -> "gui.scp_unleashed.scp5001_emergency.contact";
            default -> "gui.scp_unleashed.scp5001_emergency.silence";
        };
        int Centre = Left + PanelWidth / 2;
        for (int Column = 0; Column < 26; Column++) {
            float Wave = Stage == MekhaneProtocol.Stage.Clear ? 0.1f : 0.5f + 0.5f * Mth.sin(Time * 0.25f + Column * 0.7f);
            int Tall = 4 + (int) (Wave * 34.0f);
            int X = Left + 18 + Column * 8;
            int Bottom = Top + 108;
            Graphics.fill(X, Bottom - Tall, X + 5, Bottom, Column % 5 == 2 ? Gold : GoldDeep);
            Graphics.fill(X, Bottom - Tall, X + 5, Bottom - Tall + 1, 0xFFFFF4D2);
        }
        Graphics.drawCenteredString(font, Component.translatable(Key), Centre, Top + 118, Gold);
    }
}
