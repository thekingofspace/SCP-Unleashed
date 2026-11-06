package net.rai.scp_unleashed.security.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.security.SecurityRegistry;
import net.rai.scp_unleashed.security.SentryTurretBlockEntity;
import net.rai.scp_unleashed.security.SentryTurretMenu;
import net.rai.scp_unleashed.security.TurretCategory;
import net.rai.scp_unleashed.security.TurretConfigPayload;
import net.rai.scp_unleashed.security.TurretRound;
import net.rai.scp_unleashed.security.TurretRules;
import net.rai.scp_unleashed.security.TurretStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SentryTurretScreen extends AbstractContainerScreen<SentryTurretMenu> {
    public static final ResourceLocation Texture = SecurityRegistry.location("textures/gui/security/sentry_turret.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 232;
    public static final int TitleY = 6;
    public static final int StatusRight = 168;
    public static final int LcdX = 66;
    public static final int LcdY = 17;
    public static final int LcdWidth = 102;
    public static final int LcdHeight = 22;
    public static final int TabY = 42;
    public static final int TabWidth = 56;
    public static final int TabHeight = 14;
    public static final int[] TabX = {7, 64};
    public static final int TabU = 176;
    public static final int TabV = 72;
    public static final int AreaX = 7;
    public static final int AreaY = 56;
    public static final int AreaWidth = 162;
    public static final int AreaHeight = 82;
    public static final int ToggleX = 10;
    public static final int ToggleY = 59;
    public static final int ToggleWidth = 76;
    public static final int ToggleHeight = 24;
    public static final int TogglePitchX = 80;
    public static final int TogglePitchY = 26;
    public static final int ToggleU = 176;
    public static final int ToggleV = 0;
    public static final int IconU = 176;
    public static final int IconV = 166;
    public static final int IconSize = 12;
    public static final int LampU = 176;
    public static final int LampV = 156;
    public static final int LampSize = 6;
    public static final int FieldX = 10;
    public static final int FieldY = 59;
    public static final int FieldWidth = 96;
    public static final int FieldHeight = 14;
    public static final int FieldU = 0;
    public static final int FieldV = 232;
    public static final int ButtonY = 59;
    public static final int[] ButtonX = {108, 139};
    public static final int ButtonWidth = 29;
    public static final int ButtonHeight = 14;
    public static final int ButtonU = 176;
    public static final int ButtonV = 114;
    public static final int RowX = 10;
    public static final int RowY = 76;
    public static final int RowWidth = 149;
    public static final int RowHeight = 12;
    public static final int Rows = 5;
    public static final int RowU = 96;
    public static final int RowV = 232;
    public static final int RemoveSize = 9;
    public static final int RemoveU = 206;
    public static final int RemoveV = 156;
    public static final int TagWidth = 30;
    public static final int TrackX = 161;
    public static final int TrackWidth = 5;
    public static final int ThumbU = 226;
    public static final int ThumbV = 156;
    public static final int ThumbHeight = 10;
    public static final int StarU = 232;
    public static final int StarV = 156;
    public static final int InventoryLabelY = 140;
    public static final int DarkText = 0xFF404040;
    public static final int LightText = 0xFFD8DCE0;
    public static final int DimText = 0xFF8C949C;
    public static final int LcdText = 0xFF7CF08C;
    public static final int LcdDim = 0xFF3C8C48;
    public static final int AllowText = 0xFF7CE07C;
    public static final int DenyText = 0xFFF06C5C;
    public static final int BadText = 0xFFF06C5C;
    public static final int TabTargets = 0;
    public static final int TabAccess = 1;
    private static int RememberedTab = TabTargets;

    private EditBox Field;
    private int Tab = RememberedTab;
    private int Scroll;
    private int Pressed = -1;

    public SentryTurretScreen(SentryTurretMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = InventoryLabelY;
        titleLabelY = TitleY;
    }

    @Override
    protected void init() {
        super.init();
        Field = new EditBox(font, leftPos + FieldX + 3, topPos + FieldY + 3, FieldWidth - 6, FieldHeight - 4, Component.translatable("gui.scp_unleashed.sentry_turret.entry"));
        Field.setBordered(false);
        Field.setMaxLength(TurretRules.MaxLength);
        Field.setTextColor(LightText);
        Field.setHint(Component.translatable("gui.scp_unleashed.sentry_turret.entry_hint").withStyle(ChatFormatting.DARK_GRAY));
        Field.setResponder(this::suggest);
        addRenderableWidget(Field);
        updateTab();
    }

    @Nullable
    private SentryTurretBlockEntity turret() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(menu.getPos()) instanceof SentryTurretBlockEntity Turret ? Turret : null;
    }

    private boolean editable() {
        SentryTurretBlockEntity Turret = turret();
        return Turret != null && minecraft != null && minecraft.player != null && Turret.canEdit(minecraft.player);
    }

    private void updateTab() {
        RememberedTab = Tab;
        boolean Access = Tab == TabAccess;
        Field.setVisible(Access);
        Field.setEditable(Access && editable());
        if (!Access) {
            Field.setFocused(false);
        }
    }

    private void suggest(String Text) {
        Field.setTextColor(Text.isEmpty() || TurretRules.normalise(Text) != null ? LightText : BadText);
        String Best = bestMatch(Text);
        Field.setSuggestion(Best == null ? null : Best.substring(Text.length()));
    }

    @Nullable
    private static String bestMatch(String Text) {
        int Colon = Text.indexOf(':');
        if (Colon < 0 || Text.length() < Colon + 2) {
            return null;
        }
        String Lower = Text.toLowerCase(Locale.ROOT);
        String Best = null;
        for (ResourceLocation Id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            String Name = Id.toString();
            if (Name.startsWith(Lower) && Name.length() > Lower.length() && (Best == null || Name.compareTo(Best) < 0)) {
                Best = Name;
            }
        }
        return Best;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        SentryTurretBlockEntity Turret = turret();
        int Count = Turret == null ? 0 : Turret.getRules().getEntries().size() + 1;
        Scroll = Mth.clamp(Scroll, 0, Math.max(0, Count - Rows));
        Field.setEditable(Tab == TabAccess && editable());
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        double GuiX = MouseX - leftPos;
        double GuiY = MouseY - topPos;
        for (int Index = 0; Index < TabX.length; Index++) {
            int State = Index == Tab ? 1 : inside(GuiX, GuiY, TabX[Index], TabY, TabWidth, TabHeight) ? 2 : 0;
            Graphics.blit(Texture, leftPos + TabX[Index], topPos + TabY, TabU, TabV + State * TabHeight, TabWidth, TabHeight);
        }
        SentryTurretBlockEntity Turret = turret();
        if (Turret == null) {
            return;
        }
        boolean Editable = editable();
        if (Tab == TabTargets) {
            for (TurretCategory Category : TurretCategory.values()) {
                int X = toggleX(Category.ordinal());
                int Y = toggleY(Category.ordinal());
                boolean On = Turret.getRules().has(Category);
                boolean Hover = Editable && inside(GuiX, GuiY, X, Y, ToggleWidth, ToggleHeight);
                int State = Hover ? 2 : On ? 1 : 0;
                Graphics.blit(Texture, leftPos + X, topPos + Y, ToggleU, ToggleV + State * ToggleHeight, ToggleWidth, ToggleHeight);
                Graphics.blit(Texture, leftPos + X + 5, topPos + Y + 6, IconU + Category.ordinal() * IconSize, IconV, IconSize, IconSize);
                Graphics.blit(Texture, leftPos + X + ToggleWidth - 11, topPos + Y + 9, LampU + (On ? 2 : 0) * LampSize, LampV, LampSize, LampSize);
            }
        } else {
            Graphics.blit(Texture, leftPos + FieldX, topPos + FieldY, FieldU, FieldV, FieldWidth, FieldHeight);
            boolean Valid = Editable && TurretRules.normalise(Field.getValue()) != null;
            for (int Index = 0; Index < ButtonX.length; Index++) {
                boolean Hover = Valid && inside(GuiX, GuiY, ButtonX[Index], ButtonY, ButtonWidth, ButtonHeight);
                int State = !Valid ? 2 : Pressed == Index || Hover ? 1 : 0;
                Graphics.blit(Texture, leftPos + ButtonX[Index], topPos + ButtonY, ButtonU + Index * ButtonWidth, ButtonV + State * ButtonHeight, ButtonWidth, ButtonHeight);
            }
            int Count = Turret.getRules().getEntries().size() + 1;
            for (int Row = 0; Row < Rows; Row++) {
                int Index = Scroll + Row;
                if (Index >= Count) {
                    break;
                }
                int Y = RowY + Row * RowHeight;
                if (inside(GuiX, GuiY, RowX, Y, RowWidth, RowHeight)) {
                    Graphics.blit(Texture, leftPos + RowX, topPos + Y, RowU, RowV, RowWidth, RowHeight);
                }
                if (Index == 0) {
                    Graphics.blit(Texture, leftPos + RowX + 2, topPos + Y + 2, StarU, StarV, 7, 7);
                    continue;
                }
                TurretRules.Entry Item = Turret.getRules().getEntries().get(Index - 1);
                Graphics.blit(Texture, leftPos + RowX + 2, topPos + Y + 3, LampU + (Item.Deny() ? 2 : 1) * LampSize, LampV, LampSize, LampSize);
                if (Editable) {
                    boolean HoverRemove = inside(GuiX, GuiY, removeX(), Y + 1, RemoveSize, RemoveSize);
                    Graphics.blit(Texture, leftPos + removeX(), topPos + Y + 1, RemoveU + (HoverRemove ? RemoveSize : 0), RemoveV, RemoveSize, RemoveSize);
                }
            }
            if (Count > Rows) {
                int Travel = Rows * RowHeight - ThumbHeight;
                int ThumbY = RowY + Math.round(Travel * Scroll / (float) (Count - Rows));
                Graphics.blit(Texture, leftPos + TrackX, topPos + ThumbY, ThumbU, ThumbV, TrackWidth, ThumbHeight);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, titleLabelX, titleLabelY, DarkText, false);
        TurretStatus Status = menu.getStatus();
        Component StatusText = Component.translatable(Status.getTranslationKey());
        int Colour = statusColour(Status);
        Graphics.drawString(font, StatusText, StatusRight - font.width(StatusText), titleLabelY, Colour, false);
        Graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, DarkText, false);
        TurretRound Round = menu.getRound();
        Component Calibre = Round == null ? Component.translatable("gui.scp_unleashed.sentry_turret.lcd.empty")
                : Component.translatable("gui.scp_unleashed.sentry_turret.round." + Round.name().toLowerCase(Locale.ROOT));
        Graphics.drawString(font, Calibre, LcdX + 4, LcdY + 3, Round == null ? LcdDim : LcdText, false);
        String Loaded = String.valueOf(menu.getRounds());
        Graphics.drawString(font, Loaded, LcdX + LcdWidth - 4 - font.width(Loaded), LcdY + 3, Round == null ? LcdDim : LcdText, false);
        Component Draw = Component.translatable("gui.scp_unleashed.sentry_turret.lcd.draw", Status == TurretStatus.Offline ? 0 : menu.getDemand());
        Graphics.drawString(font, Draw, LcdX + 4, LcdY + 12, LcdDim, false);
        String Reach = (int) SentryTurretBlockEntity.Range + "m";
        Graphics.drawString(font, Reach, LcdX + LcdWidth - 4 - font.width(Reach), LcdY + 12, LcdDim, false);
        String[] Names = {"gui.scp_unleashed.sentry_turret.tab.targets", "gui.scp_unleashed.sentry_turret.tab.access"};
        for (int Index = 0; Index < TabX.length; Index++) {
            Component Label = Component.translatable(Names[Index]);
            Graphics.drawString(font, Label, TabX[Index] + (TabWidth - font.width(Label)) / 2, TabY + 3, Index == Tab ? LightText : DarkText, false);
        }
        SentryTurretBlockEntity Turret = turret();
        if (Turret == null) {
            return;
        }
        if (Tab == TabTargets) {
            for (TurretCategory Category : TurretCategory.values()) {
                boolean On = Turret.getRules().has(Category);
                Graphics.drawString(font, Component.translatable(Category.getTranslationKey()), toggleX(Category.ordinal()) + 20, toggleY(Category.ordinal()) + 8,
                        On ? LightText : DimText, false);
            }
            return;
        }
        boolean Valid = editable() && TurretRules.normalise(Field.getValue()) != null;
        String[] Buttons = {"gui.scp_unleashed.sentry_turret.allow", "gui.scp_unleashed.sentry_turret.deny"};
        for (int Index = 0; Index < ButtonX.length; Index++) {
            Component Label = Component.translatable(Buttons[Index]);
            int Tint = !Valid ? DimText : Index == 0 ? AllowText : DenyText;
            Graphics.drawString(font, Label, ButtonX[Index] + (ButtonWidth - font.width(Label)) / 2, ButtonY + 3, Tint, false);
        }
        int Count = Turret.getRules().getEntries().size() + 1;
        for (int Row = 0; Row < Rows; Row++) {
            int Index = Scroll + Row;
            if (Index >= Count) {
                break;
            }
            int Y = RowY + Row * RowHeight + 2;
            if (Index == 0) {
                String Owner = Turret.getOwnerName().isEmpty() ? "-" : Turret.getOwnerName();
                Graphics.drawString(font, clip(Owner, removeX() - TagWidth - RowX - 14), RowX + 12, Y, LightText, false);
                Component Tag = Component.translatable("gui.scp_unleashed.sentry_turret.owner");
                Graphics.drawString(font, Tag, removeX() + RemoveSize - font.width(Tag), Y, AllowText, false);
                continue;
            }
            TurretRules.Entry Item = Turret.getRules().getEntries().get(Index - 1);
            Graphics.drawString(font, clip(entryName(Item).getString(), removeX() - TagWidth - RowX - 14), RowX + 11, Y, LightText, false);
            Component Tag = Component.translatable(Item.Deny() ? "gui.scp_unleashed.sentry_turret.deny" : "gui.scp_unleashed.sentry_turret.allow");
            Graphics.drawString(font, Tag, removeX() - 2 - font.width(Tag), Y, Item.Deny() ? DenyText : AllowText, false);
        }
    }

    private String clip(String Text, int Width) {
        if (font.width(Text) <= Width) {
            return Text;
        }
        return font.plainSubstrByWidth(Text, Width - font.width("..")) + "..";
    }

    private static Component entryName(TurretRules.Entry Item) {
        if (!Item.isType()) {
            return Component.literal(Item.Key());
        }
        ResourceLocation Id = ResourceLocation.tryParse(Item.Key());
        if (Id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(Id)) {
            return Component.literal(Item.Key());
        }
        EntityType<?> Type = BuiltInRegistries.ENTITY_TYPE.get(Id);
        return Type.getDescription();
    }

    private static int statusColour(TurretStatus Status) {
        Integer Value = Status.getColour().getColor();
        return Value == null ? DarkText : 0xFF000000 | darker(Value);
    }

    private static int darker(int Rgb) {
        int Red = (Rgb >> 16) & 255;
        int Green = (Rgb >> 8) & 255;
        int Blue = Rgb & 255;
        return (Red * 3 / 5) << 16 | (Green * 3 / 5) << 8 | Blue * 3 / 5;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            renderTooltip(Graphics, MouseX, MouseY);
            return;
        }
        List<Component> Lines = tooltipAt(MouseX - leftPos, MouseY - topPos);
        if (!Lines.isEmpty()) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private List<Component> tooltipAt(double GuiX, double GuiY) {
        List<Component> Lines = new ArrayList<>();
        SentryTurretBlockEntity Turret = turret();
        if (hoveredSlot instanceof SlotItemHandler) {
            Lines.add(Component.translatable("gui.scp_unleashed.sentry_turret.ammo"));
            Lines.add(Component.translatable("gui.scp_unleashed.sentry_turret.ammo_hint").withStyle(ChatFormatting.GRAY));
            return Lines;
        }
        if (inside(GuiX, GuiY, LcdX, LcdY, LcdWidth, LcdHeight)) {
            TurretStatus Status = menu.getStatus();
            Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
            Lines.add(Component.translatable(Status.getTranslationKey() + ".hint").withStyle(ChatFormatting.GRAY));
            TurretRound Round = menu.getRound();
            if (Round != null) {
                Lines.add(Component.translatable("gui.scp_unleashed.sentry_turret.rate", String.format(Locale.ROOT, "%.1f", 20.0f / Round.getInterval()))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return Lines;
        }
        for (int Index = 0; Index < TabX.length; Index++) {
            if (inside(GuiX, GuiY, TabX[Index], TabY, TabWidth, TabHeight)) {
                Lines.add(Component.translatable(Index == TabTargets ? "gui.scp_unleashed.sentry_turret.tab.targets.hint" : "gui.scp_unleashed.sentry_turret.tab.access.hint"));
                return Lines;
            }
        }
        if (Turret == null) {
            return Lines;
        }
        boolean Editable = editable();
        if (Tab == TabTargets) {
            for (TurretCategory Category : TurretCategory.values()) {
                if (inside(GuiX, GuiY, toggleX(Category.ordinal()), toggleY(Category.ordinal()), ToggleWidth, ToggleHeight)) {
                    boolean On = Turret.getRules().has(Category);
                    Lines.add(Component.translatable(Category.getTranslationKey()).withStyle(On ? ChatFormatting.RED : ChatFormatting.GRAY));
                    Lines.add(Component.translatable(Category.getHintKey()).withStyle(ChatFormatting.GRAY));
                    Lines.add(Component.translatable(On ? "gui.scp_unleashed.sentry_turret.targeted" : "gui.scp_unleashed.sentry_turret.ignored")
                            .withStyle(On ? ChatFormatting.RED : ChatFormatting.GREEN));
                    if (!Editable) {
                        Lines.add(Component.translatable("gui.scp_unleashed.sentry_turret.view_only").withStyle(ChatFormatting.DARK_GRAY));
                    }
                    return Lines;
                }
            }
            return Lines;
        }
        if (inside(GuiX, GuiY, FieldX, FieldY, FieldWidth, FieldHeight)) {
            String Text = Field.getValue();
            if (!Text.isEmpty() && TurretRules.normalise(Text) == null) {
                Lines.add(Component.translatable(Text.indexOf(':') >= 0 ? "gui.scp_unleashed.sentry_turret.bad_type" : "gui.scp_unleashed.sentry_turret.bad_name")
                        .withStyle(ChatFormatting.RED));
            } else {
                Lines.add(Component.translatable("gui.scp_unleashed.sentry_turret.entry_tip"));
            }
            return Lines;
        }
        for (int Index = 0; Index < ButtonX.length; Index++) {
            if (inside(GuiX, GuiY, ButtonX[Index], ButtonY, ButtonWidth, ButtonHeight)) {
                Lines.add(Component.translatable(Index == 0 ? "gui.scp_unleashed.sentry_turret.allow.hint" : "gui.scp_unleashed.sentry_turret.deny.hint"));
                if (!Editable) {
                    Lines.add(Component.translatable("gui.scp_unleashed.sentry_turret.view_only").withStyle(ChatFormatting.DARK_GRAY));
                }
                return Lines;
            }
        }
        int Row = rowAt(GuiX, GuiY);
        if (Row >= 0) {
            if (Row == 0) {
                Lines.add(Component.translatable("gui.scp_unleashed.sentry_turret.owner.hint"));
                return Lines;
            }
            TurretRules.Entry Item = Turret.getRules().getEntries().get(Row - 1);
            Lines.add(entryName(Item).copy().withStyle(Item.Deny() ? ChatFormatting.RED : ChatFormatting.GREEN));
            if (Item.isType()) {
                Lines.add(Component.literal(Item.Key()).withStyle(ChatFormatting.DARK_GRAY));
            }
            Lines.add(Component.translatable(Item.Deny() ? "gui.scp_unleashed.sentry_turret.deny.row" : "gui.scp_unleashed.sentry_turret.allow.row")
                    .withStyle(ChatFormatting.GRAY));
            if (Editable) {
                Lines.add(Component.translatable(inside(GuiX, GuiY, removeX(), RowY + (Row - Scroll) * RowHeight + 1, RemoveSize, RemoveSize)
                        ? "gui.scp_unleashed.sentry_turret.remove" : "gui.scp_unleashed.sentry_turret.flip").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return Lines;
    }

    private int rowAt(double GuiX, double GuiY) {
        SentryTurretBlockEntity Turret = turret();
        if (Turret == null || Tab != TabAccess || !inside(GuiX, GuiY, RowX, RowY, RowWidth, Rows * RowHeight)) {
            return -1;
        }
        int Index = Scroll + (int) ((GuiY - RowY) / RowHeight);
        return Index <= Turret.getRules().getEntries().size() ? Index : -1;
    }

    private static int toggleX(int Index) {
        return ToggleX + Index % 2 * TogglePitchX;
    }

    private static int toggleY(int Index) {
        return ToggleY + Index / 2 * TogglePitchY;
    }

    private static int removeX() {
        return RowX + RowWidth - RemoveSize - 2;
    }

    private static boolean inside(double GuiX, double GuiY, int X, int Y, int Width, int Height) {
        return GuiX >= X && GuiX < X + Width && GuiY >= Y && GuiY < Y + Height;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        double GuiX = MouseX - leftPos;
        double GuiY = MouseY - topPos;
        if (!inside(GuiX, GuiY, FieldX, FieldY, FieldWidth, FieldHeight)) {
            Field.setFocused(false);
        }
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int Index = 0; Index < TabX.length; Index++) {
                if (inside(GuiX, GuiY, TabX[Index], TabY, TabWidth, TabHeight)) {
                    if (Tab != Index) {
                        Tab = Index;
                        updateTab();
                        play(ModSounds.PanelSelect.get(), 1.0f);
                    }
                    return true;
                }
            }
            SentryTurretBlockEntity Turret = turret();
            if (Turret != null && editable()) {
                if (Tab == TabTargets) {
                    for (TurretCategory Category : TurretCategory.values()) {
                        if (inside(GuiX, GuiY, toggleX(Category.ordinal()), toggleY(Category.ordinal()), ToggleWidth, ToggleHeight)) {
                            send(TurretConfigPayload.ActionToggle, Category.ordinal(), "");
                            play(ModSounds.PanelPress.get(), Turret.getRules().has(Category) ? 0.9f : 1.1f);
                            return true;
                        }
                    }
                } else {
                    for (int Index = 0; Index < ButtonX.length; Index++) {
                        if (inside(GuiX, GuiY, ButtonX[Index], ButtonY, ButtonWidth, ButtonHeight)) {
                            Pressed = Index;
                            submit(Index == 1);
                            return true;
                        }
                    }
                    int Row = rowAt(GuiX, GuiY);
                    if (Row > 0) {
                        int Y = RowY + (Row - Scroll) * RowHeight + 1;
                        if (inside(GuiX, GuiY, removeX(), Y, RemoveSize, RemoveSize)) {
                            send(TurretConfigPayload.ActionRemove, Row - 1, "");
                            play(ModSounds.PanelRelease.get(), 0.8f);
                        } else {
                            send(TurretConfigPayload.ActionFlip, Row - 1, "");
                            play(ModSounds.PanelSelect.get(), 1.0f);
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (Pressed >= 0) {
            Pressed = -1;
            play(ModSounds.PanelRelease.get(), 1.0f);
        }
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        if (Tab == TabAccess && inside(MouseX - leftPos, MouseY - topPos, RowX, RowY, RowWidth + TrackWidth + 2, Rows * RowHeight) && ScrollY != 0.0) {
            Scroll = Math.max(0, Scroll - (int) Math.signum(ScrollY));
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    private void submit(boolean Deny) {
        String Text = Field.getValue();
        if (TurretRules.normalise(Text) == null) {
            play(ModSounds.PanelTick.get(), 0.6f);
            return;
        }
        send(Deny ? TurretConfigPayload.ActionDeny : TurretConfigPayload.ActionAllow, 0, Text.trim());
        play(ModSounds.PanelPress.get(), Deny ? 0.85f : 1.1f);
        Field.setValue("");
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        if (Field.isFocused() && Field.isVisible()) {
            if (Key == GLFW.GLFW_KEY_ESCAPE) {
                Field.setFocused(false);
                return true;
            }
            if (Key == GLFW.GLFW_KEY_TAB) {
                acceptSuggestion();
                return true;
            }
            if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER) {
                if (editable()) {
                    submit(false);
                }
                return true;
            }
            Field.keyPressed(Key, ScanCode, Modifiers);
            return true;
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    private void acceptSuggestion() {
        String Best = bestMatch(Field.getValue());
        if (Best != null) {
            Field.setValue(Best);
        }
    }

    private void send(int Action, int Index, String Text) {
        PacketDistributor.sendToServer(new TurretConfigPayload(menu.getPos(), Action, Index, Text));
    }

    private void play(SoundEvent Sound, float Pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, 0.3f));
        }
    }
}
