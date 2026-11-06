package net.rai.scp_unleashed.computer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.archive.client.DossierScreen;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import net.rai.scp_unleashed.computer.client.apps.ChatApp;
import net.rai.scp_unleashed.computer.client.apps.DocumentsApp;
import net.rai.scp_unleashed.computer.client.apps.FilesApp;
import net.rai.scp_unleashed.computer.client.apps.RedstoneApp;
import net.rai.scp_unleashed.computer.client.apps.SchedulerApp;
import net.rai.scp_unleashed.computer.client.apps.ShopApp;
import net.rai.scp_unleashed.computer.client.apps.SoundsApp;
import net.rai.scp_unleashed.computer.data.ComputerText;
import net.rai.scp_unleashed.computer.net.ComputerApp;
import net.rai.scp_unleashed.computer.net.ComputerSection;
import net.rai.scp_unleashed.computer.network.ComputerAction;
import net.rai.scp_unleashed.computer.network.ComputerActionPayload;
import net.rai.scp_unleashed.computer.network.ComputerNoticePayload;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComputerScreen extends Screen {
    public static final double MaxDistanceSqr = 64.0;
    public static final int NoticeTicks = 70;
    public static final int LostTicks = 30;
    public static final int SilenceTicks = 120;
    public static final String Prefix = "gui.scp_unleashed.computer.";
    public static final List<ComputerApp> DesktopApps = List.of(ComputerApp.Files, ComputerApp.Chat, ComputerApp.Documents, ComputerApp.Redstone,
            ComputerApp.Sounds, ComputerApp.Scheduler, ComputerApp.Shop);
    private static final Map<BlockPos, Map<String, Long>> SeenByComputer = new HashMap<>();

    private final BlockPos Pos;
    private final OsUi Ui = new OsUi();
    private final Map<ComputerApp, OsApp> Apps = new EnumMap<>(ComputerApp.class);
    private final Map<String, Long> ChannelLast = new HashMap<>();
    private final Map<String, Long> Seen;
    private ComputerApp App = ComputerApp.Desktop;
    private int Left;
    private int Top;
    private int Ticks;
    private boolean Synced;
    private int Clock;
    private long Day = 1;
    private boolean ChatOnline;
    private boolean DocsOnline;
    private int Drives;
    private int Computers;
    @Nullable
    private Component Notice;
    private int NoticeTone;
    private int NoticeUntil;
    private int DriveUntil;
    private int LostAt = -1;
    private int LostReason;
    private int LastSync;
    private boolean Suspended;

    public ComputerScreen(BlockPos Pos) {
        super(Component.translatable(Prefix + "title"));
        this.Pos = Pos.immutable();
        this.Seen = SeenByComputer.computeIfAbsent(this.Pos, Key -> new HashMap<>());
        Apps.put(ComputerApp.Files, new FilesApp());
        Apps.put(ComputerApp.Chat, new ChatApp());
        Apps.put(ComputerApp.Documents, new DocumentsApp());
        Apps.put(ComputerApp.Redstone, new RedstoneApp());
        Apps.put(ComputerApp.Sounds, new SoundsApp());
        Apps.put(ComputerApp.Scheduler, new SchedulerApp());
        Apps.put(ComputerApp.Shop, new ShopApp());
    }

    public BlockPos getPos() {
        return Pos;
    }

    public OsUi ui() {
        return Ui;
    }

    public HolderLookup.Provider registries() {
        return Minecraft.getInstance().level == null ? RegistryAccess.EMPTY : Minecraft.getInstance().level.registryAccess();
    }

    public int ticks() {
        return Ticks;
    }

    public boolean isChatOnline() {
        return ChatOnline;
    }

    public boolean isDocsOnline() {
        return DocsOnline;
    }

    public int getClock() {
        return Clock;
    }

    @Nullable
    public OsApp app(ComputerApp Wanted) {
        return Apps.get(Wanted);
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - OsLayout.PanelWidth) / 2;
        Top = (height - OsLayout.PanelHeight) / 2;
        if (Suspended) {
            Suspended = false;
            LastSync = Ticks;
            CompoundTag Data = new CompoundTag();
            Data.putInt("App", App.ordinal());
            send(ComputerAction.App, Data);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Ticks++;
        OsApp Current = Apps.get(App);
        if (Current != null) {
            Current.tick();
        }
        if (minecraft == null || minecraft.player == null || minecraft.player.distanceToSqr(Vec3.atCenterOf(Pos)) > MaxDistanceSqr) {
            onClose();
            return;
        }
        if (LostAt >= 0 && Ticks - LostAt > LostTicks) {
            onClose();
        }
        if (LostAt < 0 && !Suspended && Ticks - LastSync > SilenceTicks) {
            lost(2);
        }
    }

    public void send(ComputerAction Action, CompoundTag Data) {
        PacketDistributor.sendToServer(new ComputerActionPayload(Pos, Action.ordinal(), Data));
    }

    public void send(ComputerAction Action) {
        send(Action, new CompoundTag());
    }

    public void openApp(ComputerApp Wanted) {
        if (Wanted == App) {
            return;
        }
        App = Wanted;
        Ui.focus(null);
        CompoundTag Data = new CompoundTag();
        Data.putInt("App", Wanted.ordinal());
        send(ComputerAction.App, Data);
        OsApp Opened = Apps.get(Wanted);
        if (Opened != null) {
            Opened.opened();
        }
    }

    public void openDossier(ItemStack Readable) {
        if (minecraft != null) {
            Suspended = true;
            minecraft.setScreen(new DossierScreen(Readable, this));
        }
    }

    public void play(SoundEvent Sound, float Pitch, float Volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, Volume));
        }
    }

    public void markSeen(String Key) {
        Long Last = ChannelLast.get(Key);
        if (Last != null) {
            Seen.put(Key, Last);
        }
    }

    public boolean isUnread(String Key) {
        return ChannelLast.getOrDefault(Key, 0L) > Seen.getOrDefault(Key, 0L);
    }

    public boolean anyUnread() {
        for (String Key : ChannelLast.keySet()) {
            if (isUnread(Key)) {
                return true;
            }
        }
        return false;
    }

    public void accept(int SectionId, CompoundTag Data, HolderLookup.Provider Registries) {
        ComputerSection Section = ComputerSection.byId(SectionId);
        LastSync = Ticks;
        if (Section == ComputerSection.System) {
            Clock = Data.getInt("Clock");
            Day = Data.getLong("Day");
            ChatOnline = Data.getBoolean("Chat");
            DocsOnline = Data.getBoolean("Docs");
            Drives = Data.getInt("Drives");
            Computers = Data.getInt("Computers");
            ChannelLast.clear();
            ListTag Channels = Data.getList("Channels", Tag.TAG_COMPOUND);
            for (int Index = 0; Index < Channels.size(); Index++) {
                CompoundTag Entry = Channels.getCompound(Index);
                ChannelLast.put(Entry.getString("Key"), Entry.getLong("Last"));
            }
            if (!Synced) {
                Synced = true;
                for (Map.Entry<String, Long> Entry : ChannelLast.entrySet()) {
                    Seen.putIfAbsent(Entry.getKey(), Entry.getValue());
                }
            }
            return;
        }
        for (Map.Entry<ComputerApp, OsApp> Entry : Apps.entrySet()) {
            if (ComputerSection.ofApp(Entry.getKey()) == Section) {
                Entry.getValue().accept(Data, Registries);
            }
        }
    }

    public void notice(Component Text, int Tone) {
        if (minecraft == null) {
            return;
        }
        if (Tone == ComputerNoticePayload.Chime) {
            play(ComputerRegistry.Message.get(), 1.0f, 0.6f);
            return;
        }
        Notice = Text;
        NoticeTone = Tone;
        NoticeUntil = Ticks + NoticeTicks;
        DriveUntil = Ticks + 16;
        play(Tone == ComputerNoticePayload.Error ? ComputerRegistry.Error.get() : ComputerRegistry.Beep.get(), 1.0f, 0.5f);
    }

    public void lost(int Reason) {
        if (LostAt < 0) {
            LostAt = Ticks;
            LostReason = Reason;
            Ui.focus(null);
            play(ComputerRegistry.Error.get(), 0.8f, 0.6f);
        }
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        Ui.begin(Graphics, font, MouseX, MouseY, Ticks);
        Ui.sprite(Left, Top, 0, 0, OsLayout.PanelWidth, OsLayout.PanelHeight);
        drawCase();
        int ScreenLeft = Left + OsLayout.ScreenX;
        int ScreenTop = Top + OsLayout.ScreenY;
        if (LostAt >= 0) {
            drawLost(ScreenLeft, ScreenTop);
        } else {
            drawBar(ScreenLeft, ScreenTop);
            if (App == ComputerApp.Desktop) {
                drawDesktop();
            } else {
                drawWindow();
            }
        }
        Ui.scanlines(Left + OsLayout.GlassX + 2, Top + OsLayout.GlassY + 2, OsLayout.GlassWidth - 4, OsLayout.GlassHeight - 4);
        Ui.end();
    }

    private void drawCase() {
        String Plate = Component.translatable(Prefix + "plate").getString();
        Ui.centered(Ui.fit(Plate, OsLayout.PlateWidth - 6), Left + OsLayout.PlateX + OsLayout.PlateWidth / 2, Top + OsLayout.PlateY + 3, OsLayout.PlateInk);
        boolean Powered = LostAt < 0 || LostReason != 1;
        drawLed(Left + OsLayout.PowerLedX, Top + OsLayout.LedY, Powered, 0xFF6CFF8C, 0xFF143A1E);
        boolean Busy = Ticks < DriveUntil && (Ticks / 2) % 2 == 0;
        drawLed(Left + OsLayout.DriveLedX, Top + OsLayout.LedY, Busy, 0xFFFFB347, 0xFF3A2610);
        Ui.tip(Left + OsLayout.PowerLedX - 1, Top + OsLayout.LedY - 1, OsLayout.LedSize + 2, OsLayout.LedSize + 2, Component.translatable(Prefix + "led.power"));
        Ui.tip(Left + OsLayout.DriveLedX - 1, Top + OsLayout.LedY - 1, OsLayout.LedSize + 2, OsLayout.LedSize + 2, Component.translatable(Prefix + "led.drive"));
    }

    private void drawLed(int X, int Y, boolean On, int Lit, int Dark) {
        Ui.fill(X, Y, OsLayout.LedSize, OsLayout.LedSize, On ? Lit : Dark);
        if (On) {
            Ui.fill(X + 1, Y + 1, 1, 1, 0xFFFFFFFF);
        }
    }

    private void drawLost(int X, int Y) {
        String Key = switch (LostReason) {
            case 1 -> "lost.power";
            case 2 -> "lost.removed";
            case 3 -> "lost.distance";
            default -> "lost.closed";
        };
        String Line = Component.translatable(Prefix + Key).getString();
        int Mid = Y + OsLayout.ScreenHeight / 2 - 4;
        if ((Ticks / 4) % 2 == 0) {
            Ui.centered(Line, X + OsLayout.ScreenWidth / 2, Mid, OsLayout.Alert);
        }
    }

    private void drawBar(int X, int Y) {
        Ui.fill(X, Y, OsLayout.ScreenWidth, OsLayout.BarHeight, OsLayout.PhosphorDark);
        Ui.fill(X, Y + OsLayout.BarHeight, OsLayout.ScreenWidth, 1, OsLayout.PhosphorFaint);
        String Os = Component.translatable(Prefix + "os").getString();
        Ui.text(Os, X + 3, Y + 2, OsLayout.PhosphorTitle);
        String Time = Component.translatable(Prefix + "clock", Day, ComputerText.clock(Clock)).getString();
        int TimeX = X + OsLayout.ScreenWidth - 3 - font.width(Time);
        int CrumbX = X + 5 + font.width(Os);
        int CrumbWidth = TimeX - 30 - CrumbX;
        if (Notice != null && Ticks <= NoticeUntil) {
            int Ink = NoticeTone == ComputerNoticePayload.Error ? OsLayout.Amber : OsLayout.PhosphorHot;
            Ui.fill(CrumbX - 2, Y + 1, Math.min(CrumbWidth, font.width(Ui.fit(Notice.getString(), CrumbWidth - 12)) + 14), OsLayout.BarHeight - 2,
                    NoticeTone == ComputerNoticePayload.Error ? 0xFF3A2A0C : 0xFF1C5A30);
            Ui.glyph(NoticeTone == ComputerNoticePayload.Error ? OsLayout.GlyphWarn : OsLayout.GlyphRight, CrumbX, Y + 1, Ink);
            Ui.clipped(Notice.getString(), CrumbX + 10, Y + 2, CrumbWidth - 12, Ink);
        } else if (App != ComputerApp.Desktop) {
            Ui.clipped("> " + Component.translatable(App.getTitleKey()).getString(), CrumbX, Y + 2, CrumbWidth, OsLayout.PhosphorDim);
        }
        Ui.text(Time, TimeX, Y + 2, OsLayout.Phosphor);
        int LampX = TimeX - 10;
        Ui.lamp(LampX, Y + 3, Drives > 0, 1);
        Ui.tip(LampX - 1, Y, 7, OsLayout.BarHeight, Component.translatable(Prefix + "status.drives", Drives));
        LampX -= 8;
        Ui.lamp(LampX, Y + 3, DocsOnline, 0);
        Ui.tip(LampX - 1, Y, 7, OsLayout.BarHeight, Component.translatable(Prefix + (DocsOnline ? "status.docs" : "status.no_docs")));
        LampX -= 8;
        Ui.lamp(LampX, Y + 3, ChatOnline, 0);
        Ui.tip(LampX - 1, Y, 7, OsLayout.BarHeight, Component.translatable(Prefix + (ChatOnline ? "status.chat" : "status.no_chat")));
    }

    private void drawDesktop() {
        int AreaX = Left + OsLayout.ScreenX;
        int AreaY = Top + OsLayout.ScreenY + OsLayout.BarHeight + 1;
        int AreaHeight = OsLayout.ScreenHeight - OsLayout.BarHeight - 1;
        Ui.sprite(AreaX + (OsLayout.ScreenWidth - OsLayout.EmblemSize) / 2, AreaY + (AreaHeight - OsLayout.EmblemSize) / 2 - 2, OsLayout.EmblemU, OsLayout.EmblemV,
                OsLayout.EmblemSize, OsLayout.EmblemSize);
        int GridWidth = OsLayout.DesktopColumns * OsLayout.DesktopCellWidth;
        int StartX = AreaX + (OsLayout.ScreenWidth - GridWidth) / 2;
        int StartY = Top + OsLayout.DesktopTop;
        int LastRow = (DesktopApps.size() - 1) / OsLayout.DesktopColumns;
        int LastRowShift = (OsLayout.DesktopColumns - (DesktopApps.size() - LastRow * OsLayout.DesktopColumns)) * OsLayout.DesktopCellWidth / 2;
        for (int Index = 0; Index < DesktopApps.size(); Index++) {
            ComputerApp Shown = DesktopApps.get(Index);
            int Row = Index / OsLayout.DesktopColumns;
            int CellX = StartX + (Index % OsLayout.DesktopColumns) * OsLayout.DesktopCellWidth + (Row == LastRow ? LastRowShift : 0);
            int CellY = StartY + Row * OsLayout.DesktopCellHeight;
            int IconX = CellX + (OsLayout.DesktopCellWidth - OsLayout.IconSize) / 2;
            boolean Over = Ui.hovered(CellX + 8, CellY, OsLayout.DesktopCellWidth - 16, OsLayout.DesktopCellHeight - 6);
            Ui.sprite(IconX, CellY + 2, iconU(Index), Over ? OsLayout.IconHoverV : OsLayout.IconV, OsLayout.IconSize, OsLayout.IconSize);
            String Label = Component.translatable(Shown.getTitleKey()).getString();
            int LabelWidth = font.width(Label);
            int LabelX = CellX + (OsLayout.DesktopCellWidth - LabelWidth) / 2;
            int LabelY = CellY + OsLayout.IconSize + 6;
            if (Over) {
                Ui.fill(LabelX - 2, LabelY - 1, LabelWidth + 4, 10, OsLayout.Phosphor);
            }
            Ui.text(Label, LabelX, LabelY, Over ? OsLayout.PhosphorDark : OsLayout.Phosphor);
            boolean Offline = Shown == ComputerApp.Chat && !ChatOnline || Shown == ComputerApp.Documents && !DocsOnline;
            if (Offline) {
                Ui.glyph(OsLayout.GlyphWarn, IconX + OsLayout.IconSize - 6, CellY, OsLayout.Amber);
            } else if (Shown == ComputerApp.Chat && anyUnread() && (Ticks / 10) % 2 == 0) {
                Ui.lamp(IconX + OsLayout.IconSize - 4, CellY + 1, true, 1);
            }
            Ui.hit(CellX + 8, CellY, OsLayout.DesktopCellWidth - 16, OsLayout.DesktopCellHeight - 6, () -> {
                play(ComputerRegistry.Key.get(), 1.0f, 0.35f);
                openApp(Shown);
            }, null, Component.translatable(Shown.getTitleKey() + ".hint"));
        }
        String Footer = Component.translatable(Prefix + "footer", Computers, Drives).getString();
        Ui.centered(Footer, AreaX + OsLayout.ScreenWidth / 2, Top + OsLayout.ScreenY + OsLayout.ScreenHeight - 11, OsLayout.PhosphorDim);
    }

    public static int iconU(int Index) {
        return Index < OsLayout.BaseIcons ? OsLayout.IconU + Index * OsLayout.IconSize : OsLayout.ExtraIconU + (Index - OsLayout.BaseIcons) * OsLayout.IconSize;
    }

    private void drawWindow() {
        int X = Left + OsLayout.WindowX;
        int Y = Top + OsLayout.WindowY;
        Ui.box(X, Y, OsLayout.WindowWidth, OsLayout.WindowHeight, OsLayout.PhosphorDim, OsLayout.GlassDeep);
        Ui.fill(X + 1, Y + 1, OsLayout.WindowWidth - 2, OsLayout.TitleHeight, OsLayout.PhosphorDim);
        Ui.text(Component.translatable(App.getTitleKey()).getString(), X + 4, Y + 3, OsLayout.PhosphorDark);
        Ui.glyphButton(X + OsLayout.WindowWidth - 13, Y + 1, 12, OsLayout.TitleHeight, OsLayout.GlyphClose, true, () -> {
            OsApp Current = Apps.get(App);
            if (Current == null || !Current.back()) {
                openApp(ComputerApp.Desktop);
            }
        }, Component.translatable(Prefix + "close"));
        OsApp Current = Apps.get(App);
        if (Current != null) {
            Current.render(this, Ui, Left + OsLayout.BodyX, Top + OsLayout.BodyY, OsLayout.BodyWidth, OsLayout.BodyHeight);
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (LostAt >= 0) {
            onClose();
            return true;
        }
        Ui.focus(null);
        if (Ui.click(MouseX, MouseY, Button)) {
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        if (ScrollY != 0.0 && Ui.scroll(MouseX, MouseY, ScrollY)) {
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        OsField Focused = Ui.focus();
        if (Key == GLFW.GLFW_KEY_ESCAPE) {
            if (Focused != null) {
                Ui.focus(null);
                return true;
            }
            OsApp Current = Apps.get(App);
            if (Current != null && Current.back()) {
                return true;
            }
            if (App != ComputerApp.Desktop) {
                openApp(ComputerApp.Desktop);
                return true;
            }
            onClose();
            return true;
        }
        if (Focused != null) {
            if (Focused.keyPressed(Key)) {
                play(ComputerRegistry.Key.get(), 0.95f + font.width(Focused.getValue()) % 7 * 0.02f, 0.25f);
                return true;
            }
            if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER) {
                OsApp Current = Apps.get(App);
                if (Current != null && Current.submit(Focused)) {
                    return true;
                }
                Ui.focus(null);
                return true;
            }
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
            play(ComputerRegistry.Key.get(), 0.9f + (Letter % 5) * 0.05f, 0.25f);
            return true;
        }
        return super.charTyped(Letter, Modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        if (!Suspended) {
            send(ComputerAction.Close);
        }
    }
}
