package net.rai.scp_unleashed.computer.client.apps;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.archive.client.DatabaseRecords;
import net.rai.scp_unleashed.archive.database.DatabaseRecord;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.computer.client.ComputerScreen;
import net.rai.scp_unleashed.computer.client.OsApp;
import net.rai.scp_unleashed.computer.client.OsLayout;
import net.rai.scp_unleashed.computer.client.OsUi;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.net.ComputerCodecs;
import net.rai.scp_unleashed.computer.network.ComputerAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentsApp implements OsApp {
    public static final String Prefix = "gui.scp_unleashed.computer.documents.";
    public static final int ListWidth = 112;
    public static final int ListRows = 12;
    public static final int CardX = 120;
    private static final Pattern Number = Pattern.compile("(\\d+)");

    private final List<DatabaseRecord> Records = new ArrayList<>();
    private boolean Present;
    private boolean Online;
    @Nullable
    private ResourceLocation Selected;
    private int Scroll;
    private int LastClick = -100;

    @Override
    public void accept(CompoundTag Data, HolderLookup.Provider Registries) {
        Present = Data.getBoolean("Present");
        Online = Data.getBoolean("Online");
        Records.clear();
        Records.addAll(ComputerCodecs.decodeList(DatabaseRecord.RecordCodec, Data, "Records", Registries));
        Records.sort(Comparator.comparingLong(DocumentsApp::number).thenComparing(Entry -> DatabaseRecords.designation(Entry).toLowerCase(Locale.ROOT)));
        if (Selected == null && !Records.isEmpty()) {
            Selected = Records.getFirst().Document();
        }
    }

    private static long number(DatabaseRecord Entry) {
        Matcher Found = Number.matcher(DatabaseRecords.designation(Entry));
        if (!Found.find()) {
            return Long.MAX_VALUE;
        }
        try {
            return Long.parseLong(Found.group(1));
        } catch (NumberFormatException Problem) {
            return Long.MAX_VALUE;
        }
    }

    @Nullable
    private DatabaseRecord selected() {
        for (DatabaseRecord Entry : Records) {
            if (Entry.Document().equals(Selected)) {
                return Entry;
            }
        }
        return null;
    }

    @Override
    public void render(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H) {
        if (!Online) {
            Ui.glyph(OsLayout.GlyphWarn, X + W / 2 - 4, Y + 40, OsLayout.Amber);
            Ui.centered(Component.translatable(Present ? Prefix + "unpowered" : Prefix + "no_server").getString(), X + W / 2, Y + 52, OsLayout.Amber);
            for (int Line = 0; Line < 2; Line++) {
                Ui.centered(Component.translatable(Prefix + "no_server.hint." + Line).getString(), X + W / 2, Y + 66 + Line * 10, OsLayout.PhosphorDim);
            }
            return;
        }
        Ui.heading(Component.translatable(Prefix + "archive", Records.size()).getString(), X, Y + 1, ListWidth);
        int Top = Y + 12;
        Scroll = Math.max(0, Math.min(Scroll, Math.max(0, Records.size() - ListRows)));
        Ui.wheel(X, Top, ListWidth, ListRows * OsLayout.RowHeight, Step -> Scroll = Math.max(0, Math.min(Math.max(0, Records.size() - ListRows), Scroll + Step)));
        if (Records.isEmpty()) {
            Ui.clipped(Component.translatable(Prefix + "empty").getString(), X + 2, Top + 4, ListWidth - 4, OsLayout.PhosphorDim);
        }
        for (int Row = 0; Row < ListRows && Scroll + Row < Records.size(); Row++) {
            DatabaseRecord Entry = Records.get(Scroll + Row);
            int RowY = Top + Row * OsLayout.RowHeight;
            boolean Picked = Entry.Document().equals(Selected);
            ResourceLocation Id = Entry.Document();
            Ui.row(X, RowY, ListWidth - 6, OsLayout.RowHeight, Picked, () -> {
                if (Id.equals(Selected) && Screen.ticks() - LastClick < 8) {
                    Screen.openDossier(ComputerFile.of(Entry).readable());
                }
                Selected = Id;
                LastClick = Screen.ticks();
            }, null, null);
            Ui.fill(X + 2, RowY + 3, 4, 5, Picked ? OsLayout.PhosphorDark : Entry.Classification().getColour());
            Ui.clipped(DatabaseRecords.designation(Entry), X + 9, RowY + 2, ListWidth - 17, Ui.rowInk(Picked));
        }
        Ui.scrollbar(X + ListWidth - 4, Top, ListRows * OsLayout.RowHeight, Scroll, Records.size(), ListRows);
        drawCard(Screen, Ui, X + CardX, Y, W - CardX, H);
    }

    private void drawCard(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H) {
        Ui.box(X, Y, W, H, OsLayout.PhosphorFaint, 0xFF050F08);
        DatabaseRecord Entry = selected();
        if (Entry == null) {
            Ui.centered(Component.translatable(Prefix + "pick").getString(), X + W / 2, Y + H / 2 - 4, OsLayout.PhosphorDim);
            return;
        }
        AnomalyDocument Document = AnomalyDocuments.client().get(Entry.Document());
        int LineY = Y + 5;
        Ui.text(DatabaseRecords.designation(Entry), X + 6, LineY, OsLayout.PhosphorHot);
        LineY += 11;
        String Title = Document == null ? Component.translatable(Prefix + "corrupted").getString() : Document.title().getString();
        List<int[]> Lines = Ui.wrap(Title, W - 12);
        for (int Index = 0; Index < Lines.size() && Index < 3; Index++) {
            int[] Span = Lines.get(Index);
            String Shown = Title.substring(Span[0], Span[1]);
            if (Index == 2 && Lines.size() > 3) {
                Shown = Ui.fit(Shown + OsUi.Ellipsis, W - 12);
            }
            Ui.text(Shown, X + 6, LineY, OsLayout.Phosphor);
            LineY += 9;
        }
        LineY += 4;
        Ui.fill(X + 4, LineY - 2, W - 8, 1, OsLayout.PhosphorFaint);
        Ui.text(Component.translatable(Prefix + "class").getString(), X + 6, LineY + 1, OsLayout.PhosphorDim);
        int ClassX = X + 6 + Ui.font().width(Component.translatable(Prefix + "class").getString()) + 4;
        Ui.text(Entry.Classification().getName().getString().toUpperCase(Locale.ROOT), ClassX, LineY + 1, Entry.Classification().getColour());
        LineY += 11;
        if (!Entry.Archivist().isEmpty()) {
            Ui.clipped(Component.translatable(Prefix + "filed", Entry.Archivist().toUpperCase(Locale.ROOT)).getString(), X + 6, LineY, W - 12, OsLayout.PhosphorDim);
            LineY += 10;
        }
        if (Entry.Day() > 0) {
            Ui.text(Component.translatable(Prefix + "day", Entry.Day()).getString(), X + 6, LineY, OsLayout.PhosphorDim);
            LineY += 10;
        }
        int Notes = Entry.Notes().readableLines().size();
        if (Notes > 0) {
            Ui.text(Component.translatable(Prefix + "notes", Notes).getString(), X + 6, LineY, OsLayout.PhosphorDim);
        }
        int ButtonY = Y + H - 17;
        int Half = (W - 12) / 2;
        Ui.button(X + 4, ButtonY, Half, OsLayout.ButtonHeight, Component.translatable(Prefix + "read"), true,
                () -> Screen.openDossier(ComputerFile.of(Entry).readable()), Component.translatable(Prefix + "read.hint"));
        Ui.button(X + 8 + Half, ButtonY, Half, OsLayout.ButtonHeight, Component.translatable(Prefix + "save"), true, () -> {
            CompoundTag Data = new CompoundTag();
            Data.putString("Document", Entry.Document().toString());
            Screen.send(ComputerAction.DocumentSave, Data);
        }, Component.translatable(Prefix + "save.hint"));
    }
}
