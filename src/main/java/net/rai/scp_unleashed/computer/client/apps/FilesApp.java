package net.rai.scp_unleashed.computer.client.apps;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.computer.PortMedia;
import net.rai.scp_unleashed.computer.client.ComputerScreen;
import net.rai.scp_unleashed.computer.client.OsApp;
import net.rai.scp_unleashed.computer.client.OsField;
import net.rai.scp_unleashed.computer.client.OsLayout;
import net.rai.scp_unleashed.computer.client.OsUi;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.net.ComputerApp;
import net.rai.scp_unleashed.computer.net.ComputerCodecs;
import net.rai.scp_unleashed.computer.network.ComputerAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilesApp implements OsApp {
    public static final String Prefix = "gui.scp_unleashed.computer.files.";
    public static final int DriveWidth = 72;
    public static final int ListX = 79;
    public static final int ListWidth = 205;
    public static final int ListRows = 11;
    public static final int ButtonY = 142;

    private final List<ComputerFile> Local = new ArrayList<>();
    private final List<Drive> Drives = new ArrayList<>();
    private final OsField RenameField = new OsField(ComputerFile.MaxName, false, OsField.AnyText);
    private final OsField NoteTitle = new OsField(ComputerFile.MaxName, false, OsField.AnyText);
    private final OsField NoteBody = new OsField(ComputerFile.MaxText, false, OsField.AnyText, true);
    private int Capacity = 24;
    private long DrivePos = Long.MIN_VALUE;
    private long WritePos = Long.MIN_VALUE;
    private int Selected = -1;
    private int Scroll;
    private Mode State = Mode.Browse;
    private int EditIndex = -1;
    private int LastClick = -100;
    @Nullable
    private ComputerFile Reading;
    private ComputerScreen Owner;

    @Override
    public void accept(CompoundTag Data, HolderLookup.Provider Registries) {
        Local.clear();
        Local.addAll(ComputerCodecs.decodeList(ComputerFile.FileCodec, Data, "Local", Registries));
        Capacity = Data.getInt("Capacity");
        Drives.clear();
        ListTag Saved = Data.getList("Drives", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Entry = Saved.getCompound(Index);
            Drives.add(new Drive(Entry.getLong("Pos"), PortMedia.byId(Entry.getInt("Media")), Entry.getInt("Capacity"), Entry.getBoolean("Writable"),
                    Entry.getString("Label"), ComputerCodecs.decodeList(ComputerFile.FileCodec, Entry, "Files", Registries)));
        }
        if (DrivePos != Long.MIN_VALUE && drive() == null) {
            DrivePos = Long.MIN_VALUE;
            Selected = -1;
        }
        Selected = Math.min(Selected, files().size() - 1);
    }

    @Nullable
    private Drive drive() {
        for (Drive Each : Drives) {
            if (Each.Pos() == DrivePos) {
                return Each;
            }
        }
        return null;
    }

    @Nullable
    private Drive writeTarget() {
        Drive Fallback = null;
        for (Drive Each : Drives) {
            if (!Each.Writable()) {
                continue;
            }
            if (Each.Pos() == WritePos) {
                return Each;
            }
            if (Fallback == null) {
                Fallback = Each;
            }
        }
        return Fallback;
    }

    private List<ComputerFile> files() {
        Drive Current = drive();
        return Current == null ? Local : Current.Files();
    }

    @Override
    public boolean back() {
        if (Reading != null) {
            Reading = null;
            return true;
        }
        if (State != Mode.Browse) {
            State = Mode.Browse;
            return true;
        }
        return false;
    }

    @Override
    public boolean submit(OsField Field) {
        if (Field == RenameField) {
            confirmRename();
            return true;
        }
        if (Field == NoteTitle) {
            Owner.ui().focus(NoteBody);
            return true;
        }
        return false;
    }

    @Override
    public void render(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H) {
        Owner = Screen;
        drawDrives(Ui, X, Y, H);
        Ui.fill(X + DriveWidth + 3, Y, 1, H, OsLayout.PhosphorFaint);
        if (State == Mode.Note) {
            drawEditor(Ui, X + ListX - 2, Y, W - ListX + 2, H);
            return;
        }
        drawList(Screen, Ui, X + ListX - 2, Y, ListWidth + 6);
        if (State == Mode.Rename) {
            drawRename(Ui, X, Y + ButtonY);
        } else {
            drawButtons(Screen, Ui, X, Y + ButtonY, W);
        }
        if (Reading != null && Reading.isImage()) {
            ImageView.draw(Ui, X + ListX - 2, Y, ListWidth + 7, ButtonY - 4, Reading, () -> Reading = null);
        } else if (Reading != null) {
            NoteView.draw(Ui, X + ListX - 2, Y, ListWidth + 7, ButtonY - 4, Reading, () -> Reading = null);
        }
    }

    private void drawDrives(OsUi Ui, int X, int Y, int H) {
        Ui.heading(Component.translatable(Prefix + "drives").getString(), X, Y + 1, DriveWidth);
        int RowY = Y + 12;
        boolean LocalSelected = DrivePos == Long.MIN_VALUE;
        Ui.row(X, RowY, DriveWidth, OsLayout.RowHeight, LocalSelected, () -> pick(Long.MIN_VALUE), null, Component.translatable(Prefix + "local.hint", Local.size(), Capacity));
        Ui.glyph(OsLayout.GlyphDrive, X + 1, RowY + 1, Ui.rowDim(LocalSelected));
        Ui.text(Component.translatable(Prefix + "local").getString(), X + 11, RowY + 2, Ui.rowInk(LocalSelected));
        Ui.right(Local.size() + "/" + Capacity, X + DriveWidth - 2, RowY + 2, Ui.rowDim(LocalSelected));
        RowY += OsLayout.RowHeight + 2;
        for (int Index = 0; Index < Drives.size() && RowY + OsLayout.RowHeight * 2 < Y + H; Index++) {
            Drive Each = Drives.get(Index);
            boolean Picked = Each.Pos() == DrivePos;
            long Pos = Each.Pos();
            Component Hint = Component.translatable(Prefix + "drive.hint." + Each.Media().getSerializedName(), Each.Files().size(), Each.Capacity());
            Ui.row(X, RowY, DriveWidth, OsLayout.RowHeight * 2, Picked, () -> pick(Pos), null, Hint);
            int Glyph = Each.Media() == PortMedia.Floppy ? OsLayout.GlyphFloppy : Each.Media() == PortMedia.Dossier ? OsLayout.GlyphDossier : OsLayout.GlyphPort;
            Ui.glyph(Glyph, X + 1, RowY + 1, Ui.rowDim(Picked));
            String Name = Each.Label().isEmpty() ? Component.translatable(Prefix + "port", Index + 1).getString() : Each.Label().toUpperCase(Locale.ROOT);
            Ui.clipped(Name, X + 11, RowY + 2, DriveWidth - 13, Ui.rowInk(Picked));
            String Media = Component.translatable(Prefix + "media." + Each.Media().getSerializedName()).getString();
            String Count = Each.Media() == PortMedia.Floppy ? " " + Each.Files().size() + "/" + Each.Capacity() : "";
            Ui.clipped(Media + Count, X + 11, RowY + 11, DriveWidth - 13, Ui.rowDim(Picked));
            RowY += OsLayout.RowHeight * 2 + 1;
        }
        if (Drives.isEmpty()) {
            Ui.clipped(Component.translatable(Prefix + "no_ports").getString(), X + 2, RowY + 2, DriveWidth - 2, OsLayout.PhosphorFaint);
        }
    }

    private void pick(long Pos) {
        DrivePos = Pos;
        Selected = -1;
        Scroll = 0;
        State = Mode.Browse;
        Drive Current = drive();
        if (Current != null && Current.Writable()) {
            WritePos = Pos;
        }
    }

    private void drawList(ComputerScreen Screen, OsUi Ui, int X, int Y, int W) {
        List<ComputerFile> Shown = files();
        Ui.text(Component.translatable(Prefix + "name").getString(), X + 2, Y + 1, OsLayout.PhosphorTitle);
        Ui.right(Component.translatable(Prefix + "type").getString(), X + W - 8, Y + 1, OsLayout.PhosphorTitle);
        Ui.fill(X, Y + 10, W, 1, OsLayout.PhosphorFaint);
        int Top = Y + 12;
        Scroll = Math.max(0, Math.min(Scroll, Math.max(0, Shown.size() - ListRows)));
        Ui.wheel(X, Top, W, ListRows * OsLayout.RowHeight, Step -> Scroll = Math.max(0, Math.min(Math.max(0, Shown.size() - ListRows), Scroll + Step)));
        if (Shown.isEmpty()) {
            Drive Current = drive();
            String Key = Current == null ? "empty.local" : Current.Media() == PortMedia.Empty ? "empty.port" : "empty.disc";
            Ui.centered(Component.translatable(Prefix + Key).getString(), X + W / 2, Top + 40, OsLayout.PhosphorDim);
        }
        for (int Row = 0; Row < ListRows && Scroll + Row < Shown.size(); Row++) {
            int Index = Scroll + Row;
            ComputerFile File = Shown.get(Index);
            int RowY = Top + Row * OsLayout.RowHeight;
            boolean Picked = Index == Selected;
            Ui.row(X, RowY, W - 6, OsLayout.RowHeight, Picked, () -> {
                if (Selected == Index && Screen.ticks() - LastClick < 8) {
                    view(Screen, File);
                }
                Selected = Index;
                LastClick = Screen.ticks();
            }, null, null);
            Ui.glyph(File.isDossier() ? OsLayout.GlyphDossier : OsLayout.GlyphNote, X + 2, RowY + 1, Picked ? OsLayout.PhosphorDark : File.isDossier()
                    ? File.Classification().getColour() : OsLayout.PhosphorTitle);
            Ui.clipped(File.Name(), X + 13, RowY + 2, W - 72, Ui.rowInk(Picked));
            String Type = File.isDossier() ? File.Classification().getName().getString().toUpperCase(Locale.ROOT)
                    : Component.translatable(Prefix + (File.isImage() ? "image" : "note")).getString();
            Ui.right(Ui.fit(Type, 52), X + W - 9, RowY + 2, Picked ? OsLayout.PhosphorDark : File.isDossier() ? File.Classification().getColour() : OsLayout.PhosphorDim);
        }
        Ui.scrollbar(X + W - 4, Top, ListRows * OsLayout.RowHeight, Scroll, Shown.size(), ListRows);
    }


    private void drawButtons(ComputerScreen Screen, OsUi Ui, int X, int Y, int W) {
        List<ComputerFile> Shown = files();
        ComputerFile File = Selected >= 0 && Selected < Shown.size() ? Shown.get(Selected) : null;
        Drive Current = drive();
        if (Current == null) {
            int Width = 46;
            int Gap = 2;
            Drive Target = writeTarget();
            Ui.button(X, Y, Width, OsLayout.ButtonHeight, Component.translatable(Prefix + "view"), File != null, () -> view(Screen, File),
                    Component.translatable(Prefix + "view.hint"));
            Ui.button(X + (Width + Gap), Y, Width, OsLayout.ButtonHeight, Component.translatable(Prefix + "rename"), File != null, this::startRename,
                    Component.translatable(Prefix + "rename.hint"));
            Ui.button(X + (Width + Gap) * 2, Y, Width, OsLayout.ButtonHeight, Component.translatable(Prefix + "delete"), File != null,
                    () -> fileAction(ComputerAction.FileDelete), Component.translatable(Prefix + "delete.hint"));
            Ui.button(X + (Width + Gap) * 3, Y, Width, OsLayout.ButtonHeight, Component.translatable(Prefix + "write"), File != null && Target != null,
                    () -> write(Target), Target == null ? Component.translatable(Prefix + "write.none")
                            : Component.translatable(Prefix + "write.hint", label(Target), Target.Files().size(), Target.Capacity()));
            Ui.button(X + (Width + Gap) * 4, Y, Width, OsLayout.ButtonHeight, Component.translatable(Prefix + "send"), File != null && Screen.isChatOnline(),
                    () -> sendToChat(Screen), Component.translatable(Prefix + "send.hint"));
            Ui.button(X + (Width + Gap) * 5, Y, Width, OsLayout.ButtonHeight, Component.translatable(Prefix + "new"), Local.size() < Capacity, () -> startNote(-1),
                    Component.translatable(Prefix + "new.hint"));
            return;
        }
        int Width = 70;
        int Gap = 2;
        boolean Floppy = Current.Writable();
        Ui.button(X, Y, Width, OsLayout.ButtonHeight, Component.translatable(Prefix + "view"), File != null, () -> view(Screen, File),
                Component.translatable(Prefix + "view.hint"));
        Ui.button(X + Width + Gap, Y, Width, OsLayout.ButtonHeight, Component.translatable(Prefix + "copy"), File != null && Local.size() < Capacity,
                () -> driveAction(ComputerAction.FileImport, Current), Component.translatable(Prefix + "copy.hint"));
        Ui.button(X + (Width + Gap) * 2, Y, Width, OsLayout.ButtonHeight, Component.translatable(Prefix + "rename"), File != null && Floppy, this::startRename,
                Component.translatable(Floppy ? Prefix + "rename.hint" : Prefix + "read_only"));
        Ui.button(X + (Width + Gap) * 3, Y, Width, OsLayout.ButtonHeight, Component.translatable(Prefix + "delete"), File != null && Floppy,
                () -> driveAction(ComputerAction.DriveDelete, Current), Component.translatable(Floppy ? Prefix + "delete.disc" : Prefix + "read_only"));
    }

    private String label(Drive Target) {
        int Index = Drives.indexOf(Target);
        return Target.Label().isEmpty() ? Component.translatable(Prefix + "port", Index + 1).getString() : Target.Label().toUpperCase(Locale.ROOT);
    }

    private void view(ComputerScreen Screen, @Nullable ComputerFile File) {
        if (File == null) {
            return;
        }
        if (File.isDossier()) {
            Screen.openDossier(File.readable());
        } else if (File.isImage()) {
            Reading = File;
            CompoundTag Data = new CompoundTag();
            Drive Current = drive();
            if (Current != null) {
                Data.putLong("Drive", Current.Pos());
            }
            Data.putInt("Index", Selected);
            Screen.send(ComputerAction.ImageView, Data);
        } else if (drive() == null) {
            startNote(Selected);
        } else {
            Reading = File;
        }
    }

    private void fileAction(ComputerAction Action) {
        CompoundTag Data = new CompoundTag();
        Data.putInt("Index", Selected);
        Owner.send(Action, Data);
        Selected = -1;
    }

    private void driveAction(ComputerAction Action, Drive Target) {
        CompoundTag Data = new CompoundTag();
        Data.putLong("Drive", Target.Pos());
        Data.putInt("Index", Selected);
        Owner.send(Action, Data);
        if (Action == ComputerAction.DriveDelete) {
            Selected = -1;
        }
    }

    private void write(@Nullable Drive Target) {
        if (Target == null) {
            return;
        }
        CompoundTag Data = new CompoundTag();
        Data.putLong("Drive", Target.Pos());
        Data.putInt("Index", Selected);
        Owner.send(ComputerAction.FileExport, Data);
    }

    private void sendToChat(ComputerScreen Screen) {
        if (Screen.app(ComputerApp.Chat) instanceof ChatApp Chat) {
            Chat.attach(Selected);
        }
        Screen.openApp(ComputerApp.Chat);
    }

    private void startRename() {
        List<ComputerFile> Shown = files();
        if (Selected < 0 || Selected >= Shown.size()) {
            return;
        }
        RenameField.setValue(Shown.get(Selected).Name());
        State = Mode.Rename;
        Owner.ui().focus(RenameField);
    }

    private void confirmRename() {
        String Name = RenameField.getValue().strip();
        if (Name.isEmpty()) {
            return;
        }
        CompoundTag Data = new CompoundTag();
        Data.putInt("Index", Selected);
        Data.putString("Name", Name);
        Drive Current = drive();
        if (Current == null) {
            Owner.send(ComputerAction.FileRename, Data);
        } else {
            Data.putLong("Drive", Current.Pos());
            Owner.send(ComputerAction.DriveRename, Data);
        }
        State = Mode.Browse;
        Owner.ui().focus(null);
    }

    private void drawRename(OsUi Ui, int X, int Y) {
        Ui.text(Component.translatable(Prefix + "rename.label").getString(), X, Y + 3, OsLayout.PhosphorTitle);
        Ui.field(RenameField, X + 40, Y, 150, null, null);
        Ui.button(X + 196, Y, 44, OsLayout.ButtonHeight, Component.translatable(Prefix + "ok"), !RenameField.getValue().isBlank(), this::confirmRename, null);
        Ui.button(X + 244, Y, 44, OsLayout.ButtonHeight, Component.translatable(Prefix + "cancel"), true, () -> {
            State = Mode.Browse;
            Owner.ui().focus(null);
        }, null);
    }

    private void startNote(int Index) {
        EditIndex = Index;
        if (Index >= 0 && Index < Local.size()) {
            NoteTitle.setValue(Local.get(Index).Name());
            NoteBody.setValue(Local.get(Index).Text());
        } else {
            NoteTitle.setValue("");
            NoteBody.setValue("");
        }
        State = Mode.Note;
        Owner.ui().focus(Index >= 0 ? NoteBody : NoteTitle);
    }

    private void drawEditor(OsUi Ui, int X, int Y, int W, int H) {
        Ui.text(Component.translatable(Prefix + "note.title").getString(), X + 2, Y + 3, OsLayout.PhosphorTitle);
        Ui.field(NoteTitle, X + 34, Y, W - 34, Component.translatable(Prefix + "note.untitled"), null);
        Ui.area(NoteBody, X, Y + 15, W, H - 34, Component.translatable(Prefix + "note.hint"));
        Ui.text(NoteBody.getValue().length() + "/" + ComputerFile.MaxText, X + 2, Y + H - 13 + 3, OsLayout.PhosphorDim);
        Ui.button(X + W - 92, Y + H - 13, 44, OsLayout.ButtonHeight, Component.translatable(Prefix + "save"), true, () -> {
            CompoundTag Data = new CompoundTag();
            Data.putInt("Index", EditIndex);
            Data.putString("Name", NoteTitle.getValue().isBlank() ? Component.translatable(Prefix + "note.untitled").getString() : NoteTitle.getValue());
            Data.putString("Text", NoteBody.getValue());
            Owner.send(ComputerAction.NoteSave, Data);
            State = Mode.Browse;
            Owner.ui().focus(null);
        }, null);
        Ui.button(X + W - 46, Y + H - 13, 46, OsLayout.ButtonHeight, Component.translatable(Prefix + "cancel"), true, () -> {
            State = Mode.Browse;
            Owner.ui().focus(null);
        }, null);
    }

    private enum Mode {
        Browse,
        Rename,
        Note
    }

    private record Drive(long Pos, PortMedia Media, int Capacity, boolean Writable, String Label, List<ComputerFile> Files) {
    }
}
