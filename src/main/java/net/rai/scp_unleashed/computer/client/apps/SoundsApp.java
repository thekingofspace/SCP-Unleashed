package net.rai.scp_unleashed.computer.client.apps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.computer.client.ComputerScreen;
import net.rai.scp_unleashed.computer.client.OsApp;
import net.rai.scp_unleashed.computer.client.OsField;
import net.rai.scp_unleashed.computer.client.OsLayout;
import net.rai.scp_unleashed.computer.client.OsUi;
import net.rai.scp_unleashed.computer.data.ComputerText;
import net.rai.scp_unleashed.computer.data.SoundEntry;
import net.rai.scp_unleashed.computer.net.ComputerCodecs;
import net.rai.scp_unleashed.computer.network.ComputerAction;
import net.rai.scp_unleashed.speaker.SoundCatalog;
import net.rai.scp_unleashed.speaker.SoundConsoleBlockEntity;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;
import net.rai.scp_unleashed.speaker.cassette.CassetteRecording;
import net.rai.scp_unleashed.wire.WireChannels;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SoundsApp implements OsApp {
    public static final String Prefix = "gui.scp_unleashed.computer.sounds.";
    public static final int EntryHeight = 16;
    public static final int VisibleEntries = 8;
    public static final int PickerX = 150;
    public static final int PickerRows = 9;

    private final List<Row> Entries = new ArrayList<>();
    private final OsField Title = new OsField(SoundEntry.MaxTitle, true, OsField.AnyText);
    private final OsField Port = new OsField(WireChannels.PortDigits, false, OsField.Digits);
    private int Max = 16;
    private int Scroll;
    private boolean Editing;
    private int EditId;
    private ResourceLocation Sound = SoundCatalog.first().Id();
    private String Label = "";
    private boolean Loop;
    private float Volume = SoundMessages.DefaultVolume;
    private float Pitch = SoundMessages.DefaultPitch;
    private SoundCatalog.Category Shown = SoundCatalog.Category.Facility;
    private int PickScroll;
    @Nullable
    private SoundInstance Preview;
    private ComputerScreen Owner;

    @Override
    public void accept(CompoundTag Data, HolderLookup.Provider Registries) {
        Entries.clear();
        Max = Data.getInt("Max");
        ListTag Saved = Data.getList("Entries", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Each = Saved.getCompound(Index);
            ComputerCodecs.decode(SoundEntry.EntryCodec, Each, Registries).ifPresent(Entry -> Entries.add(new Row(Entry, Each.getBoolean("Playing"))));
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
        if (Editing) {
            drawEditor(Ui, X, Y, W, H);
            return;
        }
        Ui.heading(Component.translatable(Prefix + "list", Entries.size(), Max).getString(), X, Y + 1, W);
        int Top = Y + 12;
        Scroll = Math.max(0, Math.min(Scroll, Math.max(0, Entries.size() - VisibleEntries)));
        Ui.wheel(X, Top, W, VisibleEntries * EntryHeight, Step -> Scroll = Math.max(0, Math.min(Math.max(0, Entries.size() - VisibleEntries), Scroll + Step)));
        if (Entries.isEmpty()) {
            Ui.centered(Component.translatable(Prefix + "empty").getString(), X + W / 2, Top + 40, OsLayout.PhosphorDim);
            Ui.centered(Component.translatable(Prefix + "empty.hint").getString(), X + W / 2, Top + 52, OsLayout.PhosphorFaint);
        }
        for (int Index = 0; Index < VisibleEntries && Scroll + Index < Entries.size(); Index++) {
            drawEntry(Screen, Ui, Entries.get(Scroll + Index), X, Top + Index * EntryHeight, W - 6);
        }
        Ui.scrollbar(X + W - 4, Top, VisibleEntries * EntryHeight, Scroll, Entries.size(), VisibleEntries);
        Ui.button(X, Y + H - 13, 80, OsLayout.ButtonHeight, Component.translatable(Prefix + "new"), Entries.size() < Max, this::create,
                Component.translatable(Prefix + "new.hint"));
        Ui.text(Component.translatable(Prefix + "footer").getString(), X + 86, Y + H - 10, OsLayout.PhosphorFaint);
    }

    private void drawEntry(ComputerScreen Screen, OsUi Ui, Row Shown, int X, int Y, int W) {
        SoundEntry Entry = Shown.Entry();
        boolean Over = Ui.hovered(X, Y, W, EntryHeight - 1);
        Ui.box(X, Y, W, EntryHeight - 1, Shown.Playing() ? OsLayout.Phosphor : Over ? OsLayout.PhosphorTitle : OsLayout.PhosphorFaint,
                Shown.Playing() ? 0xFF1C5A30 : Over ? OsUi.ButtonHover : 0xFF071A0D);
        int Id = Entry.Id();
        Ui.hit(X, Y, W, EntryHeight - 1, () -> press(Screen, Id), () -> edit(Entry), Component.translatable(Prefix + "entry.hint"));
        Ui.glyph(Shown.Playing() && Entry.Loop() ? OsLayout.GlyphStop : OsLayout.GlyphPlay, X + 3, Y + 3, Shown.Playing() ? OsLayout.PhosphorHot : OsLayout.Phosphor);
        Ui.clipped(Entry.Title(), X + 14, Y + 4, 82, Shown.Playing() ? OsLayout.PhosphorHot : OsLayout.Phosphor);
        Ui.text("P" + ComputerText.port(Entry.Port()), X + 100, Y + 4, OsLayout.PhosphorDim);
        Ui.clipped(Entry.soundName().getString().toUpperCase(Locale.ROOT), X + 132, Y + 4, W - 132 - 32, OsLayout.PhosphorDim);
        if (Entry.Loop()) {
            Ui.glyph(OsLayout.GlyphLoop, X + W - 26, Y + 3, OsLayout.PhosphorTitle);
        }
        Ui.lamp(X + W - 10, Y + 5, Shown.Playing(), 0);
    }

    private void press(ComputerScreen Screen, int Id) {
        CompoundTag Data = new CompoundTag();
        Data.putInt("Id", Id);
        Screen.send(ComputerAction.SoundPress, Data);
    }

    private void create() {
        EditId = 0;
        Title.setValue("");
        Port.setValue("0");
        Sound = SoundCatalog.first().Id();
        Label = "";
        Loop = false;
        Volume = SoundMessages.DefaultVolume;
        Pitch = SoundMessages.DefaultPitch;
        Shown = SoundCatalog.Category.Facility;
        PickScroll = 0;
        Editing = true;
        Owner.ui().focus(Title);
    }

    private void edit(SoundEntry Entry) {
        EditId = Entry.Id();
        Title.setValue(Entry.Title());
        Port.setValue(String.valueOf(Entry.Port()));
        Sound = Entry.Sound();
        Label = Entry.Label();
        Loop = Entry.Loop();
        Volume = Entry.Volume();
        Pitch = Entry.Pitch();
        SoundCatalog.Entry Known = SoundCatalog.find(Sound);
        Shown = Known == null ? SoundCatalog.Category.Anomalous : Known.Group();
        PickScroll = 0;
        Editing = true;
        Owner.ui().focus(null);
    }

    private void save() {
        SoundEntry Draft = new SoundEntry(EditId, Title.getValue(), WireChannels.clampPort(Port.number(0)), Sound, Label, Volume, Pitch, Loop);
        Tag Encoded = ComputerCodecs.encode(SoundEntry.EntryCodec, Draft, Owner.registries());
        if (Encoded != null) {
            CompoundTag Data = new CompoundTag();
            Data.put("Sound", Encoded);
            Owner.send(ComputerAction.SoundSave, Data);
        }
        stopPreview();
        Editing = false;
        Owner.ui().focus(null);
    }

    private void drawEditor(OsUi Ui, int X, int Y, int W, int H) {
        Ui.heading(Component.translatable(EditId == 0 ? Prefix + "create" : Prefix + "edit").getString(), X, Y + 1, PickerX - 8);
        int LabelX = X;
        int FieldX = X + 44;
        int RowY = Y + 13;
        Ui.text(Component.translatable(Prefix + "title").getString(), LabelX, RowY + 2, OsLayout.PhosphorTitle);
        Ui.field(Title, FieldX, RowY, PickerX - 52, Component.translatable(Prefix + "title.hint"), null);
        RowY += 16;
        Ui.text(Component.translatable(Prefix + "port").getString(), LabelX, RowY + 2, OsLayout.PhosphorTitle);
        Ui.field(Port, FieldX, RowY, 36, null, Component.translatable(Prefix + "port.hint"));
        Ui.toggle(FieldX + 42, RowY, PickerX - 94, OsLayout.FieldHeight, Component.translatable(Prefix + "loop"), Loop, () -> Loop = !Loop,
                Component.translatable(Prefix + "loop.hint"));
        RowY += 16;
        Ui.text(Component.translatable(Prefix + "volume").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
        Ui.stepper(FieldX, RowY, PickerX - 52, Math.round(Volume * 100) + "%", () -> volume(-1), () -> volume(1), Component.translatable(Prefix + "volume.hint"),
                Way -> volume(-Way));
        RowY += 16;
        Ui.text(Component.translatable(Prefix + "pitch").getString(), LabelX, RowY + 3, OsLayout.PhosphorTitle);
        Ui.stepper(FieldX, RowY, PickerX - 52, String.format(Locale.ROOT, "%.2f", Pitch), () -> pitch(-1), () -> pitch(1), Component.translatable(Prefix + "pitch.hint"),
                Way -> pitch(-Way));
        RowY += 18;
        Ui.text(Component.translatable(Prefix + "chosen").getString(), LabelX, RowY, OsLayout.PhosphorDim);
        Ui.clipped(nameOf(Sound, Label).toUpperCase(Locale.ROOT), LabelX, RowY + 10, PickerX - 10, OsLayout.PhosphorHot);
        int ButtonY = Y + H - 13;
        Ui.button(X, ButtonY, 34, OsLayout.ButtonHeight, Component.translatable(Prefix + "test"), true, this::preview, Component.translatable(Prefix + "test.hint"));
        Ui.button(X + 36, ButtonY, 34, OsLayout.ButtonHeight, Component.translatable(Prefix + "save"), true, this::save, null);
        if (EditId != 0) {
            Ui.button(X + 72, ButtonY, 34, OsLayout.ButtonHeight, Component.translatable(Prefix + "delete"), true, () -> {
                CompoundTag Data = new CompoundTag();
                Data.putInt("Id", EditId);
                Owner.send(ComputerAction.SoundDelete, Data);
                stopPreview();
                Editing = false;
            }, null);
        }
        Ui.button(X + 108, ButtonY, 34, OsLayout.ButtonHeight, Component.translatable(Prefix + "cancel"), true, () -> {
            stopPreview();
            Editing = false;
            Owner.ui().focus(null);
        }, null);
        drawPicker(Ui, X + PickerX, Y, W - PickerX, H);
    }

    private void drawPicker(OsUi Ui, int X, int Y, int W, int H) {
        Ui.fill(X - 4, Y, 1, H, OsLayout.PhosphorFaint);
        Ui.cycler(X, Y, W - 2, OsLayout.ButtonHeight, Component.translatable(Shown.getTranslationKey()).getString(), true, () -> category(1), () -> category(-1),
                Component.translatable(Prefix + "category.hint"));
        List<Choice> Choices = choices();
        int Top = Y + 17;
        PickScroll = Math.max(0, Math.min(PickScroll, Math.max(0, Choices.size() - PickerRows)));
        Ui.wheel(X, Top, W, PickerRows * OsLayout.RowHeight, Step -> PickScroll = Math.max(0, Math.min(Math.max(0, Choices.size() - PickerRows), PickScroll + Step)));
        if (Choices.isEmpty()) {
            Ui.clipped(Component.translatable("gui.scp_unleashed.sound_console.library_empty").getString(), X + 2, Top + 4, W - 4, OsLayout.PhosphorDim);
            Ui.clipped(Component.translatable("gui.scp_unleashed.sound_console.library_hint").getString(), X + 2, Top + 14, W - 4, OsLayout.PhosphorFaint);
        }
        for (int Row = 0; Row < PickerRows && PickScroll + Row < Choices.size(); Row++) {
            Choice Option = Choices.get(PickScroll + Row);
            int RowY = Top + Row * OsLayout.RowHeight;
            boolean Picked = Option.Id().equals(Sound);
            Ui.row(X, RowY, W - 6, OsLayout.RowHeight, Picked, () -> {
                Sound = Option.Id();
                Label = Option.Label();
            }, null, null);
            Ui.clipped(Option.Name().toUpperCase(Locale.ROOT), X + 3, RowY + 2, W - 12, Ui.rowInk(Picked));
        }
        Ui.scrollbar(X + W - 4, Top, PickerRows * OsLayout.RowHeight, PickScroll, Choices.size(), PickerRows);
    }

    private void category(int Way) {
        SoundCatalog.Category[] Groups = SoundCatalog.Category.values();
        Shown = Groups[Math.floorMod(Shown.ordinal() + Way, Groups.length)];
        PickScroll = 0;
    }

    private List<Choice> choices() {
        List<Choice> Found = new ArrayList<>();
        if (Shown == SoundCatalog.Category.Anomalous) {
            for (CassetteRecording Recording : AnomalousLibrary.client()) {
                Found.add(new Choice(Recording.Sound(), Recording.Title().getString(), Recording.Title().getString()));
            }
            return Found;
        }
        for (SoundCatalog.Entry Entry : SoundCatalog.inCategory(Shown)) {
            Found.add(new Choice(Entry.Id(), Component.translatable(Entry.getTranslationKey()).getString(), ""));
        }
        return Found;
    }

    private static String nameOf(ResourceLocation Id, String Fallback) {
        SoundCatalog.Entry Known = SoundCatalog.find(Id);
        if (Known != null && !Known.isAnomalous()) {
            return Component.translatable(Known.getTranslationKey()).getString();
        }
        return Fallback.isEmpty() ? Id.getPath() : Fallback;
    }

    private void volume(int Way) {
        float Size = Screen.hasShiftDown() ? 0.5f : 0.1f;
        Volume = SoundConsoleBlockEntity.clampVolume(Math.round((Volume + Way * Size) * 100.0f) / 100.0f);
    }

    private void pitch(int Way) {
        float Size = Screen.hasShiftDown() ? 0.25f : 0.05f;
        Pitch = SoundMessages.clampPitch(Math.round((Pitch + Way * Size) * 100.0f) / 100.0f);
    }

    private void preview() {
        stopPreview();
        Preview = new SimpleSoundInstance(Sound, SoundMessages.Source, Math.min(Volume, 1.0f), Pitch, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
        Minecraft.getInstance().getSoundManager().play(Preview);
    }

    private void stopPreview() {
        if (Preview != null) {
            Minecraft.getInstance().getSoundManager().stop(Preview);
            Preview = null;
        }
    }

    private record Row(SoundEntry Entry, boolean Playing) {
    }

    private record Choice(ResourceLocation Id, String Name, String Label) {
    }
}
