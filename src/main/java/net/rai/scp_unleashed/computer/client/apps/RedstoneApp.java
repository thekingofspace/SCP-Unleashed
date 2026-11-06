package net.rai.scp_unleashed.computer.client.apps;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.computer.client.ComputerScreen;
import net.rai.scp_unleashed.computer.client.OsApp;
import net.rai.scp_unleashed.computer.client.OsField;
import net.rai.scp_unleashed.computer.client.OsLayout;
import net.rai.scp_unleashed.computer.client.OsUi;
import net.rai.scp_unleashed.computer.data.ComputerText;
import net.rai.scp_unleashed.computer.data.RedstoneButton;
import net.rai.scp_unleashed.computer.net.ComputerCodecs;
import net.rai.scp_unleashed.computer.network.ComputerAction;
import net.rai.scp_unleashed.wire.WireChannels;

import java.util.ArrayList;
import java.util.List;

public class RedstoneApp implements OsApp {
    public static final String Prefix = "gui.scp_unleashed.computer.redstone.";
    public static final int Columns = 3;
    public static final int TileWidth = 94;
    public static final int TileHeight = 26;
    public static final int Gap = 3;

    private final List<Entry> Buttons = new ArrayList<>();
    private final OsField Title = new OsField(RedstoneButton.MaxTitle, true, OsField.AnyText);
    private final OsField Port = new OsField(WireChannels.PortDigits, false, OsField.Digits);
    private int Max = 14;
    private boolean Editing;
    private int EditId;
    private RedstoneButton.Mode Kind = RedstoneButton.Mode.Toggle;
    private int Seconds = RedstoneButton.DefaultSeconds;
    private ComputerScreen Owner;

    @Override
    public void accept(CompoundTag Data, HolderLookup.Provider Registries) {
        Buttons.clear();
        Max = Data.getInt("Max");
        ListTag Saved = Data.getList("Buttons", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Each = Saved.getCompound(Index);
            ComputerCodecs.decode(RedstoneButton.ButtonCodec, Each, Registries)
                    .ifPresent(Button -> Buttons.add(new Entry(Button, Each.getBoolean("On"), Each.getLong("Left"))));
        }
    }

    @Override
    public boolean back() {
        if (Editing) {
            Editing = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean submit(OsField Field) {
        if (Editing) {
            save();
            return true;
        }
        return false;
    }

    @Override
    public void render(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H) {
        Owner = Screen;
        int Count = Buttons.size() + (Buttons.size() < Max ? 1 : 0);
        for (int Index = 0; Index < Count; Index++) {
            int TileX = X + (Index % Columns) * (TileWidth + Gap);
            int TileY = Y + (Index / Columns) * (TileHeight + Gap);
            if (TileY + TileHeight > Y + H) {
                break;
            }
            if (Index >= Buttons.size()) {
                drawNew(Ui, TileX, TileY);
            } else {
                drawTile(Screen, Ui, Buttons.get(Index), TileX, TileY);
            }
        }
        if (Buttons.isEmpty()) {
            Ui.text(Component.translatable(Prefix + "empty").getString(), X + TileWidth + Gap + 4, Y + 4, OsLayout.PhosphorDim);
            Ui.text(Component.translatable(Prefix + "empty.hint").getString(), X + TileWidth + Gap + 4, Y + 15, OsLayout.PhosphorFaint);
        }
        if (Editing) {
            drawEditor(Ui, X + 34, Y + 14, W - 68, H - 28);
        }
    }

    private void drawTile(ComputerScreen Screen, OsUi Ui, Entry Shown, int X, int Y) {
        RedstoneButton Button = Shown.Button();
        boolean Over = !Editing && Ui.hovered(X, Y, TileWidth, TileHeight);
        int Border = Shown.On() ? OsLayout.Phosphor : Over ? OsLayout.PhosphorTitle : OsLayout.PhosphorDim;
        Ui.box(X, Y, TileWidth, TileHeight, Border, Shown.On() ? 0xFF1C5A30 : Over ? OsUi.ButtonHover : OsLayout.PhosphorDark);
        Ui.lamp(X + TileWidth - 8, Y + 4, Shown.On(), 0);
        Ui.clipped(Button.Title(), X + 4, Y + 4, TileWidth - 14, Shown.On() ? OsLayout.PhosphorHot : OsLayout.Phosphor);
        String Mode = Button.Kind() == RedstoneButton.Mode.Toggle ? Component.translatable(Prefix + "toggle").getString()
                : Component.translatable(Prefix + "pulse.short", Button.Seconds()).getString();
        String State = Shown.On() && Shown.Left() > 0 ? Component.translatable(Prefix + "left", (Shown.Left() + 19) / 20).getString() : "";
        int StateWidth = State.isEmpty() ? 0 : Ui.font().width(State) + 4;
        Ui.clipped("P" + ComputerText.port(Button.Port()) + " " + Mode, X + 4, Y + 15, TileWidth - 8 - StateWidth, OsLayout.PhosphorDim);
        if (!State.isEmpty()) {
            Ui.right(State, X + TileWidth - 4, Y + 15, OsLayout.PhosphorHot);
        }
        if (!Editing) {
            int Id = Button.Id();
            Ui.hit(X, Y, TileWidth, TileHeight, () -> press(Screen, Id), () -> edit(Button), Component.translatable(Prefix + "tile.hint"));
        }
    }

    private void drawNew(OsUi Ui, int X, int Y) {
        boolean Over = !Editing && Ui.hovered(X, Y, TileWidth, TileHeight);
        Ui.frame(X, Y, TileWidth, TileHeight, Over ? OsLayout.Phosphor : OsLayout.PhosphorFaint);
        String Label = Component.translatable(Prefix + "new").getString();
        int LabelX = X + (TileWidth - Ui.font().width(Label) - 11) / 2;
        Ui.glyph(OsLayout.GlyphPlus, LabelX, Y + 9, Over ? OsLayout.PhosphorHot : OsLayout.PhosphorDim);
        Ui.text(Label, LabelX + 11, Y + 9, Over ? OsLayout.PhosphorHot : OsLayout.PhosphorDim);
        if (!Editing) {
            Ui.hit(X, Y, TileWidth, TileHeight, () -> create(), null, Component.translatable(Prefix + "new.hint"));
        }
    }

    private void press(ComputerScreen Screen, int Id) {
        CompoundTag Data = new CompoundTag();
        Data.putInt("Id", Id);
        Screen.send(ComputerAction.ButtonPress, Data);
    }

    private void create() {
        EditId = 0;
        Title.setValue("");
        Port.setValue("0");
        Kind = RedstoneButton.Mode.Toggle;
        Seconds = RedstoneButton.DefaultSeconds;
        Editing = true;
        Owner.ui().focus(Title);
    }

    private void edit(RedstoneButton Button) {
        EditId = Button.Id();
        Title.setValue(Button.Title());
        Port.setValue(String.valueOf(Button.Port()));
        Kind = Button.Kind();
        Seconds = Button.Seconds();
        Editing = true;
        Owner.ui().focus(null);
    }

    private void save() {
        RedstoneButton Draft = new RedstoneButton(EditId, Title.getValue(), WireChannels.clampPort(Port.number(0)), Kind, Seconds);
        CompoundTag Data = new CompoundTag();
        Tag Encoded = ComputerCodecs.encode(RedstoneButton.ButtonCodec, Draft, Owner.registries());
        if (Encoded != null) {
            Data.put("Button", Encoded);
            Owner.send(ComputerAction.ButtonSave, Data);
        }
        Editing = false;
        Owner.ui().focus(null);
    }

    private void drawEditor(OsUi Ui, int X, int Y, int W, int H) {
        Ui.hit(X, Y, W, H, null, null, null);
        Ui.box(X, Y, W, H, OsLayout.Phosphor, OsLayout.PhosphorDark);
        Ui.heading(Component.translatable(EditId == 0 ? Prefix + "create" : Prefix + "edit").getString(), X + 5, Y + 4, W - 10);
        int LabelX = X + 6;
        int FieldX = X + 52;
        int RowY = Y + 18;
        Ui.text(Component.translatable(Prefix + "title").getString(), LabelX, RowY + 2, OsLayout.PhosphorTitle);
        Ui.field(Title, FieldX, RowY, W - 58, Component.translatable(Prefix + "title.hint"), null);
        RowY += 17;
        Ui.text(Component.translatable(Prefix + "port").getString(), LabelX, RowY + 2, OsLayout.PhosphorTitle);
        Ui.field(Port, FieldX, RowY, 36, null, Component.translatable(Prefix + "port.hint"));
        Ui.text(Component.translatable(Prefix + "port.range").getString(), FieldX + 42, RowY + 2, OsLayout.PhosphorFaint);
        RowY += 17;
        Ui.text(Component.translatable(Prefix + "mode").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
        Ui.toggle(FieldX, RowY, 50, OsLayout.ButtonHeight, Component.translatable(Prefix + "toggle"), Kind == RedstoneButton.Mode.Toggle,
                () -> Kind = RedstoneButton.Mode.Toggle, Component.translatable(Prefix + "toggle.hint"));
        Ui.toggle(FieldX + 54, RowY, 50, OsLayout.ButtonHeight, Component.translatable(Prefix + "pulse"), Kind == RedstoneButton.Mode.Pulse,
                () -> Kind = RedstoneButton.Mode.Pulse, Component.translatable(Prefix + "pulse.hint"));
        RowY += 17;
        if (Kind == RedstoneButton.Mode.Pulse) {
            Ui.text(Component.translatable(Prefix + "length").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
            Ui.stepper(FieldX, RowY, 80, Component.translatable(Prefix + "seconds", Seconds).getString(), () -> step(-1), () -> step(1),
                    Component.translatable(Prefix + "length.hint"), Way -> step(-Way));
        }
        int ButtonY = Y + H - 17;
        Ui.button(X + 6, ButtonY, 52, OsLayout.ButtonHeight, Component.translatable(Prefix + "save"), true, this::save, null);
        if (EditId != 0) {
            Ui.button(X + 62, ButtonY, 52, OsLayout.ButtonHeight, Component.translatable(Prefix + "delete"), true, () -> {
                CompoundTag Data = new CompoundTag();
                Data.putInt("Id", EditId);
                Owner.send(ComputerAction.ButtonDelete, Data);
                Editing = false;
            }, null);
        }
        Ui.button(X + W - 58, ButtonY, 52, OsLayout.ButtonHeight, Component.translatable(Prefix + "cancel"), true, () -> {
            Editing = false;
            Owner.ui().focus(null);
        }, null);
    }

    private void step(int Way) {
        int Size = Screen.hasShiftDown() ? 10 : 1;
        Seconds = Mth.clamp(Seconds + Way * Size, RedstoneButton.MinSeconds, RedstoneButton.MaxSeconds);
    }

    private record Entry(RedstoneButton Button, boolean On, long Left) {
    }
}
