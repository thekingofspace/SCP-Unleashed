package net.rai.scp_unleashed.cameras.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cameras.CameraLinks;
import net.rai.scp_unleashed.cameras.CameraRegistry;
import net.rai.scp_unleashed.cameras.network.CameraEditPayload;
import net.rai.scp_unleashed.cameras.network.CameraOpenPayload;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

public class CameraTabletScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/cameras/tablet.png");
    public static final int TextureSize = 256;
    public static final int PanelWidth = 252;
    public static final int PanelHeight = 216;
    public static final int ScreenLeft = 12;
    public static final int ScreenTop = 12;
    public static final int ScreenRight = 240;
    public static final int ScreenBottom = 200;
    public static final int HeaderY = 16;
    public static final int RuleY = 27;
    public static final int RowTop = 31;
    public static final int RowHeight = 22;
    public static final int VisibleRows = 7;
    public static final int LineStep = 11;
    public static final int FooterY = 188;
    public static final int EmptyY = 96;
    public static final int IndexX = 16;
    public static final int DotX = 31;
    public static final int NameX = 40;
    public static final int TextRight = 205;
    public static final int RenameX = 210;
    public static final int UnlinkX = 222;
    public static final int ButtonY = 5;
    public static final int IconSize = 9;
    public static final int ScrollX = 235;
    public static final int ScrollWidth = 3;
    public static final int DotSize = 5;
    public static final int RenameU = 0;
    public static final int UnlinkU = 9;
    public static final int IconV = 220;
    public static final int IconHoverV = 229;
    public static final int DotHereU = 18;
    public static final int DotAwayU = 23;
    public static final int DotV = 220;
    public static final int Ink = 0xFF96E6A8;
    public static final int InkBright = 0xFFD2F6D8;
    public static final int InkDim = 0xFF4A8A5E;
    public static final int InkAway = 0xFF7C8A84;
    public static final int RowHover = 0x3330E07A;
    public static final int RowPending = 0x4430E07A;
    public static final int ScrollTrack = 0xFF16302A;
    public static final int ScrollThumb = 0xFF4A8A5E;
    public static final int PendingTicks = 40;
    public static final String Ellipsis = "...";
    public static final int EditFill = 0xFF0A1A12;
    public static final int EditEdge = 0xFF4A8A5E;

    private final InteractionHand Hand;
    private int Left;
    private int Top;
    private int Scroll;
    private int Editing = -1;
    private int Pending = -1;
    private int PendingAge;
    private EditBox NameBox;

    public CameraTabletScreen(InteractionHand Hand) {
        super(Component.translatable("item.scp_unleashed.camera_tablet"));
        this.Hand = Hand;
    }

    @Override
    protected void init() {
        Left = (width - PanelWidth) / 2;
        Top = (height - PanelHeight) / 2;
        NameBox = new EditBox(font, 0, 0, 120, 11, Component.translatable("gui.scp_unleashed.camera_tablet.rename"));
        NameBox.setMaxLength(CameraLinks.NameLimit);
        NameBox.setBordered(false);
        NameBox.setVisible(false);
        NameBox.setTextColor(InkBright);
        addWidget(NameBox);
    }

    private ItemStack tablet() {
        LocalPlayer User = Minecraft.getInstance().player;
        return User == null ? ItemStack.EMPTY : User.getItemInHand(Hand);
    }

    private CameraLinks links() {
        ItemStack Stack = tablet();
        return Stack.is(CameraRegistry.Tablet.get()) ? CameraLinks.of(Stack) : CameraLinks.Empty;
    }

    @Override
    public void tick() {
        super.tick();
        if (!tablet().is(CameraRegistry.Tablet.get())) {
            onClose();
            return;
        }
        if (Pending >= 0 && ++PendingAge > PendingTicks) {
            Pending = -1;
        }
        int Count = links().size();
        Scroll = Mth.clamp(Scroll, 0, Math.max(0, Count - VisibleRows));
        if (Editing >= Count) {
            stopEditing();
        }
    }

    private int rowAt(double MouseX, double MouseY) {
        int X = (int) MouseX - Left;
        int Y = (int) MouseY - Top;
        if (X < ScreenLeft + 2 || X >= ScrollX - 1 || Y < RowTop || Y >= RowTop + VisibleRows * RowHeight) {
            return -1;
        }
        int Row = (Y - RowTop) / RowHeight + Scroll;
        return Row < links().size() ? Row : -1;
    }

    private boolean inside(double MouseX, double MouseY, int X, int Y, int Size) {
        return MouseX >= Left + X && MouseX < Left + X + Size && MouseY >= Top + Y && MouseY < Top + Y + Size;
    }

    private int rowY(int Row) {
        return RowTop + (Row - Scroll) * RowHeight;
    }

    private int buttonY(int Row) {
        return rowY(Row) + ButtonY;
    }

    private String fitted(String Text, int Room) {
        if (font.width(Text) <= Room) {
            return Text;
        }
        return font.plainSubstrByWidth(Text, Room - font.width(Ellipsis)).stripTrailing() + Ellipsis;
    }

    private boolean isHere(CameraLinks.Link Entry) {
        Minecraft Game = Minecraft.getInstance();
        return Game.level != null && Entry.dimension().equals(Game.level.dimension());
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        renderTransparentBackground(Graphics);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight, TextureSize, TextureSize);
        CameraLinks Links = links();
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.camera_tablet.title"), Left + IndexX, Top + HeaderY, InkBright, false);
        Component Linked = Component.translatable("gui.scp_unleashed.camera_tablet.linked", Links.size(), CameraLinks.MaxLinks);
        Graphics.drawString(font, Linked, Left + ScreenRight - 5 - font.width(Linked), Top + HeaderY, InkDim, false);
        Graphics.fill(Left + ScreenLeft + 3, Top + RuleY, Left + ScreenRight - 3, Top + RuleY + 1, InkDim);
        Component Tip = null;
        if (Links.isEmpty()) {
            Component Empty = Component.translatable("gui.scp_unleashed.camera_tablet.empty");
            Graphics.drawString(font, Empty, Left + (PanelWidth - font.width(Empty)) / 2, Top + EmptyY, Ink, false);
            List<FormattedCharSequence> Hint = font.split(Component.translatable("gui.scp_unleashed.camera_tablet.empty_hint"), ScreenRight - ScreenLeft - 8);
            for (int Line = 0; Line < Hint.size(); Line++) {
                FormattedCharSequence Piece = Hint.get(Line);
                Graphics.drawString(font, Piece, Left + (PanelWidth - font.width(Piece)) / 2, Top + EmptyY + 14 + Line * 10, InkDim, false);
            }
        }
        int Hover = Editing >= 0 ? -1 : rowAt(MouseX, MouseY);
        int End = Math.min(Links.size(), Scroll + VisibleRows);
        for (int Row = Scroll; Row < End; Row++) {
            CameraLinks.Link Entry = Links.Entries().get(Row);
            int Y = Top + rowY(Row);
            boolean Here = isHere(Entry);
            if (Row == Pending) {
                Graphics.fill(Left + ScreenLeft + 2, Y - 1, Left + ScrollX - 2, Y + RowHeight - 2, RowPending);
            } else if (Row == Hover) {
                Graphics.fill(Left + ScreenLeft + 2, Y - 1, Left + ScrollX - 2, Y + RowHeight - 2, RowHover);
            }
            Graphics.drawString(font, String.format(Locale.ROOT, "%02d", Row + 1), Left + IndexX, Y + 1, InkDim, false);
            Graphics.blit(Texture, Left + DotX, Y + 2, Here ? DotHereU : DotAwayU, DotV, DotSize, DotSize, TextureSize, TextureSize);
            Component Where = Row == Pending ? Component.translatable("gui.scp_unleashed.camera_tablet.connecting")
                    : Here ? Component.translatable("gui.scp_unleashed.camera_tablet.coords", Entry.pos().getX(), Entry.pos().getY(), Entry.pos().getZ())
                    : Component.translatable("gui.scp_unleashed.camera_tablet.elsewhere");
            if (Row != Editing) {
                String Name = Entry.Name().isEmpty() ? CameraLinks.fallbackName(Row + 1) : Entry.Name();
                Graphics.drawString(font, fitted(Name, TextRight - NameX), Left + NameX, Y + 1, Row == Pending ? InkBright : Here ? Ink : InkAway, false);
            }
            Graphics.drawString(font, fitted(Where.getString(), TextRight - NameX), Left + NameX, Y + 1 + LineStep, InkDim, false);
            boolean OverRename = inside(MouseX, MouseY, RenameX, buttonY(Row), IconSize);
            boolean OverUnlink = inside(MouseX, MouseY, UnlinkX, buttonY(Row), IconSize);
            Graphics.blit(Texture, Left + RenameX, Y + ButtonY, RenameU, OverRename ? IconHoverV : IconV, IconSize, IconSize, TextureSize, TextureSize);
            Graphics.blit(Texture, Left + UnlinkX, Y + ButtonY, UnlinkU, OverUnlink ? IconHoverV : IconV, IconSize, IconSize, TextureSize, TextureSize);
            if (Editing < 0 && OverRename) {
                Tip = Component.translatable("gui.scp_unleashed.camera_tablet.rename");
            } else if (Editing < 0 && OverUnlink) {
                Tip = Component.translatable("gui.scp_unleashed.camera_tablet.unlink");
            }
        }
        if (Links.size() > VisibleRows) {
            int TrackTop = Top + RowTop;
            int TrackHeight = VisibleRows * RowHeight - 2;
            Graphics.fill(Left + ScrollX, TrackTop, Left + ScrollX + ScrollWidth, TrackTop + TrackHeight, ScrollTrack);
            int Thumb = Math.max(8, TrackHeight * VisibleRows / Links.size());
            int Travel = TrackHeight - Thumb;
            int Offset = Links.size() - VisibleRows <= 0 ? 0 : Travel * Scroll / (Links.size() - VisibleRows);
            Graphics.fill(Left + ScrollX, TrackTop + Offset, Left + ScrollX + ScrollWidth, TrackTop + Offset + Thumb, ScrollThumb);
        }
        Component Footer = Component.translatable(Editing >= 0 ? "gui.scp_unleashed.camera_tablet.footer_rename" : "gui.scp_unleashed.camera_tablet.footer");
        Graphics.drawString(font, Footer, Left + IndexX, Top + FooterY, InkDim, false);
        if (Editing >= 0) {
            int BoxX = NameBox.getX() - 2;
            int BoxY = NameBox.getY() - 2;
            Graphics.fill(BoxX, BoxY, BoxX + NameBox.getWidth() + 3, BoxY + 11, EditEdge);
            Graphics.fill(BoxX + 1, BoxY + 1, BoxX + NameBox.getWidth() + 2, BoxY + 10, EditFill);
        }
        NameBox.render(Graphics, MouseX, MouseY, Partial);
        if (Tip != null) {
            Graphics.renderTooltip(font, Tip, MouseX, MouseY);
        } else if (Hover >= 0 && Hover != Pending && Editing < 0 && !inside(MouseX, MouseY, RenameX, buttonY(Hover), IconSize) && !inside(MouseX, MouseY, UnlinkX, buttonY(Hover), IconSize)) {
            CameraLinks.Link Entry = Links.get(Hover);
            if (Entry != null && !isHere(Entry)) {
                Graphics.renderTooltip(font, Component.translatable("gui.scp_unleashed.camera_tablet.elsewhere_tip"), MouseX, MouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Editing >= 0) {
            if (NameBox.isMouseOver(MouseX, MouseY)) {
                return super.mouseClicked(MouseX, MouseY, Button);
            }
            commitRename();
            return true;
        }
        if (Button != 0) {
            return super.mouseClicked(MouseX, MouseY, Button);
        }
        int Row = rowAt(MouseX, MouseY);
        if (Row < 0) {
            return super.mouseClicked(MouseX, MouseY, Button);
        }
        if (inside(MouseX, MouseY, RenameX, buttonY(Row), IconSize)) {
            startEditing(Row);
            tap();
            return true;
        }
        if (inside(MouseX, MouseY, UnlinkX, buttonY(Row), IconSize)) {
            PacketDistributor.sendToServer(new CameraEditPayload(Hand.ordinal(), Row, true, ""));
            tap();
            return true;
        }
        Pending = Row;
        PendingAge = 0;
        PacketDistributor.sendToServer(new CameraOpenPayload(Hand.ordinal(), Row));
        tap();
        return true;
    }

    private void tap() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(CameraRegistry.TabletLink.get(), 1.3f, 0.35f));
    }

    private void startEditing(int Row) {
        CameraLinks.Link Entry = links().get(Row);
        if (Entry == null) {
            return;
        }
        Editing = Row;
        NameBox.setX(Left + NameX + 1);
        NameBox.setY(Top + rowY(Row) + 2);
        NameBox.setWidth(TextRight - NameX - 2);
        NameBox.setValue(Entry.Name().isEmpty() ? CameraLinks.fallbackName(Row + 1) : Entry.Name());
        NameBox.setVisible(true);
        NameBox.setFocused(true);
        setFocused(NameBox);
    }

    private void stopEditing() {
        Editing = -1;
        NameBox.setVisible(false);
        NameBox.setFocused(false);
        setFocused(null);
    }

    private void commitRename() {
        if (Editing >= 0) {
            String Value = CameraLinks.clean(NameBox.getValue());
            if (!Value.isEmpty()) {
                PacketDistributor.sendToServer(new CameraEditPayload(Hand.ordinal(), Editing, false, Value));
            }
        }
        stopEditing();
    }

    @Override
    public boolean keyPressed(int Key, int Scan, int Modifiers) {
        if (Editing >= 0) {
            if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER) {
                commitRename();
                return true;
            }
            if (Key == GLFW.GLFW_KEY_ESCAPE) {
                stopEditing();
                return true;
            }
            return NameBox.keyPressed(Key, Scan, Modifiers) || NameBox.canConsumeInput() || super.keyPressed(Key, Scan, Modifiers);
        }
        return super.keyPressed(Key, Scan, Modifiers);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double DeltaX, double DeltaY) {
        if (Editing < 0 && links().size() > VisibleRows) {
            Scroll = Mth.clamp(Scroll - (int) Math.signum(DeltaY), 0, links().size() - VisibleRows);
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, DeltaX, DeltaY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
