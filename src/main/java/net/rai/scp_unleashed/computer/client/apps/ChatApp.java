package net.rai.scp_unleashed.computer.client.apps;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import net.rai.scp_unleashed.computer.client.ComputerScreen;
import net.rai.scp_unleashed.computer.client.OsApp;
import net.rai.scp_unleashed.computer.client.OsField;
import net.rai.scp_unleashed.computer.client.OsLayout;
import net.rai.scp_unleashed.computer.client.OsUi;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.data.ComputerText;
import net.rai.scp_unleashed.computer.net.ComputerCodecs;
import net.rai.scp_unleashed.computer.network.ComputerAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChatApp implements OsApp {
    public static final String Prefix = "gui.scp_unleashed.computer.chat.";
    public static final int ChannelWidth = 72;
    public static final int MessageX = 79;
    public static final int MessageWidth = 201;
    public static final int MessageHeight = 136;
    public static final int LineHeight = 9;
    public static final int InputY = 142;
    public static final int InputWidth = 150;
    public static final int AttachX = 232;
    public static final int SendX = 248;
    public static final int SendWidth = 40;
    public static final int PatternLines = 2;

    private final OsField Input = new OsField(ChatMessage.MaxText, false, OsField.AnyText);
    private final List<ChatChannels.Info> Channels = new ArrayList<>();
    private final List<ChatMessage> Messages = new ArrayList<>();
    private final List<ComputerFile> Local = new ArrayList<>();
    private boolean Online;
    private String Channel = ChatChannels.Network;
    private int Scroll;
    private int Attach = -1;
    private boolean Picking;
    private int PickScroll;
    @Nullable
    private ComputerFile Reading;
    private int ReportedScroll = -1;
    private int ReportedVisible = -1;
    private long LastSeenId;
    private String LastSeenChannel = "";
    private ComputerScreen Owner;

    @Override
    public void accept(CompoundTag Data, HolderLookup.Provider Registries) {
        Online = Data.getBoolean("Online");
        Channels.clear();
        Channels.addAll(ComputerCodecs.decodeList(ChatChannels.Info.InfoCodec, Data, "Channels", Registries));
        String Key = Data.getString("Channel");
        List<ChatMessage> Incoming = ComputerCodecs.decodeList(ChatMessage.MessageCodec, Data, "Messages", Registries);
        Local.clear();
        Local.addAll(ComputerCodecs.decodeList(ComputerFile.FileCodec, Data, "Local", Registries));
        if (Attach >= Local.size()) {
            Attach = -1;
        }
        if (!Key.equals(Channel)) {
            Channel = Key;
            Scroll = 0;
        }
        long Newest = Incoming.isEmpty() ? 0L : Incoming.getLast().Id();
        boolean Arrived = Owner != null && Newest > LastSeenId && LastSeenId > 0 && !Incoming.isEmpty() && Key.equals(LastSeenChannel);
        if (Arrived && Scroll > 0) {
            Scroll = Math.min(Scroll + 1, Math.max(0, Incoming.size() - 1));
        }
        if (Arrived && !mine(Incoming.getLast())) {
            Owner.play(ComputerRegistry.Message.get(), 1.0f, 0.5f);
        }
        LastSeenId = Newest;
        LastSeenChannel = Key;
        Messages.clear();
        Messages.addAll(Incoming);
        Scroll = Math.min(Scroll, Math.max(0, Messages.size() - 1));
        if (Owner != null) {
            Owner.markSeen(Channel);
        }
    }

    private static boolean mine(ChatMessage Message) {
        Minecraft Game = Minecraft.getInstance();
        return Message.From() == ChatMessage.Sender.Player && Game.player != null && Game.player.getGameProfile().getName().equals(Message.Name());
    }

    public void attach(int Index) {
        Attach = Index;
        Picking = false;
    }

    @Override
    public void opened() {
        ReportedScroll = -1;
        ReportedVisible = -1;
    }

    @Override
    public boolean back() {
        if (Reading != null) {
            Reading = null;
            return true;
        }
        if (Picking) {
            Picking = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean submit(OsField Field) {
        if (Field == Input) {
            send();
            return true;
        }
        return false;
    }

    private void send() {
        String Text = ComputerText.clean(Input.getValue(), ChatMessage.MaxText);
        if (Owner == null || Text.isEmpty() && Attach < 0) {
            return;
        }
        CompoundTag Data = new CompoundTag();
        Data.putString("Key", Channel);
        Data.putString("Text", Text);
        Data.putInt("Attach", Attach);
        Owner.send(ComputerAction.ChatSend, Data);
        Input.setValue("");
        Attach = -1;
        Scroll = 0;
        Owner.ui().focus(Input);
    }

    private void select(String Key) {
        if (Owner == null) {
            return;
        }
        Channel = Key;
        Scroll = 0;
        Messages.clear();
        CompoundTag Data = new CompoundTag();
        Data.putString("Key", Key);
        Owner.send(ComputerAction.ChatChannel, Data);
        Owner.markSeen(Key);
    }

    @Override
    public void render(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H) {
        Owner = Screen;
        drawChannels(Screen, Ui, X, Y, H);
        Ui.fill(X + ChannelWidth + 3, Y, 1, H, OsLayout.PhosphorFaint);
        if (!Online) {
            Ui.glyph(OsLayout.GlyphWarn, X + MessageX + MessageWidth / 2 - 4, Y + 46, OsLayout.Amber);
            Ui.centered(Component.translatable(Prefix + "no_server").getString(), X + MessageX + MessageWidth / 2, Y + 58, OsLayout.Amber);
            Ui.centered(Component.translatable(Prefix + "no_server.hint").getString(), X + MessageX + MessageWidth / 2, Y + 70, OsLayout.PhosphorDim);
            return;
        }
        int Visible = drawMessages(Screen, Ui, X + MessageX, Y, MessageWidth, MessageHeight, Reading == null && !Picking);
        report(Visible);
        drawInput(Ui, X, Y);
        if (Picking) {
            drawPicker(Ui, X + MessageX, Y + 10, MessageWidth, 118);
        }
        if (Reading != null && Reading.isImage()) {
            ImageView.draw(Ui, X + MessageX - 2, Y, MessageWidth + 7, MessageHeight + 2, Reading, () -> Reading = null);
        } else if (Reading != null) {
            NoteView.draw(Ui, X + MessageX - 2, Y, MessageWidth + 7, MessageHeight + 2, Reading, () -> Reading = null);
        }
    }

    private void drawChannels(ComputerScreen Screen, OsUi Ui, int X, int Y, int H) {
        Ui.heading(Component.translatable(Prefix + "channels").getString(), X, Y + 1, ChannelWidth);
        int RowY = Y + 12;
        for (ChatChannels.Info Info : Channels) {
            if (RowY + OsLayout.RowHeight > Y + H - 12) {
                break;
            }
            boolean Selected = Info.Key().equals(Channel);
            String Key = Info.Key();
            Ui.row(X, RowY, ChannelWidth, OsLayout.RowHeight, Selected, () -> select(Key), null,
                    Component.translatable(Info.isNetwork() ? Prefix + "network.hint" : Prefix + "stored.hint", Info.Count()));
            Ui.glyph(OsLayout.GlyphChannel, X + 1, RowY + 1, Ui.rowDim(Selected));
            Ui.clipped(Info.Name(), X + 11, RowY + 2, ChannelWidth - 19, Ui.rowInk(Selected));
            if (!Selected && Screen.isUnread(Info.Key()) && (Screen.ticks() / 10) % 2 == 0) {
                Ui.lamp(X + ChannelWidth - 6, RowY + 3, true, 1);
            }
            RowY += OsLayout.RowHeight;
        }
        String Footer = Component.translatable(Online ? Prefix + "online" : Prefix + "offline").getString();
        Ui.lamp(X, Y + H - 8, Online, Online ? 0 : 2);
        Ui.clipped(Footer, X + 8, Y + H - 9, ChannelWidth - 8, Online ? OsLayout.PhosphorDim : OsLayout.Alert);
    }

    private int drawMessages(ComputerScreen Screen, OsUi Ui, int X, int Y, int W, int H, boolean Interactive) {
        Ui.box(X - 2, Y, W + 7, H + 2, OsLayout.PhosphorFaint, 0xFF050F08);
        Ui.wheel(X - 2, Y, W + 7, H + 2, Step -> Scroll = Math.max(0, Math.min(Math.max(0, Messages.size() - 1), Scroll - Step)));
        if (Messages.isEmpty()) {
            Ui.centered(Component.translatable(Prefix + "empty").getString(), X + W / 2, Y + H / 2 - 4, OsLayout.PhosphorDim);
            Ui.scrollbar(X + W + 1, Y + 2, H - 2, 0, 0, 1);
            return 0;
        }
        boolean Shielded = Minecraft.getInstance().player != null && AntiMemetic.isProtected(Minecraft.getInstance().player);
        int Bottom = Y + H - 2;
        int End = Messages.size() - Scroll;
        int Shown = 0;
        int TextWidth = W - 4;
        for (int Index = End - 1; Index >= 0 && Bottom > Y + 2; Index--) {
            ChatMessage Message = Messages.get(Index);
            List<Line> Lines = layout(Ui, Message, TextWidth, Shielded);
            int Height = 0;
            for (Line Each : Lines) {
                Height += Each.Height();
            }
            int Top = Bottom - Height;
            if (Top < Y + 2 && Shown > 0) {
                break;
            }
            Ui.graphics().enableScissor(X - 1, Y + 1, X + W + 1, Y + H + 1);
            int LineY = Top;
            for (Line Each : Lines) {
                drawLine(Screen, Ui, Message, Each, X + 2, LineY, TextWidth, Interactive);
                LineY += Each.Height();
            }
            Ui.graphics().disableScissor();
            if (Interactive) {
                Ui.tip(X, Math.max(Y, Top), W, Math.min(Height, Bottom - Math.max(Y, Top)), Component.translatable(Prefix + "stamp",
                        ComputerText.clockOf(Message.Stamp()), Message.nameComponent()));
            }
            Bottom = Top - 2;
            Shown++;
        }
        Ui.scrollbar(X + W + 1, Y + 2, H - 2, Math.max(0, Messages.size() - Scroll - Shown), Messages.size(), Math.max(1, Shown));
        return Shown;
    }

    private List<Line> layout(OsUi Ui, ChatMessage Message, int Width, boolean Shielded) {
        List<Line> Lines = new ArrayList<>();
        String Name = Message.nameComponent().getString();
        if (Message.Memetic()) {
            Lines.add(new Line(Kind.Pattern, Name + ":", LineHeight * PatternLines + 1, Shielded));
            return Lines;
        }
        String Body = Name + ": " + Message.textComponent().getString();
        if (Message.Text().isEmpty() && Message.From() != ChatMessage.Sender.Anomaly) {
            Body = Name + ":";
        }
        List<int[]> Wrapped = Ui.wrap(Body, Width);
        for (int Index = 0; Index < Wrapped.size(); Index++) {
            int[] Span = Wrapped.get(Index);
            Lines.add(new Line(Index == 0 ? Kind.First : Kind.Text, Body.substring(Span[0], Span[1]), LineHeight, Shielded));
        }
        if (Message.Attachment().isPresent()) {
            Lines.add(new Line(Kind.Attachment, Message.Attachment().get().Name(), LineHeight + 3, Shielded));
        }
        return Lines;
    }

    private void drawLine(ComputerScreen Screen, OsUi Ui, ChatMessage Message, Line Each, int X, int Y, int Width, boolean Interactive) {
        String Name = Message.nameComponent().getString() + ":";
        int NameInk = switch (Message.From()) {
            case System -> OsLayout.Amber;
            case Anomaly -> OsLayout.Alert;
            default -> OsLayout.PhosphorTitle;
        };
        int TextInk = Message.From() == ChatMessage.Sender.Anomaly ? 0xFFFFB0A4 : OsLayout.Phosphor;
        switch (Each.Type()) {
            case First -> {
                Ui.text(Each.Text(), X, Y, TextInk);
                if (Each.Text().startsWith(Name)) {
                    Ui.text(Name, X, Y, NameInk);
                }
            }
            case Text -> Ui.text(Each.Text(), X + 4, Y, TextInk);
            case Pattern -> {
                Ui.text(Each.Text(), X, Y, NameInk);
                int PatternX = X + Ui.font().width(Each.Text()) + 4;
                int PatternWidth = Math.max(8, Width - (PatternX - X));
                if (Each.Shielded()) {
                    for (int Px = 0; Px < PatternWidth; Px += 2) {
                        int Noise = (Px * 7 + Screen.ticks() * 3 + Px / 3) % 5;
                        Ui.fill(PatternX + Px, Y + 3 + Noise % 3, 2, 2, 0xFF2A3A2E);
                    }
                } else {
                    Ui.pattern(PatternX, Y, PatternWidth, Each.Height() - 1, Screen.ticks() / 3);
                }
            }
            case Attachment -> {
                ComputerFile File = Message.Attachment().orElse(null);
                if (File == null) {
                    return;
                }
                Ui.glyph(File.isDossier() ? OsLayout.GlyphDossier : OsLayout.GlyphNote, X + 4, Y + 2, OsLayout.PhosphorTitle);
                Ui.clipped(File.Name(), X + 14, Y + 2, Width - 80, OsLayout.PhosphorHot);
                if (!Interactive) {
                    return;
                }
                Ui.button(X + Width - 62, Y, 30, 11, Component.translatable(Prefix + "view"), true, () -> view(Screen, File, Message.Id()),
                        Component.translatable(Prefix + "view.hint"));
                long Id = Message.Id();
                Ui.button(X + Width - 30, Y, 30, 11, Component.translatable(Prefix + "save"), true, () -> {
                    CompoundTag Data = new CompoundTag();
                    Data.putString("Key", Channel);
                    Data.putLong("Id", Id);
                    Screen.send(ComputerAction.ChatSave, Data);
                }, Component.translatable(Prefix + "save.hint"));
            }
        }
    }

    private void view(ComputerScreen Screen, ComputerFile File, long Id) {
        if (File.isDossier()) {
            Screen.openDossier(File.readable());
        } else if (File.isImage()) {
            Reading = File;
            CompoundTag Data = new CompoundTag();
            Data.putString("Key", Channel);
            Data.putLong("Id", Id);
            Screen.send(ComputerAction.ChatImage, Data);
        } else {
            Reading = File;
        }
    }

    private void drawInput(OsUi Ui, int X, int Y) {
        int FieldX = X + MessageX - 2;
        if (Attach >= 0 && Attach < Local.size()) {
            ComputerFile File = Local.get(Attach);
            int ChipY = Y + InputY - 13;
            Ui.box(FieldX, ChipY, InputWidth, 12, OsLayout.PhosphorDim, OsLayout.PhosphorDark);
            Ui.glyph(OsLayout.GlyphClip, FieldX + 2, ChipY + 2, OsLayout.PhosphorTitle);
            Ui.clipped(File.Name(), FieldX + 12, ChipY + 2, InputWidth - 26, OsLayout.PhosphorHot);
            Ui.glyphButton(FieldX + InputWidth - 11, ChipY + 1, 10, 10, OsLayout.GlyphClose, true, () -> Attach = -1,
                    Component.translatable(Prefix + "detach"));
        }
        Ui.field(Input, FieldX, Y + InputY, InputWidth, Component.translatable(Prefix + "type"), null);
        Ui.glyphButton(X + AttachX, Y + InputY, 14, OsLayout.FieldHeight, OsLayout.GlyphClip, !Local.isEmpty(), () -> {
            Picking = !Picking;
            PickScroll = 0;
        }, Component.translatable(Local.isEmpty() ? Prefix + "attach.none" : Prefix + "attach"));
        Ui.button(X + SendX, Y + InputY, SendWidth, OsLayout.FieldHeight, Component.translatable(Prefix + "send"),
                !Input.getValue().isBlank() || Attach >= 0, this::send, Component.translatable(Prefix + "send.hint"));
    }

    private void drawPicker(OsUi Ui, int X, int Y, int W, int H) {
        Ui.hit(X, Y, W, H, null, null, null);
        Ui.box(X, Y, W, H, OsLayout.Phosphor, OsLayout.PhosphorDark);
        Ui.heading(Component.translatable(Prefix + "attach.title").getString(), X + 4, Y + 3, W - 8);
        int Rows = (H - 18) / OsLayout.RowHeight;
        PickScroll = Math.max(0, Math.min(PickScroll, Math.max(0, Local.size() - Rows)));
        Ui.wheel(X, Y, W, H, Step -> PickScroll = Math.max(0, Math.min(Math.max(0, Local.size() - Rows), PickScroll + Step)));
        for (int Row = 0; Row < Rows && PickScroll + Row < Local.size(); Row++) {
            int Index = PickScroll + Row;
            ComputerFile File = Local.get(Index);
            int RowY = Y + 14 + Row * OsLayout.RowHeight;
            boolean Selected = Index == Attach;
            Ui.row(X + 3, RowY, W - 12, OsLayout.RowHeight, Selected, () -> {
                Attach = Index;
                Picking = false;
            }, null, null);
            Ui.glyph(File.isDossier() ? OsLayout.GlyphDossier : OsLayout.GlyphNote, X + 5, RowY + 1, Ui.rowDim(Selected));
            Ui.clipped(File.Name(), X + 16, RowY + 2, W - 30, Ui.rowInk(Selected));
        }
        Ui.scrollbar(X + W - 7, Y + 14, H - 18, PickScroll, Local.size(), Rows);
    }

    private void report(int Visible) {
        if (Owner == null || Visible == ReportedVisible && Scroll == ReportedScroll) {
            return;
        }
        ReportedVisible = Visible;
        ReportedScroll = Scroll;
        CompoundTag Data = new CompoundTag();
        Data.putString("Key", Channel);
        Data.putInt("Scroll", Scroll);
        Data.putInt("Visible", Math.max(1, Visible));
        Owner.send(ComputerAction.ChatView, Data);
    }

    private enum Kind {
        First,
        Text,
        Pattern,
        Attachment
    }

    private record Line(Kind Type, String Text, int Height, boolean Shielded) {
    }
}
