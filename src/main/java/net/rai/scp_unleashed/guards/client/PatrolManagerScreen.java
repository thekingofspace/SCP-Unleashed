package net.rai.scp_unleashed.guards.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.guards.GuardsRegistry;
import net.rai.scp_unleashed.guards.PatrolConfigPayload;
import net.rai.scp_unleashed.guards.PatrolManagerBlockEntity;
import net.rai.scp_unleashed.guards.PatrolManagerMenu;
import net.rai.scp_unleashed.guards.PatrolPoint;
import net.rai.scp_unleashed.guards.PatrolRoute;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.security.TurretCategory;
import net.rai.scp_unleashed.security.TurretRules;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatrolManagerScreen extends AbstractContainerScreen<PatrolManagerMenu> {
    public static final ResourceLocation Texture = GuardsRegistry.location("textures/gui/guards/patrol_manager.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 200;
    public static final int TitleY = 6;
    public static final int LcdX = 7;
    public static final int LcdY = 17;
    public static final int LcdWidth = 162;
    public static final int LcdHeight = 22;
    public static final int TabY = 42;
    public static final int TabWidth = 52;
    public static final int TabHeight = 14;
    public static final int[] TabX = {7, 62, 117};
    public static final int TabU = 176;
    public static final int TabV = 72;
    public static final int AreaX = 7;
    public static final int AreaY = 56;
    public static final int AreaWidth = 162;
    public static final int AreaHeight = 138;
    public static final int ModeX = 10;
    public static final int ModeY = 59;
    public static final int ModeWidth = 76;
    public static final int ModeHeight = 14;
    public static final int ModeU = 0;
    public static final int ModeV = 214;
    public static final int RouteRowY = 76;
    public static final int RouteRowHeight = 13;
    public static final int RouteRows = 8;
    public static final int RouteRowU = 96;
    public static final int RouteRowV = 213;
    public static final int CoordsWidth = 46;
    public static final int WaitRight = 88;
    public static final int SmallSize = 9;
    public static final int SmallU = 176;
    public static final int SmallV = 180;
    public static final int[] SmallX = {90, 100, 112, 122};
    public static final int ToggleX = 10;
    public static final int ToggleY = 59;
    public static final int ToggleWidth = 76;
    public static final int ToggleHeight = 24;
    public static final int TogglePitchX = 80;
    public static final int TogglePitchY = 26;
    public static final int ToggleU = 176;
    public static final int ToggleV = 0;
    public static final int OrderY = 141;
    public static final int[] OrderX = {10, 90};
    public static final int OrderIconU = 212;
    public static final int OrderIconV = 180;
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
    public static final int FieldV = 200;
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
    public static final int Rows = 9;
    public static final int RowU = 96;
    public static final int RowV = 200;
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
    public static final int HintY = 170;
    public static final int DarkText = 0xFF404040;
    public static final int LightText = 0xFFD8DCE0;
    public static final int DimText = 0xFF8C949C;
    public static final int LcdText = 0xFF7CF08C;
    public static final int LcdDim = 0xFF3C8C48;
    public static final int AllowText = 0xFF7CE07C;
    public static final int DenyText = 0xFFF06C5C;
    public static final int BadText = 0xFFF06C5C;
    public static final int WarnText = 0xFFF0C050;
    public static final int TabRoute = 0;
    public static final int TabRules = 1;
    public static final int TabAccess = 2;
    public static final int SmallMinus = 0;
    public static final int SmallPlus = 1;
    public static final int SmallUp = 2;
    public static final int SmallDown = 3;
    private static int RememberedTab = TabRoute;

    private EditBox Field;
    private int Tab = RememberedTab;
    private int Scroll;
    private int Pressed = -1;

    public PatrolManagerScreen(PatrolManagerMenu Menu, Inventory Pockets, Component Title) {
        super(Menu, Pockets, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
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
    private PatrolManagerBlockEntity manager() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(menu.getPos()) instanceof PatrolManagerBlockEntity Found ? Found : null;
    }

    private boolean editable() {
        PatrolManagerBlockEntity Manager = manager();
        return Manager != null && minecraft != null && minecraft.player != null && Manager.canEdit(minecraft.player);
    }

    private void updateTab() {
        RememberedTab = Tab;
        Scroll = 0;
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

    private int listSize(PatrolManagerBlockEntity Manager) {
        return Tab == TabRoute ? Manager.getPoints().size() : Manager.getRules().getEntries().size() + 1;
    }

    private int visibleRows() {
        return Tab == TabRoute ? RouteRows : Rows;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        PatrolManagerBlockEntity Manager = manager();
        int Count = Manager == null ? 0 : listSize(Manager);
        Scroll = Mth.clamp(Scroll, 0, Math.max(0, Count - visibleRows()));
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
        PatrolManagerBlockEntity Manager = manager();
        if (Manager == null) {
            return;
        }
        boolean Editable = editable();
        if (Tab == TabRoute) {
            renderRouteBg(Graphics, Manager, GuiX, GuiY, Editable);
        } else if (Tab == TabRules) {
            renderRulesBg(Graphics, Manager, GuiX, GuiY, Editable);
        } else {
            renderAccessBg(Graphics, Manager, GuiX, GuiY, Editable);
        }
    }

    private void renderRouteBg(GuiGraphics Graphics, PatrolManagerBlockEntity Manager, double GuiX, double GuiY, boolean Editable) {
        int ModeState = !Editable ? 2 : inside(GuiX, GuiY, ModeX, ModeY, ModeWidth, ModeHeight) ? 1 : 0;
        Graphics.blit(Texture, leftPos + ModeX, topPos + ModeY, ModeU, ModeV + ModeState * ModeHeight, ModeWidth, ModeHeight);
        List<PatrolPoint> Points = Manager.getPoints();
        for (int Row = 0; Row < RouteRows; Row++) {
            int Index = Scroll + Row;
            if (Index >= Points.size()) {
                break;
            }
            int Y = RouteRowY + Row * RouteRowHeight;
            if (inside(GuiX, GuiY, RowX, Y, RowWidth, RouteRowHeight)) {
                Graphics.blit(Texture, leftPos + RowX, topPos + Y, RouteRowU, RouteRowV, RowWidth, RouteRowHeight);
            }
            for (int Kind = 0; Kind < SmallX.length; Kind++) {
                boolean Usable = Editable && smallUsable(Kind, Index, Points);
                boolean Hover = Usable && inside(GuiX, GuiY, RowX + SmallX[Kind], Y + 2, SmallSize, SmallSize);
                int State = !Usable ? 2 : Hover ? 1 : 0;
                Graphics.blit(Texture, leftPos + RowX + SmallX[Kind], topPos + Y + 2, SmallU + Kind * SmallSize, SmallV + State * SmallSize, SmallSize, SmallSize);
            }
            if (Editable) {
                boolean HoverRemove = inside(GuiX, GuiY, removeX(), Y + 2, RemoveSize, RemoveSize);
                Graphics.blit(Texture, leftPos + removeX(), topPos + Y + 2, RemoveU + (HoverRemove ? RemoveSize : 0), RemoveV, RemoveSize, RemoveSize);
            }
        }
        thumb(Graphics, Points.size(), RouteRows, RouteRowY, RouteRowHeight);
    }

    private static boolean smallUsable(int Kind, int Index, List<PatrolPoint> Points) {
        return switch (Kind) {
            case SmallMinus -> Points.get(Index).Wait() > 0;
            case SmallPlus -> Points.get(Index).Wait() < PatrolPoint.MaxWait;
            case SmallUp -> Index > 0;
            default -> Index < Points.size() - 1;
        };
    }

    private void renderRulesBg(GuiGraphics Graphics, PatrolManagerBlockEntity Manager, double GuiX, double GuiY, boolean Editable) {
        for (TurretCategory Category : TurretCategory.values()) {
            int X = toggleX(Category.ordinal());
            int Y = toggleY(Category.ordinal());
            boolean On = Manager.getRules().has(Category);
            toggle(Graphics, X, Y, On, Editable && inside(GuiX, GuiY, X, Y, ToggleWidth, ToggleHeight));
            Graphics.blit(Texture, leftPos + X + 5, topPos + Y + 6, IconU + Category.ordinal() * IconSize, IconV, IconSize, IconSize);
        }
        boolean[] States = {!Manager.isHoldFire(), Manager.isMelee()};
        for (int Index = 0; Index < OrderX.length; Index++) {
            toggle(Graphics, OrderX[Index], OrderY, States[Index], Editable && inside(GuiX, GuiY, OrderX[Index], OrderY, ToggleWidth, ToggleHeight));
            Graphics.blit(Texture, leftPos + OrderX[Index] + 5, topPos + OrderY + 6, OrderIconU + Index * IconSize, OrderIconV, IconSize, IconSize);
        }
    }

    private void toggle(GuiGraphics Graphics, int X, int Y, boolean On, boolean Hover) {
        int State = Hover ? 2 : On ? 1 : 0;
        Graphics.blit(Texture, leftPos + X, topPos + Y, ToggleU, ToggleV + State * ToggleHeight, ToggleWidth, ToggleHeight);
        Graphics.blit(Texture, leftPos + X + ToggleWidth - 11, topPos + Y + 9, LampU + (On ? 2 : 0) * LampSize, LampV, LampSize, LampSize);
    }

    private void renderAccessBg(GuiGraphics Graphics, PatrolManagerBlockEntity Manager, double GuiX, double GuiY, boolean Editable) {
        Graphics.blit(Texture, leftPos + FieldX, topPos + FieldY, FieldU, FieldV, FieldWidth, FieldHeight);
        boolean Valid = Editable && TurretRules.normalise(Field.getValue()) != null;
        for (int Index = 0; Index < ButtonX.length; Index++) {
            boolean Hover = Valid && inside(GuiX, GuiY, ButtonX[Index], ButtonY, ButtonWidth, ButtonHeight);
            int State = !Valid ? 2 : Pressed == Index || Hover ? 1 : 0;
            Graphics.blit(Texture, leftPos + ButtonX[Index], topPos + ButtonY, ButtonU + Index * ButtonWidth, ButtonV + State * ButtonHeight, ButtonWidth, ButtonHeight);
        }
        int Count = Manager.getRules().getEntries().size() + 1;
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
            TurretRules.Entry Rule = Manager.getRules().getEntries().get(Index - 1);
            Graphics.blit(Texture, leftPos + RowX + 2, topPos + Y + 3, LampU + (Rule.Deny() ? 2 : 1) * LampSize, LampV, LampSize, LampSize);
            if (Editable) {
                boolean HoverRemove = inside(GuiX, GuiY, removeX(), Y + 1, RemoveSize, RemoveSize);
                Graphics.blit(Texture, leftPos + removeX(), topPos + Y + 1, RemoveU + (HoverRemove ? RemoveSize : 0), RemoveV, RemoveSize, RemoveSize);
            }
        }
        thumb(Graphics, Count, Rows, RowY, RowHeight);
    }

    private void thumb(GuiGraphics Graphics, int Count, int Visible, int Top, int Height) {
        if (Count <= Visible) {
            return;
        }
        int Travel = Visible * Height - ThumbHeight;
        int ThumbY = Top + Math.round(Travel * Scroll / (float) (Count - Visible));
        Graphics.blit(Texture, leftPos + TrackX, topPos + ThumbY, ThumbU, ThumbV, TrackWidth, ThumbHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, titleLabelX, titleLabelY, DarkText, false);
        PatrolManagerBlockEntity Manager = manager();
        String[] Names = {"gui.scp_unleashed.patrol_manager.tab.route", "gui.scp_unleashed.patrol_manager.tab.rules", "gui.scp_unleashed.patrol_manager.tab.access"};
        for (int Index = 0; Index < TabX.length; Index++) {
            Component Label = Component.translatable(Names[Index]);
            Graphics.drawString(font, Label, TabX[Index] + (TabWidth - font.width(Label)) / 2, TabY + 3, Index == Tab ? LightText : DarkText, false);
        }
        if (Manager == null) {
            return;
        }
        if (!editable()) {
            Component Locked = Component.translatable("gui.scp_unleashed.patrol_manager.view_only");
            Graphics.drawString(font, Locked, PanelWidth - 8 - font.width(Locked), titleLabelY, 0xFF8C3020, false);
        }
        int Count = Manager.getPoints().size();
        Component Summary = Count == 0 ? Component.translatable("gui.scp_unleashed.patrol_manager.lcd.empty")
                : Component.translatable("gui.scp_unleashed.patrol_manager.lcd.route", Count, Component.translatable(modeKey(Manager)).getString().toUpperCase(Locale.ROOT));
        Graphics.drawString(font, Summary, LcdX + 4, LcdY + 3, Count == 0 ? LcdDim : LcdText, false);
        Component Orders = Component.translatable("gui.scp_unleashed.patrol_manager.lcd.orders", Component.translatable(Manager.isHoldFire()
                ? "gui.scp_unleashed.patrol_manager.lcd.hold" : "gui.scp_unleashed.patrol_manager.lcd.free"), Manager.getIssued());
        Graphics.drawString(font, Orders, LcdX + 4, LcdY + 12, LcdDim, false);
        if (Tab == TabRoute) {
            renderRouteLabels(Graphics, Manager);
        } else if (Tab == TabRules) {
            renderRulesLabels(Graphics, Manager);
        } else {
            renderAccessLabels(Graphics, Manager);
        }
    }

    private static String modeKey(PatrolManagerBlockEntity Manager) {
        if (Manager.getPoints().size() == 1) {
            return "gui.scp_unleashed.patrol_manager.mode.post";
        }
        return Manager.isPingPong() ? "gui.scp_unleashed.patrol_manager.mode.ping_pong" : "gui.scp_unleashed.patrol_manager.mode.loop";
    }

    private void renderRouteLabels(GuiGraphics Graphics, PatrolManagerBlockEntity Manager) {
        Component Mode = Component.translatable(Manager.isPingPong() ? "gui.scp_unleashed.patrol_manager.mode.ping_pong" : "gui.scp_unleashed.patrol_manager.mode.loop");
        Graphics.drawString(font, Mode, ModeX + (ModeWidth - font.width(Mode)) / 2, ModeY + 3, editable() ? LightText : DimText, false);
        List<PatrolPoint> Points = Manager.getPoints();
        String Tally = Points.size() + "/" + PatrolRoute.MaxPoints;
        Graphics.drawString(font, Tally, AreaX + AreaWidth - 6 - font.width(Tally), ModeY + 3, DimText, false);
        if (Points.isEmpty()) {
            List<FormattedCharSequence> Lines = font.split(Component.translatable("gui.scp_unleashed.patrol_manager.route.empty"), AreaWidth - 12);
            for (int Line = 0; Line < Lines.size(); Line++) {
                Graphics.drawString(font, Lines.get(Line), RowX + 2, RouteRowY + 4 + Line * 10, DimText, false);
            }
            return;
        }
        for (int Row = 0; Row < RouteRows; Row++) {
            int Index = Scroll + Row;
            if (Index >= Points.size()) {
                break;
            }
            PatrolPoint Point = Points.get(Index);
            int Y = RouteRowY + Row * RouteRowHeight + 3;
            Graphics.drawString(font, String.valueOf(Index + 1), RowX + 2, Y, WarnText, false);
            BlockPos Pos = Point.Pos();
            Graphics.drawString(font, clip(Pos.getX() + " " + Pos.getY() + " " + Pos.getZ(), CoordsWidth), RowX + 14, Y, LightText, false);
            String Wait = seconds(Point.Wait());
            Graphics.drawString(font, Wait, RowX + WaitRight - font.width(Wait), Y, AllowText, false);
        }
        if (Points.size() == 1) {
            Graphics.drawString(font, Component.translatable("gui.scp_unleashed.patrol_manager.route.post"), RowX + 2, HintY + 10, DimText, false);
        }
    }

    private static String seconds(int Ticks) {
        float Value = Ticks / 20.0f;
        return (Value == Math.floor(Value) ? String.valueOf((int) Value) : String.format(Locale.ROOT, "%.1f", Value)) + "s";
    }

    private void renderRulesLabels(GuiGraphics Graphics, PatrolManagerBlockEntity Manager) {
        for (TurretCategory Category : TurretCategory.values()) {
            boolean On = Manager.getRules().has(Category);
            Graphics.drawString(font, Component.translatable(categoryKey(Category)), toggleX(Category.ordinal()) + 20, toggleY(Category.ordinal()) + 8, On ? LightText : DimText, false);
        }
        String[] Keys = {Manager.isHoldFire() ? "gui.scp_unleashed.patrol_manager.fire.hold_short" : "gui.scp_unleashed.patrol_manager.fire.free_short",
                Manager.isMelee() ? "gui.scp_unleashed.patrol_manager.melee.on_short" : "gui.scp_unleashed.patrol_manager.melee.off_short"};
        boolean[] States = {!Manager.isHoldFire(), Manager.isMelee()};
        for (int Index = 0; Index < OrderX.length; Index++) {
            Graphics.drawString(font, Component.translatable(Keys[Index]), OrderX[Index] + 20, OrderY + 8, States[Index] ? LightText : DimText, false);
        }
        List<FormattedCharSequence> Lines = font.split(Component.translatable("gui.scp_unleashed.patrol_manager.rules.hint"), AreaWidth - 10);
        for (int Line = 0; Line < Math.min(2, Lines.size()); Line++) {
            Graphics.drawString(font, Lines.get(Line), RowX, HintY + Line * 10, DimText, false);
        }
    }

    public static String categoryKey(TurretCategory Category) {
        return "gui.scp_unleashed.patrol_manager.category." + Category.getName();
    }

    private void renderAccessLabels(GuiGraphics Graphics, PatrolManagerBlockEntity Manager) {
        boolean Valid = editable() && TurretRules.normalise(Field.getValue()) != null;
        String[] Buttons = {"gui.scp_unleashed.sentry_turret.allow", "gui.scp_unleashed.sentry_turret.deny"};
        for (int Index = 0; Index < ButtonX.length; Index++) {
            Component Label = Component.translatable(Buttons[Index]);
            int Tint = !Valid ? DimText : Index == 0 ? AllowText : DenyText;
            Graphics.drawString(font, Label, ButtonX[Index] + (ButtonWidth - font.width(Label)) / 2, ButtonY + 3, Tint, false);
        }
        int Count = Manager.getRules().getEntries().size() + 1;
        for (int Row = 0; Row < Rows; Row++) {
            int Index = Scroll + Row;
            if (Index >= Count) {
                break;
            }
            int Y = RowY + Row * RowHeight + 2;
            if (Index == 0) {
                String Owner = Manager.getOwnerName().isEmpty() ? "-" : Manager.getOwnerName();
                Graphics.drawString(font, clip(Owner, removeX() - TagWidth - RowX - 14), RowX + 12, Y, LightText, false);
                Component Badge = Component.translatable("gui.scp_unleashed.sentry_turret.owner");
                Graphics.drawString(font, Badge, removeX() + RemoveSize - font.width(Badge), Y, AllowText, false);
                continue;
            }
            TurretRules.Entry Rule = Manager.getRules().getEntries().get(Index - 1);
            Graphics.drawString(font, clip(entryName(Rule).getString(), removeX() - TagWidth - RowX - 14), RowX + 11, Y, LightText, false);
            Component Badge = Component.translatable(Rule.Deny() ? "gui.scp_unleashed.patrol_manager.threat" : "gui.scp_unleashed.patrol_manager.spare");
            Graphics.drawString(font, Badge, removeX() - 2 - font.width(Badge), Y, Rule.Deny() ? DenyText : AllowText, false);
        }
    }

    private String clip(String Text, int Width) {
        if (font.width(Text) <= Width) {
            return Text;
        }
        return font.plainSubstrByWidth(Text, Width - font.width("..")) + "..";
    }

    private static Component entryName(TurretRules.Entry Rule) {
        if (!Rule.isType()) {
            return Component.literal(Rule.Key());
        }
        ResourceLocation Id = ResourceLocation.tryParse(Rule.Key());
        if (Id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(Id)) {
            return Component.literal(Rule.Key());
        }
        EntityType<?> Species = BuiltInRegistries.ENTITY_TYPE.get(Id);
        return Species.getDescription();
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        List<Component> Lines = tooltipAt(MouseX - leftPos, MouseY - topPos);
        if (!Lines.isEmpty()) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private List<Component> tooltipAt(double GuiX, double GuiY) {
        List<Component> Lines = new ArrayList<>();
        PatrolManagerBlockEntity Manager = manager();
        String[] Hints = {"gui.scp_unleashed.patrol_manager.tab.route.hint", "gui.scp_unleashed.patrol_manager.tab.rules.hint", "gui.scp_unleashed.patrol_manager.tab.access.hint"};
        for (int Index = 0; Index < TabX.length; Index++) {
            if (inside(GuiX, GuiY, TabX[Index], TabY, TabWidth, TabHeight)) {
                Lines.add(Component.translatable(Hints[Index]));
                return Lines;
            }
        }
        if (Manager == null) {
            return Lines;
        }
        boolean Editable = editable();
        if (inside(GuiX, GuiY, LcdX, LcdY, LcdWidth, LcdHeight)) {
            Lines.add(Component.translatable("gui.scp_unleashed.patrol_manager.lcd.hint"));
            return Lines;
        }
        if (Tab == TabRoute) {
            if (inside(GuiX, GuiY, ModeX, ModeY, ModeWidth, ModeHeight)) {
                Lines.add(Component.translatable(Manager.isPingPong() ? "gui.scp_unleashed.patrol_manager.mode.ping_pong" : "gui.scp_unleashed.patrol_manager.mode.loop")
                        .withStyle(ChatFormatting.GOLD));
                Lines.add(Component.translatable(Manager.isPingPong() ? "gui.scp_unleashed.patrol_manager.mode.ping_pong.hint" : "gui.scp_unleashed.patrol_manager.mode.loop.hint")
                        .withStyle(ChatFormatting.GRAY));
                return Lines;
            }
            int Row = routeRowAt(GuiX, GuiY);
            if (Row >= 0) {
                PatrolPoint Point = Manager.getPoints().get(Row);
                BlockPos Pos = Point.Pos();
                Lines.add(Component.translatable("gui.scp_unleashed.patrol_manager.route.row", Row + 1, Pos.getX(), Pos.getY(), Pos.getZ()).withStyle(ChatFormatting.GOLD));
                Lines.add(Component.translatable("gui.scp_unleashed.patrol_manager.route.wait", seconds(Point.Wait())).withStyle(ChatFormatting.GRAY));
                int Y = RouteRowY + (Row - Scroll) * RouteRowHeight + 2;
                String[] Small = {"minus", "plus", "up", "down"};
                for (int Kind = 0; Kind < SmallX.length; Kind++) {
                    if (inside(GuiX, GuiY, RowX + SmallX[Kind], Y, SmallSize, SmallSize)) {
                        Lines.add(Component.translatable("gui.scp_unleashed.patrol_manager.route." + Small[Kind]).withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
                if (Editable && inside(GuiX, GuiY, removeX(), Y, RemoveSize, RemoveSize)) {
                    Lines.add(Component.translatable("gui.scp_unleashed.patrol_manager.route.remove").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            return Lines;
        }
        if (Tab == TabRules) {
            for (TurretCategory Category : TurretCategory.values()) {
                if (inside(GuiX, GuiY, toggleX(Category.ordinal()), toggleY(Category.ordinal()), ToggleWidth, ToggleHeight)) {
                    boolean On = Manager.getRules().has(Category);
                    Lines.add(Component.translatable(categoryKey(Category)).withStyle(On ? ChatFormatting.RED : ChatFormatting.GRAY));
                    Lines.add(Component.translatable(categoryKey(Category) + ".hint").withStyle(ChatFormatting.GRAY));
                    Lines.add(Component.translatable(On ? "gui.scp_unleashed.patrol_manager.engaged" : "gui.scp_unleashed.patrol_manager.ignored")
                            .withStyle(On ? ChatFormatting.RED : ChatFormatting.GREEN));
                    return Lines;
                }
            }
            if (inside(GuiX, GuiY, OrderX[0], OrderY, ToggleWidth, ToggleHeight)) {
                Lines.add(Component.translatable(Manager.isHoldFire() ? "gui.scp_unleashed.patrol_manager.fire.hold" : "gui.scp_unleashed.patrol_manager.fire.free")
                        .withStyle(Manager.isHoldFire() ? ChatFormatting.YELLOW : ChatFormatting.RED));
                Lines.add(Component.translatable("gui.scp_unleashed.patrol_manager.fire.hint").withStyle(ChatFormatting.GRAY));
                return Lines;
            }
            if (inside(GuiX, GuiY, OrderX[1], OrderY, ToggleWidth, ToggleHeight)) {
                Lines.add(Component.translatable(Manager.isMelee() ? "gui.scp_unleashed.patrol_manager.melee.on" : "gui.scp_unleashed.patrol_manager.melee.off")
                        .withStyle(ChatFormatting.GOLD));
                Lines.add(Component.translatable("gui.scp_unleashed.patrol_manager.melee.hint").withStyle(ChatFormatting.GRAY));
                return Lines;
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
                Lines.add(Component.translatable(Index == 0 ? "gui.scp_unleashed.patrol_manager.allow.hint" : "gui.scp_unleashed.patrol_manager.deny.hint"));
                return Lines;
            }
        }
        int Row = accessRowAt(GuiX, GuiY);
        if (Row == 0) {
            Lines.add(Component.translatable("gui.scp_unleashed.patrol_manager.owner.hint"));
        } else if (Row > 0) {
            TurretRules.Entry Rule = Manager.getRules().getEntries().get(Row - 1);
            Lines.add(entryName(Rule).copy().withStyle(Rule.Deny() ? ChatFormatting.RED : ChatFormatting.GREEN));
            if (Rule.isType()) {
                Lines.add(Component.literal(Rule.Key()).withStyle(ChatFormatting.DARK_GRAY));
            }
            Lines.add(Component.translatable(Rule.Deny() ? "gui.scp_unleashed.patrol_manager.threat.row" : "gui.scp_unleashed.patrol_manager.spare.row")
                    .withStyle(ChatFormatting.GRAY));
            if (Editable) {
                Lines.add(Component.translatable(inside(GuiX, GuiY, removeX(), RowY + (Row - Scroll) * RowHeight + 1, RemoveSize, RemoveSize)
                        ? "gui.scp_unleashed.sentry_turret.remove" : "gui.scp_unleashed.sentry_turret.flip").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return Lines;
    }

    private int routeRowAt(double GuiX, double GuiY) {
        PatrolManagerBlockEntity Manager = manager();
        if (Manager == null || Tab != TabRoute || !inside(GuiX, GuiY, RowX, RouteRowY, RowWidth, RouteRows * RouteRowHeight)) {
            return -1;
        }
        int Index = Scroll + (int) ((GuiY - RouteRowY) / RouteRowHeight);
        return Index < Manager.getPoints().size() ? Index : -1;
    }

    private int accessRowAt(double GuiX, double GuiY) {
        PatrolManagerBlockEntity Manager = manager();
        if (Manager == null || Tab != TabAccess || !inside(GuiX, GuiY, RowX, RowY, RowWidth, Rows * RowHeight)) {
            return -1;
        }
        int Index = Scroll + (int) ((GuiY - RowY) / RowHeight);
        return Index <= Manager.getRules().getEntries().size() ? Index : -1;
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
        if (Button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(MouseX, MouseY, Button);
        }
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
        PatrolManagerBlockEntity Manager = manager();
        if (Manager == null || !editable()) {
            return super.mouseClicked(MouseX, MouseY, Button);
        }
        if (Tab == TabRoute && clickRoute(Manager, GuiX, GuiY)) {
            return true;
        }
        if (Tab == TabRules && clickRules(Manager, GuiX, GuiY)) {
            return true;
        }
        if (Tab == TabAccess && clickAccess(GuiX, GuiY)) {
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    private boolean clickRoute(PatrolManagerBlockEntity Manager, double GuiX, double GuiY) {
        if (inside(GuiX, GuiY, ModeX, ModeY, ModeWidth, ModeHeight)) {
            send(PatrolConfigPayload.ActionMode, 0, 0, "");
            play(ModSounds.PanelPress.get(), Manager.isPingPong() ? 0.9f : 1.1f);
            return true;
        }
        int Row = routeRowAt(GuiX, GuiY);
        if (Row < 0) {
            return false;
        }
        int Y = RouteRowY + (Row - Scroll) * RouteRowHeight + 2;
        List<PatrolPoint> Points = Manager.getPoints();
        for (int Kind = 0; Kind < SmallX.length; Kind++) {
            if (!inside(GuiX, GuiY, RowX + SmallX[Kind], Y, SmallSize, SmallSize)) {
                continue;
            }
            if (!smallUsable(Kind, Row, Points)) {
                play(ModSounds.PanelTick.get(), 0.6f);
                return true;
            }
            int Step = hasShiftDown() ? PatrolPoint.WaitStep * 5 : PatrolPoint.WaitStep;
            switch (Kind) {
                case SmallMinus -> send(PatrolConfigPayload.ActionWait, Row, -Step, "");
                case SmallPlus -> send(PatrolConfigPayload.ActionWait, Row, Step, "");
                case SmallUp -> send(PatrolConfigPayload.ActionMove, Row, -1, "");
                default -> send(PatrolConfigPayload.ActionMove, Row, 1, "");
            }
            play(ModSounds.PanelSelect.get(), Kind == SmallMinus || Kind == SmallDown ? 0.9f : 1.1f);
            return true;
        }
        if (inside(GuiX, GuiY, removeX(), Y, RemoveSize, RemoveSize)) {
            send(PatrolConfigPayload.ActionUnlink, Row, 0, "");
            play(ModSounds.PanelRelease.get(), 0.8f);
            return true;
        }
        return false;
    }

    private boolean clickRules(PatrolManagerBlockEntity Manager, double GuiX, double GuiY) {
        for (TurretCategory Category : TurretCategory.values()) {
            if (inside(GuiX, GuiY, toggleX(Category.ordinal()), toggleY(Category.ordinal()), ToggleWidth, ToggleHeight)) {
                send(PatrolConfigPayload.ActionToggle, Category.ordinal(), 0, "");
                play(ModSounds.PanelPress.get(), Manager.getRules().has(Category) ? 0.9f : 1.1f);
                return true;
            }
        }
        if (inside(GuiX, GuiY, OrderX[0], OrderY, ToggleWidth, ToggleHeight)) {
            send(PatrolConfigPayload.ActionFire, 0, 0, "");
            play(ModSounds.PanelPress.get(), Manager.isHoldFire() ? 1.1f : 0.9f);
            return true;
        }
        if (inside(GuiX, GuiY, OrderX[1], OrderY, ToggleWidth, ToggleHeight)) {
            send(PatrolConfigPayload.ActionMelee, 0, 0, "");
            play(ModSounds.PanelPress.get(), Manager.isMelee() ? 0.9f : 1.1f);
            return true;
        }
        return false;
    }

    private boolean clickAccess(double GuiX, double GuiY) {
        for (int Index = 0; Index < ButtonX.length; Index++) {
            if (inside(GuiX, GuiY, ButtonX[Index], ButtonY, ButtonWidth, ButtonHeight)) {
                Pressed = Index;
                submit(Index == 1);
                return true;
            }
        }
        int Row = accessRowAt(GuiX, GuiY);
        if (Row > 0) {
            int Y = RowY + (Row - Scroll) * RowHeight + 1;
            if (inside(GuiX, GuiY, removeX(), Y, RemoveSize, RemoveSize)) {
                send(PatrolConfigPayload.ActionRemove, Row - 1, 0, "");
                play(ModSounds.PanelRelease.get(), 0.8f);
            } else {
                send(PatrolConfigPayload.ActionFlip, Row - 1, 0, "");
                play(ModSounds.PanelSelect.get(), 1.0f);
            }
            return true;
        }
        return false;
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
        int Top = Tab == TabRoute ? RouteRowY : RowY;
        int Height = Tab == TabRoute ? RouteRows * RouteRowHeight : Rows * RowHeight;
        if (Tab != TabRules && inside(MouseX - leftPos, MouseY - topPos, RowX, Top, RowWidth + TrackWidth + 2, Height) && ScrollY != 0.0) {
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
        send(Deny ? PatrolConfigPayload.ActionDeny : PatrolConfigPayload.ActionAllow, 0, 0, Text.trim());
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
                String Best = bestMatch(Field.getValue());
                if (Best != null) {
                    Field.setValue(Best);
                }
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

    private void send(int Action, int Index, int Value, String Text) {
        PacketDistributor.sendToServer(new PatrolConfigPayload(menu.getPos(), Action, Index, Value, Text));
    }

    private void play(SoundEvent Sound, float Pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, 0.3f));
        }
    }
}
