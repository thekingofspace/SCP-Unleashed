package net.rai.scp_unleashed.computer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.network.ComputerAction;
import net.rai.scp_unleashed.computer.network.ComputerActionPayload;
import org.lwjgl.glfw.GLFW;

public class MessageDatabaseScreen extends Screen {
    public static final String Prefix = "gui.scp_unleashed.message_database.";
    public static final int PanelU = 0;
    public static final int PanelV = 328;
    public static final int PanelWidth = 208;
    public static final int PanelHeight = 104;
    public static final int GlassX = 8;
    public static final int GlassY = 8;
    public static final int GlassWidth = 192;
    public static final int GlassHeight = 72;
    public static final int PlateX = 12;
    public static final int PlateY = 86;
    public static final int PlateWidth = 104;
    public static final int LedX = 188;
    public static final int LedY = 90;
    public static final int ConfirmTicks = 60;
    public static final double MaxDistanceSqr = 64.0;

    private final BlockPos Pos;
    private final OsUi Ui = new OsUi();
    private final OsField Name = new OsField(ChatChannels.MaxName, true, Letter -> Letter != ' ');
    private int Count;
    private int Capacity = 200;
    private boolean Running;
    private int Left;
    private int Top;
    private int Ticks;
    private int ConfirmUntil = -1;

    public MessageDatabaseScreen(BlockPos Pos, CompoundTag Data) {
        super(Component.translatable(Prefix + "title"));
        this.Pos = Pos.immutable();
        accept(Data);
    }

    public BlockPos getPos() {
        return Pos;
    }

    public void accept(CompoundTag Data) {
        if (Ui.focus() != Name) {
            Name.setValue(Data.getString("Name"));
        }
        Count = Data.getInt("Count");
        Capacity = Math.max(1, Data.getInt("Capacity"));
        Running = Data.getBoolean("Running");
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

    @Override
    public void tick() {
        super.tick();
        Ticks++;
        if (minecraft == null || minecraft.player == null || minecraft.player.distanceToSqr(Vec3.atCenterOf(Pos)) > MaxDistanceSqr) {
            onClose();
        }
    }

    private void send(ComputerAction Action, CompoundTag Data) {
        PacketDistributor.sendToServer(new ComputerActionPayload(Pos, Action.ordinal(), Data));
    }

    private void rename() {
        CompoundTag Data = new CompoundTag();
        Data.putString("Name", Name.getValue());
        send(ComputerAction.DatabaseRename, Data);
        Ui.focus(null);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        Ui.begin(Graphics, font, MouseX, MouseY, Ticks);
        Ui.sprite(Left, Top, PanelU, PanelV, PanelWidth, PanelHeight);
        Ui.centered(Ui.fit(Component.translatable(Prefix + "plate").getString(), PlateWidth - 6), Left + PlateX + PlateWidth / 2, Top + PlateY + 3, OsLayout.PlateInk);
        Ui.fill(Left + LedX, Top + LedY, 5, 5, Running ? 0xFF6CFF8C : 0xFF143A1E);
        if (Running) {
            Ui.fill(Left + LedX + 1, Top + LedY + 1, 1, 1, 0xFFFFFFFF);
        }
        int X = Left + GlassX + 6;
        int Y = Top + GlassY + 4;
        int W = GlassWidth - 12;
        Ui.heading(Component.translatable(Prefix + "title").getString(), X, Y, W);
        Ui.text(Component.translatable(Prefix + "channel").getString(), X, Y + 15, OsLayout.PhosphorTitle);
        Ui.field(Name, X + 46, Y + 13, 84, null, Component.translatable(Prefix + "channel.hint"));
        Ui.button(X + 134, Y + 13, W - 134, OsLayout.FieldHeight, Component.translatable(Prefix + "rename"), !Name.getValue().isBlank(), this::rename, null);
        Ui.text(Component.translatable(Prefix + "stored", Count, Capacity).getString(), X, Y + 31, OsLayout.Phosphor);
        Ui.box(X, Y + 41, W, 6, OsLayout.PhosphorDim, OsLayout.PhosphorDark);
        Ui.fill(X + 1, Y + 42, (W - 2) * Math.min(Count, Capacity) / Capacity, 4, Count >= Capacity ? OsLayout.Amber : OsLayout.Phosphor);
        boolean Confirming = Ticks < ConfirmUntil;
        Ui.button(X, Y + 51, 80, OsLayout.ButtonHeight, Component.translatable(Confirming ? Prefix + "confirm" : Prefix + "clear"), Count > 0, () -> {
            if (Ticks < ConfirmUntil) {
                send(ComputerAction.DatabaseClear, new CompoundTag());
                ConfirmUntil = -1;
            } else {
                ConfirmUntil = Ticks + ConfirmTicks;
            }
        }, Component.translatable(Prefix + "clear.hint"));
        Ui.lamp(X + 86, Y + 55, Running, Running ? 0 : 2);
        Ui.text(Component.translatable(Running ? Prefix + "online" : Prefix + "offline").getString(), X + 94, Y + 54, Running ? OsLayout.PhosphorDim : OsLayout.Alert);
        Ui.scanlines(Left + GlassX + 1, Top + GlassY + 1, GlassWidth - 2, GlassHeight - 2);
        Ui.end();
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        Ui.focus(null);
        if (Ui.click(MouseX, MouseY, Button)) {
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        OsField Focused = Ui.focus();
        if (Key == GLFW.GLFW_KEY_ESCAPE) {
            if (Focused != null) {
                Ui.focus(null);
                return true;
            }
            onClose();
            return true;
        }
        if (Focused != null) {
            if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER) {
                rename();
                return true;
            }
            Focused.keyPressed(Key);
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(Key, ScanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    @Override
    public boolean charTyped(char Letter, int Modifiers) {
        OsField Focused = Ui.focus();
        if (Focused != null && Focused.charTyped(Letter)) {
            return true;
        }
        return super.charTyped(Letter, Modifiers);
    }
}
