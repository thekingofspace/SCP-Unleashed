package net.rai.scp_unleashed.rifttech.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.matrix.InstabilityMatrixBlock;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import net.rai.scp_unleashed.rifttech.GridStatus;
import net.rai.scp_unleashed.rifttech.GridTuning;
import net.rai.scp_unleashed.rifttech.PylonGrid;
import net.rai.scp_unleashed.rifttech.PylonMenu;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class PylonScreen extends AbstractContainerScreen<PylonMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/rifttech/instability_pylon.png");
    public static final int PanelWidth = 220;
    public static final int PanelHeight = 166;
    public static final int TitleX = 10;
    public static final int TitleY = 7;
    public static final int TitleColour = 0xFFE4E2D6;
    public static final int EngraveColour = 0xFF8E97A2;
    public static final int MapX = 10;
    public static final int MapY = 24;
    public static final int MapSize = 68;
    public static final int MapInset = 9;
    public static final int ChargeX = 10;
    public static final int ChargeY = 104;
    public static final int ChargeWidth = 68;
    public static final int ChargeHeight = 6;
    public static final int ChargeU = 100;
    public static final int ChargeV = 212;
    public static final int HeaderY = 26;
    public static final int HeaderLeft = 91;
    public static final int HeaderRight = 182;
    public static final int ButtonX = 88;
    public static final int ButtonTop = 36;
    public static final int ButtonStep = 14;
    public static final int ButtonWidth = 96;
    public static final int ButtonHeight = 13;
    public static final int ButtonU = 0;
    public static final int ButtonNormalV = 170;
    public static final int ButtonHoverV = 184;
    public static final int ButtonSelectedV = 198;
    public static final int GoldNormalV = 212;
    public static final int GoldHoverV = 226;
    public static final int GoldSelectedV = 240;
    public static final int ApocalypticTop = ButtonTop + 5 * ButtonStep + 3;
    public static final int LedU = 100;
    public static final int LedV = 170;
    public static final int LedSize = 7;
    public static final int LedOffsetX = 4;
    public static final int LedOffsetY = 3;
    public static final int NameOffsetX = 15;
    public static final int TextOffsetY = 3;
    public static final int PowerX = 195;
    public static final int PowerY = 25;
    public static final int PowerWidth = 14;
    public static final int PowerHeight = 94;
    public static final int PowerU = 224;
    public static final int PowerV = 0;
    public static final int LcdX = 10;
    public static final int LcdY = 130;
    public static final int LcdWidth = 158;
    public static final int LcdHeight = 22;
    public static final int LampX = 14;
    public static final int LampY = 133;
    public static final int LampSize = 6;
    public static final int LampU = 116;
    public static final int LampV = 170;
    public static final int StatusTextX = 24;
    public static final int StatusTextY = 133;
    public static final int ReadoutX = 14;
    public static final int ReadoutY = 143;
    public static final int ReadoutColour = 0xFF8CFFA0;
    public static final int ReadoutDim = 0xFF3F7A4C;
    public static final int ToggleX = 176;
    public static final int ToggleY = 128;
    public static final int ToggleWidth = 36;
    public static final int ToggleHeight = 26;
    public static final int ToggleU = 100;
    public static final int ToggleV = 184;
    public static final int ToggleStep = 38;

    private int Ticks;
    private int Pressed = -1;

    public PylonScreen(PylonMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
    }

    private static int buttonY(int Index) {
        return ButtonTop + Index * ButtonStep;
    }

    private static float[] rgb(int Colour) {
        return new float[]{((Colour >> 16) & 255) / 255.0f, ((Colour >> 8) & 255) / 255.0f, (Colour & 255) / 255.0f};
    }

    private static int argb(float Alpha, float Red, float Green, float Blue) {
        return (Mth.clamp(Math.round(Alpha * 255.0f), 0, 255) << 24) | (Mth.clamp(Math.round(Red * 255.0f), 0, 255) << 16)
                | (Mth.clamp(Math.round(Green * 255.0f), 0, 255) << 8) | Mth.clamp(Math.round(Blue * 255.0f), 0, 255);
    }

    private float beat(float PartialTick) {
        float Time = Ticks + PartialTick;
        return 0.72f + 0.28f * (0.5f + 0.5f * Mth.sin(Time * 0.2f));
    }

    private boolean golden() {
        return menu.isApocalyptic();
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        GridStatus Status = menu.getStatus();
        FragmentRarity Selected = menu.getTier();
        boolean Golden = golden();
        RenderSystem.enableBlend();
        map(Graphics, PartialTick, Status);
        charge(Graphics, PartialTick, Status);
        for (FragmentRarity Tier : FragmentRarity.values()) {
            int Index = Tier.getTier() - 1;
            int Y = topPos + buttonY(Index);
            boolean Chosen = Tier == Selected && !Golden;
            boolean Hover = !Chosen && isHovering(ButtonX, buttonY(Index), ButtonWidth, ButtonHeight, MouseX, MouseY);
            int V = Chosen || Pressed == Index ? ButtonSelectedV : Hover ? ButtonHoverV : ButtonNormalV;
            Graphics.blit(Texture, leftPos + ButtonX, Y, ButtonU, V, ButtonWidth, ButtonHeight);
            led(Graphics, ButtonX, buttonY(Index), Tier.getColour(), Chosen, Status, PartialTick);
        }
        boolean OverGold = isHovering(ButtonX, ApocalypticTop, ButtonWidth, ButtonHeight, MouseX, MouseY);
        int GoldV = Golden || Pressed == PylonMenu.ApocalypticButton ? GoldSelectedV : OverGold ? GoldHoverV : GoldNormalV;
        Graphics.blit(Texture, leftPos + ButtonX, topPos + ApocalypticTop, ButtonU, GoldV, ButtonWidth, ButtonHeight);
        led(Graphics, ButtonX, ApocalypticTop, ApocalypticRiftEntity.Colour, Golden, Status, PartialTick);
        power(Graphics);
        int Lamp = lampIndex(Status);
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * (LampSize + 1), LampV, LampSize, LampSize);
        }
        boolean On = menu.isEnabled();
        boolean OverToggle = isHovering(ToggleX, ToggleY, ToggleWidth, ToggleHeight, MouseX, MouseY);
        int Frame = (On ? 2 : 0) + (OverToggle || Pressed == PylonMenu.ToggleButton ? 1 : 0);
        Graphics.blit(Texture, leftPos + ToggleX, topPos + ToggleY, ToggleU + Frame * ToggleStep, ToggleV, ToggleWidth, ToggleHeight);
        RenderSystem.disableBlend();
    }

    private void led(GuiGraphics Graphics, int X, int Y, int Colour, boolean Chosen, GridStatus Status, float PartialTick) {
        float[] Tint = rgb(Colour);
        float Shine = Chosen ? (Status.isCharging() ? beat(PartialTick) : 0.85f) : 0.42f;
        Graphics.setColor(Tint[0] * Shine, Tint[1] * Shine, Tint[2] * Shine, 1.0f);
        Graphics.blit(Texture, leftPos + X + LedOffsetX, topPos + Y + LedOffsetY + (Chosen ? 1 : 0), Chosen ? LedU : LedU + LedSize + 1, LedV, LedSize, LedSize);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void power(GuiGraphics Graphics) {
        int Demand = menu.getGridDemand();
        float Share = Demand <= 0 ? 0.0f : Mth.clamp(menu.getIntake() / (float) Demand, 0.0f, 1.0f);
        int Filled = Math.round(Share * PowerHeight);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + PowerX, topPos + PowerY + PowerHeight - Filled, PowerU, PowerV + PowerHeight - Filled, PowerWidth, Filled);
        }
    }

    private void charge(GuiGraphics Graphics, float PartialTick, GridStatus Status) {
        boolean Golden = golden();
        float Amount = Golden ? menu.getApocalypticCharge() : menu.getCharge();
        int Filled = Math.round(Amount * ChargeWidth);
        if (Filled <= 0) {
            return;
        }
        float[] Tint = rgb(Golden ? ApocalypticRiftEntity.Colour : menu.getTier().getColour());
        float Shine = Status.isCharging() ? beat(PartialTick) : 0.55f;
        Graphics.setColor(Mth.lerp(0.2f, Tint[0], 1.0f) * Shine, Mth.lerp(0.2f, Tint[1], 1.0f) * Shine, Mth.lerp(0.2f, Tint[2], 1.0f) * Shine, 1.0f);
        Graphics.blit(Texture, leftPos + ChargeX, topPos + ChargeY, ChargeU, ChargeV, Filled, ChargeHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void map(GuiGraphics Graphics, float PartialTick, GridStatus Status) {
        VertexConsumer Consumer = Graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f Matrix = Graphics.pose().last().pose();
        int SizeX = menu.getSizeX();
        int SizeZ = menu.getSizeZ();
        int Inner = MapSize - 2 * MapInset;
        int Left = leftPos + MapX + MapInset;
        int Top = topPos + MapY + MapInset;
        float[] Tint = rgb(golden() ? ApocalypticRiftEntity.Colour : menu.getTier().getColour());
        if (SizeX > 0 && SizeZ > 0) {
            float Scale = Inner / (float) Math.max(SizeX, SizeZ);
            int Width = Math.max(2, Math.round(SizeX * Scale));
            int Depth = Math.max(2, Math.round(SizeZ * Scale));
            int X0 = Left + (Inner - Width) / 2;
            int Z0 = Top + (Inner - Depth) / 2;
            int X1 = X0 + Width;
            int Z1 = Z0 + Depth;
            boolean Formed = Status.isFormed();
            float Strength = Formed ? (Status.isCharging() ? beat(PartialTick) : 0.55f) : 0.25f;
            int Edge = argb(0.9f, Mth.lerp(0.35f, Tint[0], 1.0f) * Strength, Mth.lerp(0.35f, Tint[1], 1.0f) * Strength, Mth.lerp(0.35f, Tint[2], 1.0f) * Strength);
            for (int X = X0; X <= X1; X++) {
                if (Formed || (X - X0) % 3 != 2) {
                    pixel(Consumer, Matrix, X, Z0, Edge);
                    pixel(Consumer, Matrix, X, Z1, Edge);
                }
            }
            for (int Z = Z0; Z <= Z1; Z++) {
                if (Formed || (Z - Z0) % 3 != 2) {
                    pixel(Consumer, Matrix, X0, Z, Edge);
                    pixel(Consumer, Matrix, X1, Z, Edge);
                }
            }
            int[][] Spots = {{X0, Z0}, {X1, Z0}, {X0, Z1}, {X1, Z1}};
            int Mask = menu.getMask();
            int Mine = menu.getCornerIndex();
            boolean Box = menu.getRise() > 0;
            for (int Index = 0; Index < 4; Index++) {
                boolean Lower = (Mask & (1 << Index)) != 0;
                boolean Upper = !Box || (Mask & (1 << (Index + 4))) != 0;
                boolean Lit = Lower && Upper;
                int Colour = Lit ? 0xFF6EF08C : 0xFFF0483C;
                if (Mine % 4 == Index && Mine >= 0 && (Ticks / 6) % 2 == 0) {
                    Colour = 0xFFF2F2EC;
                }
                dot(Consumer, Matrix, Spots[Index][0], Spots[Index][1], Colour, Box);
            }
        }
        BlockPos Issue = menu.getIssueOffset();
        if (Issue != null && menu.getIssue() != PylonGrid.Problem.None && SizeX == 0) {
            int Range = GridTuning.MaxSide;
            float Scale = (Inner / 2.0f) / Range;
            int Cx = Left + Inner / 2;
            int Cz = Top + Inner / 2;
            dot(Consumer, Matrix, Cx, Cz, 0xFFF2F2EC, false);
            if ((Ticks / 8) % 2 == 0) {
                int X = Cx + Math.round(Mth.clamp(Issue.getX() * Scale, -Inner / 2.0f, Inner / 2.0f));
                int Z = Cz + Math.round(Mth.clamp(Issue.getZ() * Scale, -Inner / 2.0f, Inner / 2.0f));
                cross(Consumer, Matrix, X, Z, 0xFFF0483C);
            }
        }
        Graphics.flush();
    }

    private static void dot(VertexConsumer Consumer, Matrix4f Matrix, int X, int Y, int Colour, boolean Double) {
        for (int OffsetY = -1; OffsetY <= 1; OffsetY++) {
            for (int OffsetX = -1; OffsetX <= 1; OffsetX++) {
                int Shade = OffsetX == 0 && OffsetY == 0 ? Colour : (Colour & 0xFFFFFF) | 0x99000000;
                pixel(Consumer, Matrix, X + OffsetX, Y + OffsetY, Shade);
            }
        }
        if (Double) {
            pixel(Consumer, Matrix, X + 2, Y - 2, Colour);
            pixel(Consumer, Matrix, X + 3, Y - 3, (Colour & 0xFFFFFF) | 0x99000000);
        }
    }

    private static void cross(VertexConsumer Consumer, Matrix4f Matrix, int X, int Y, int Colour) {
        for (int Step = -2; Step <= 2; Step++) {
            pixel(Consumer, Matrix, X + Step, Y + Step, Colour);
            pixel(Consumer, Matrix, X + Step, Y - Step, Colour);
        }
    }

    private static void pixel(VertexConsumer Consumer, Matrix4f Matrix, int X, int Y, int Colour) {
        Consumer.addVertex(Matrix, X, Y, 0.0f).setColor(Colour);
        Consumer.addVertex(Matrix, X, Y + 1, 0.0f).setColor(Colour);
        Consumer.addVertex(Matrix, X + 1, Y + 1, 0.0f).setColor(Colour);
        Consumer.addVertex(Matrix, X + 1, Y, 0.0f).setColor(Colour);
    }

    private int lampIndex(GridStatus Status) {
        return switch (Status) {
            case Charging -> 0;
            case Apocalyptic -> Ticks % 16 < 10 ? 4 : -1;
            case Holding -> 4;
            case LowPower, Obstructed, Starved, Searching, Conflict -> Ticks % 20 < 12 ? 1 : -1;
            case Unpowered, CornerUnpowered -> Ticks % 20 < 10 ? 2 : -1;
            case Saturated -> 3;
            default -> -1;
        };
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, TitleX, TitleY, TitleColour, false);
        Component Left = Component.translatable("gui.scp_unleashed.instability_pylon.target");
        Component Right = Component.translatable("gui.scp_unleashed.instability_matrix.unit");
        Graphics.drawString(font, Left, HeaderLeft, HeaderY, EngraveColour, false);
        Graphics.drawString(font, Right, HeaderRight - font.width(Right), HeaderY, EngraveColour, false);
        FragmentRarity Selected = menu.getTier();
        boolean Golden = golden();
        boolean Box = menu.getRise() > 0;
        for (FragmentRarity Tier : FragmentRarity.values()) {
            boolean Chosen = Tier == Selected && !Golden;
            int Y = buttonY(Tier.getTier() - 1) + TextOffsetY + (Chosen ? 1 : 0);
            label(Graphics, Component.translatable(InstabilityMatrixBlock.tierKey(Tier)), GridTuning.grouped(GridTuning.drawFor(Tier, Box)), Y, Tier.getColour(), Chosen);
        }
        int GoldY = ApocalypticTop + TextOffsetY + (Golden ? 1 : 0);
        label(Graphics, Component.translatable("gui.scp_unleashed.instability_pylon.apocalyptic"), "", GoldY, ApocalypticRiftEntity.Colour, Golden);
        GridStatus Status = menu.getStatus();
        Integer StatusColour = Status.getColour().getColor();
        Graphics.drawString(font, Component.translatable(Status.getTranslationKey()), StatusTextX, StatusTextY, 0xFF000000 | (StatusColour == null ? 0xAAAAAA : StatusColour), false);
        Graphics.drawString(font, readout(Status), ReadoutX, ReadoutY, Status.isFormed() ? ReadoutColour : ReadoutDim, false);
        Component Size = sizeLabel();
        Graphics.drawString(font, Size, MapX + (MapSize - font.width(Size)) / 2, MapY + MapSize + 1, EngraveColour, false);
    }

    private void label(GuiGraphics Graphics, Component Name, String Draw, int Y, int Colour, boolean Chosen) {
        float[] Tint = rgb(Colour);
        float Lift = Chosen ? 0.25f : 0.12f;
        float Dim = Chosen ? 1.0f : 0.86f;
        int NameColour = argb(1.0f, Mth.lerp(Lift, Tint[0], 1.0f) * Dim, Mth.lerp(Lift, Tint[1], 1.0f) * Dim, Mth.lerp(Lift, Tint[2], 1.0f) * Dim);
        Graphics.drawString(font, Name, ButtonX + NameOffsetX, Y, NameColour, false);
        Graphics.drawString(font, Draw, ButtonX + ButtonWidth - 5 - font.width(Draw), Y, Chosen ? 0xFFF2F2EC : 0xFFA9B0B8, false);
    }

    private Component sizeLabel() {
        if (menu.getSizeX() <= 0) {
            return Component.translatable("gui.scp_unleashed.instability_pylon.no_grid");
        }
        return menu.getRise() > 0
                ? Component.translatable("gui.scp_unleashed.instability_pylon.size_box", menu.getSizeX(), menu.getSizeZ(), menu.getRise())
                : Component.translatable("gui.scp_unleashed.instability_pylon.size_flat", menu.getSizeX(), menu.getSizeZ());
    }

    private Component readout(GridStatus Status) {
        PylonGrid.Problem Issue = menu.getIssue();
        BlockPos Offset = menu.getIssueOffset();
        if (!Status.isFormed() && Issue != PylonGrid.Problem.None) {
            return Offset != null && Issue.hasSpot()
                    ? Component.translatable(Issue.getTranslationKey() + ".at", offsetText(Offset))
                    : Component.translatable(Issue.getTranslationKey());
        }
        if (Status == GridStatus.Unpowered) {
            return Component.translatable("gui.scp_unleashed.instability_pylon.readout_unpowered", GridTuning.StandbyDraw);
        }
        if (Status.isCharging()) {
            float Amount = golden() ? menu.getApocalypticCharge() : menu.getCharge();
            return Component.translatable("gui.scp_unleashed.instability_pylon.readout_charge", Math.round(Amount * 100.0f), GridTuning.clock(menu.getEta()));
        }
        if (Status.isFormed()) {
            return Component.translatable("gui.scp_unleashed.instability_pylon.readout_field", menu.getRifts(), menu.getCap());
        }
        return Component.translatable("gui.scp_unleashed.instability_pylon.readout_search");
    }

    private static Component offsetText(BlockPos Offset) {
        return Component.translatable("gui.scp_unleashed.instability_pylon.offset", signed(Offset.getX()), signed(Offset.getY()), signed(Offset.getZ()));
    }

    private static String signed(int Value) {
        return Value > 0 ? "+" + Value : Integer.toString(Value);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        List<Component> Lines = tooltip(MouseX, MouseY);
        if (Lines.isEmpty()) {
            renderTooltip(Graphics, MouseX, MouseY);
        } else {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private List<Component> tooltip(int MouseX, int MouseY) {
        List<Component> Lines = new ArrayList<>();
        GridStatus Status = menu.getStatus();
        boolean Box = menu.getRise() > 0;
        for (FragmentRarity Tier : FragmentRarity.values()) {
            if (!isHovering(ButtonX, buttonY(Tier.getTier() - 1), ButtonWidth, ButtonHeight, MouseX, MouseY)) {
                continue;
            }
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.tier_title", Component.translatable(InstabilityMatrixBlock.tierKey(Tier)))
                    .withStyle(Style.EMPTY.withColor(Tier.getColour() & 0xFFFFFF)));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.tier_draw", GridTuning.grouped(GridTuning.drawFor(Tier, Box))));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.tier_time", GridTuning.clock(GridTuning.secondsFor(Tier, Box))).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.tier_select").withStyle(ChatFormatting.DARK_GRAY));
            return Lines;
        }
        if (isHovering(ButtonX, ApocalypticTop, ButtonWidth, ButtonHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.apocalyptic").withStyle(Style.EMPTY.withColor(ApocalypticRiftEntity.Colour & 0xFFFFFF)));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.apocalyptic_draw", GridTuning.grouped(GridTuning.ApocalypticDraw),
                    GridTuning.grouped(GridTuning.ApocalypticMinIntake)));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.apocalyptic_time", GridTuning.apocalypticMinutes()).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.apocalyptic_hint").withStyle(ChatFormatting.DARK_GRAY));
            return Lines;
        }
        if (isHovering(MapX, MapY, MapSize, MapSize + 10, MouseX, MouseY)) {
            Lines.add(sizeLabel());
            if (menu.getCorners() > 0) {
                Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.corners", Integer.bitCount(menu.getMask()), menu.getCorners()).withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.rifts", menu.getRifts(), menu.getCap()).withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.formed", menu.getFormed()).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.map_hint", GridTuning.MinSide, GridTuning.MaxSide).withStyle(ChatFormatting.GRAY));
            }
        } else if (isHovering(ChargeX - 1, ChargeY - 1, ChargeWidth + 2, ChargeHeight + 2, MouseX, MouseY)) {
            boolean Golden = golden();
            Lines.add(Component.translatable(Golden ? "gui.scp_unleashed.instability_pylon.apocalyptic_charge" : "gui.scp_unleashed.instability_pylon.charge",
                    Math.round((Golden ? menu.getApocalypticCharge() : menu.getCharge()) * 100.0f)));
            Lines.add(Component.translatable(Golden ? "gui.scp_unleashed.instability_pylon.apocalyptic_charge_hint" : "gui.scp_unleashed.instability_pylon.charge_hint")
                    .withStyle(ChatFormatting.GRAY));
        } else if (isHovering(PowerX - 1, PowerY - 1, PowerWidth + 2, PowerHeight + 2, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.power", GridTuning.grouped(menu.getIntake()), GridTuning.grouped(menu.getGridDemand())));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.power_own", GridTuning.grouped(menu.getOwnReceived()), GridTuning.grouped(menu.getOwnDemand()))
                    .withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.power_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(LcdX, LcdY, LcdWidth, LcdHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
            Lines.add(Component.translatable(Status.getHintKey()).withStyle(ChatFormatting.GRAY));
            PylonGrid.Problem Issue = menu.getIssue();
            BlockPos Offset = menu.getIssueOffset();
            if (Issue != PylonGrid.Problem.None) {
                Lines.add((Offset != null && Issue.hasSpot() ? Component.translatable(Issue.getTranslationKey() + ".at", offsetText(Offset))
                        : Component.translatable(Issue.getTranslationKey())).withStyle(ChatFormatting.GOLD));
            }
            if (Status.isCharging()) {
                Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.eta", GridTuning.clock(menu.getEta())));
            }
        } else if (isHovering(ToggleX, ToggleY, ToggleWidth, ToggleHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable(menu.isEnabled() ? "gui.scp_unleashed.instability_matrix.switch_on" : "gui.scp_unleashed.instability_matrix.switch_off"));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_pylon.switch_hint").withStyle(ChatFormatting.GRAY));
        }
        return Lines;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            FragmentRarity Selected = menu.getTier();
            for (FragmentRarity Tier : FragmentRarity.values()) {
                int Index = Tier.getTier() - 1;
                if ((Tier != Selected || golden()) && isHovering(ButtonX, buttonY(Index), ButtonWidth, ButtonHeight, MouseX, MouseY)) {
                    press(PylonMenu.FirstTierButton + Index, false);
                    return true;
                }
            }
            if (isHovering(ButtonX, ApocalypticTop, ButtonWidth, ButtonHeight, MouseX, MouseY)) {
                press(PylonMenu.ApocalypticButton, true);
                return true;
            }
            if (isHovering(ToggleX, ToggleY, ToggleWidth, ToggleHeight, MouseX, MouseY)) {
                press(PylonMenu.ToggleButton, true);
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if ((Pressed == PylonMenu.ToggleButton || Pressed == PylonMenu.ApocalypticButton) && minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PanelRelease.get(), 1.0f, 0.3f));
        }
        Pressed = -1;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    private void press(int Id, boolean Loud) {
        Pressed = Id;
        if (minecraft != null && minecraft.gameMode != null) {
            if (Loud) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.PanelPress.get(), 1.0f, 0.35f));
            }
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Id);
        }
    }
}
