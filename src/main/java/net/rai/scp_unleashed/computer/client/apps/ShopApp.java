package net.rai.scp_unleashed.computer.client.apps;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.computer.client.ComputerScreen;
import net.rai.scp_unleashed.computer.client.OsApp;
import net.rai.scp_unleashed.computer.client.OsLayout;
import net.rai.scp_unleashed.computer.client.OsUi;
import net.rai.scp_unleashed.computer.net.ComputerCodecs;
import net.rai.scp_unleashed.computer.network.ComputerAction;
import net.rai.scp_unleashed.mining.shop.ShopKind;
import net.rai.scp_unleashed.mining.shop.ShopListing;
import net.rai.scp_unleashed.mining.shop.ShopService;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShopApp implements OsApp {
    public static final String Prefix = "gui.scp_unleashed.computer.shop.";
    public static final int ListWidth = 156;
    public static final int ListRows = 7;
    public static final int ItemRow = 20;
    public static final int CardGap = 6;
    public static final int ConfirmTicks = 60;
    public static final int DescriptionRows = 5;

    private final List<ShopListing> Entries = new ArrayList<>();
    private int Banks;
    private int Offline;
    private long Balance;
    private boolean Synced;
    @Nullable
    private ResourceLocation Selected;
    @Nullable
    private ResourceLocation Armed;
    private int ArmedAt;
    private int Scroll;

    @Override
    public void accept(CompoundTag Data, HolderLookup.Provider Registries) {
        Synced = true;
        Banks = Data.getInt("Banks");
        Offline = Data.getInt("Offline");
        Balance = Data.getLong("Balance");
        Entries.clear();
        Entries.addAll(ComputerCodecs.decodeList(ShopListing.ListingCodec, Data, "Entries", Registries));
        if (selected() == null) {
            Selected = Entries.isEmpty() ? null : Entries.getFirst().Id();
        }
    }

    @Override
    public void opened() {
        Armed = null;
    }

    @Override
    public boolean back() {
        if (Armed != null) {
            Armed = null;
            return true;
        }
        return false;
    }

    @Nullable
    private ShopListing selected() {
        for (ShopListing Entry : Entries) {
            if (Entry.Id().equals(Selected)) {
                return Entry;
            }
        }
        return null;
    }

    @Override
    public void render(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H) {
        if (Armed != null && Screen.ticks() - ArmedAt > ConfirmTicks) {
            Armed = null;
        }
        Ui.heading(Component.translatable(Prefix + "catalogue", Entries.size()).getString(), X, Y + 1, ListWidth);
        int Top = Y + 13;
        int MaxScroll = Math.max(0, Entries.size() - ListRows);
        Scroll = Math.max(0, Math.min(Scroll, MaxScroll));
        Ui.wheel(X, Top, ListWidth, ListRows * ItemRow, Step -> Scroll = Math.max(0, Math.min(MaxScroll, Scroll + Step)));
        if (Entries.isEmpty()) {
            Ui.clipped(Component.translatable(Synced ? Prefix + "empty" : Prefix + "loading").getString(), X + 2, Top + 4, ListWidth - 8, OsLayout.PhosphorDim);
        }
        for (int Row = 0; Row < ListRows && Scroll + Row < Entries.size(); Row++) {
            drawRow(Screen, Ui, Entries.get(Scroll + Row), X, Top + Row * ItemRow);
        }
        Ui.scrollbar(X + ListWidth - 4, Top, ListRows * ItemRow, Scroll, Entries.size(), ListRows);
        drawCard(Screen, Ui, X + ListWidth + CardGap, Y, W - ListWidth - CardGap, H);
    }

    private void drawRow(ComputerScreen Screen, OsUi Ui, ShopListing Entry, int X, int Y) {
        boolean Picked = Entry.Id().equals(Selected);
        int Width = ListWidth - 6;
        ResourceLocation Id = Entry.Id();
        Ui.row(X, Y, Width, ItemRow - 1, Picked, () -> {
            if (!Id.equals(Selected)) {
                Armed = null;
            }
            Selected = Id;
        }, null, null);
        drawIcon(Ui, Entry, X + 2, Y + 1);
        String Price = ShopService.points(Entry.Price());
        int PriceWidth = Ui.font().width(Price);
        boolean Affordable = Banks > 0 && Balance >= Entry.Price();
        Ui.clipped(Entry.Name().getString(), X + 21, Y + 2, Width - 25 - PriceWidth - 4, Ui.rowInk(Picked));
        Ui.right(Price, X + Width - 3, Y + 2, Picked ? OsLayout.PhosphorDark : Affordable ? OsLayout.PhosphorHot : OsLayout.AmberDim);
        Ui.clipped(kindLabel(Entry), X + 21, Y + 11, Width - 25, Picked ? Ui.rowDim(true) : kindInk(Entry));
    }

    private void drawIcon(OsUi Ui, ShopListing Entry, int X, int Y) {
        ItemStack Icon = Entry.Icon();
        if (Icon.isEmpty()) {
            Ui.glyph(Entry.Kind() == ShopKind.Entity ? OsLayout.GlyphLock : OsLayout.GlyphClip, X + 4, Y + 4, OsLayout.PhosphorDim);
            return;
        }
        Ui.graphics().renderItem(Icon, X, Y);
        if (Entry.Count() > 1) {
            Ui.graphics().renderItemDecorations(Ui.font(), Icon, X, Y, String.valueOf(Entry.Count()));
        }
    }

    private static String kindLabel(ShopListing Entry) {
        if (Entry.Classification().isPresent()) {
            return Entry.Classification().get().getName().getString().toUpperCase(Locale.ROOT);
        }
        if (Entry.Kind() == ShopKind.Entity) {
            return Component.translatable(Prefix + "kind.entity").getString();
        }
        return Component.translatable(Prefix + "kind.item").getString();
    }

    private static int kindInk(ShopListing Entry) {
        if (Entry.Classification().isPresent()) {
            return Entry.Classification().get().getColour();
        }
        return Entry.Kind() == ShopKind.Entity ? OsLayout.PhosphorTitle : OsLayout.PhosphorDim;
    }

    private void drawCard(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H) {
        Ui.box(X, Y, W, H, OsLayout.PhosphorFaint, 0xFF050F08);
        drawBalance(Ui, X, Y, W);
        Ui.fill(X + 4, Y + 24, W - 8, 1, OsLayout.PhosphorFaint);
        ShopListing Entry = selected();
        if (Entry == null) {
            Ui.centered(Component.translatable(Prefix + "pick").getString(), X + W / 2, Y + 80, OsLayout.PhosphorDim);
            return;
        }
        drawIcon(Ui, Entry, X + 5, Y + 29);
        String Name = Entry.Name().getString();
        List<int[]> NameLines = Ui.wrap(Name, W - 30);
        for (int Index = 0; Index < NameLines.size() && Index < 2; Index++) {
            int[] Span = NameLines.get(Index);
            String Shown = Name.substring(Span[0], Span[1]);
            if (Index == 1 && NameLines.size() > 2) {
                Shown = Ui.fit(Shown + OsUi.Ellipsis, W - 30);
            }
            Ui.text(Shown, X + 25, Y + 28 + Index * 9, OsLayout.PhosphorHot);
        }
        int LineY = Y + 48;
        String Kind = kindLabel(Entry);
        if (Entry.Kind() == ShopKind.Item && Entry.Count() > 1) {
            Kind = Component.translatable(Prefix + "kind.count", Kind, Entry.Count()).getString();
        }
        Ui.clipped(Kind, X + 5, LineY, W - 10, kindInk(Entry));
        LineY += 11;
        String Description = Entry.Description().getString();
        if (Description.isEmpty()) {
            Description = Component.translatable(Entry.Kind() == ShopKind.Entity ? Prefix + "about.entity" : Prefix + "about.item").getString();
        }
        List<int[]> Lines = Ui.wrap(Description, W - 10);
        for (int Index = 0; Index < Lines.size() && Index < DescriptionRows; Index++) {
            int[] Span = Lines.get(Index);
            String Shown = Description.substring(Span[0], Span[1]);
            if (Index == DescriptionRows - 1 && Lines.size() > DescriptionRows) {
                Shown = Ui.fit(Shown + OsUi.Ellipsis, W - 10);
            }
            Ui.text(Shown, X + 5, LineY + Index * 9, OsLayout.Phosphor);
        }
        boolean Affordable = Banks > 0 && Balance >= Entry.Price();
        int PriceY = Y + H - 44;
        Ui.text(Component.translatable(Prefix + "price").getString(), X + 5, PriceY, OsLayout.PhosphorDim);
        Ui.right(Component.translatable(Prefix + "points", ShopService.points(Entry.Price())).getString(), X + W - 5, PriceY, Affordable ? OsLayout.PhosphorHot : OsLayout.Amber);
        if (Entry.Dossier()) {
            Ui.glyph(OsLayout.GlyphDossier, X + W - 14, Y + 29, OsLayout.PhosphorTitle);
            Ui.tip(X + W - 15, Y + 28, 10, 10, Component.translatable(Prefix + "dossier"));
        }
        boolean Confirming = Entry.Id().equals(Armed);
        Component Label = Component.translatable(Confirming ? Prefix + "confirm" : Prefix + "buy");
        Component Hint = Banks <= 0 ? Component.translatable(Offline > 0 ? Prefix + "hint.offline" : Prefix + "hint.no_bank")
                : !Affordable ? Component.translatable(Prefix + "hint.funds", ShopService.points(Entry.Price() - Balance))
                : Component.translatable(Entry.Kind() == ShopKind.Entity ? Prefix + "hint.entity" : Prefix + "hint.item");
        int ButtonY = Y + H - 32;
        ResourceLocation Id = Entry.Id();
        long Price = Entry.Price();
        Ui.button(X + 5, ButtonY, W - 10, OsLayout.ButtonHeight, Label, Affordable, () -> press(Screen, Id, Price), Hint);
        String After = Affordable ? Component.translatable(Prefix + "after", ShopService.points(Balance - Entry.Price())).getString()
                : Component.translatable(Banks <= 0 ? Prefix + "locked" : Prefix + "short", ShopService.points(Math.max(0L, Entry.Price() - Balance))).getString();
        Ui.centered(Ui.fit(After, W - 10), X + W / 2, Y + H - 14, Affordable ? OsLayout.PhosphorDim : OsLayout.AmberDim);
    }

    private void drawBalance(OsUi Ui, int X, int Y, int W) {
        Ui.text(Component.translatable(Prefix + "balance").getString(), X + 5, Y + 4, OsLayout.PhosphorDim);
        int Tone = Banks > 0 ? 0 : Offline > 0 ? 1 : 2;
        Ui.lamp(X + W - 10, Y + 4, true, Tone);
        Component Status = Banks > 0 ? Component.translatable(Prefix + "banks", Banks)
                : Component.translatable(Offline > 0 ? Prefix + "bank_offline" : Prefix + "no_bank");
        Ui.tip(X + W - 12, Y + 2, 9, 9, Status);
        if (Banks > 0) {
            Ui.clipped(Component.translatable(Prefix + "points", ShopService.points(Balance)).getString(), X + 5, Y + 13, W - 10, OsLayout.PhosphorHot);
        } else {
            Ui.clipped(Status.getString().toUpperCase(Locale.ROOT), X + 5, Y + 13, W - 10, Offline > 0 ? OsLayout.Amber : OsLayout.Alert);
        }
    }

    private void press(ComputerScreen Screen, ResourceLocation Id, long Price) {
        if (!Id.equals(Armed)) {
            Armed = Id;
            ArmedAt = Screen.ticks();
            return;
        }
        Armed = null;
        CompoundTag Data = new CompoundTag();
        Data.putString("Entry", Id.toString());
        Data.putLong("Price", Price);
        Screen.send(ComputerAction.ShopBuy, Data);
    }
}
