package net.rai.scp_unleashed.matrix.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.matrix.InstabilityMatrixBlock;
import net.rai.scp_unleashed.matrix.InstabilityMatrixBlockEntity;
import net.rai.scp_unleashed.matrix.MatrixField;
import net.rai.scp_unleashed.matrix.MatrixMenu;
import net.rai.scp_unleashed.matrix.MatrixStatus;
import net.rai.scp_unleashed.matrix.MatrixTuning;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.rift.RiftFormation;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MatrixScreen extends AbstractContainerScreen<MatrixMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/matrix/instability_matrix.png");
    public static final int PanelWidth = 220;
    public static final int PanelHeight = 152;
    public static final int TitleX = 10;
    public static final int TitleY = 7;
    public static final int TitleColour = 0xFFE4E2D6;
    public static final int EngraveColour = 0xFF8E97A2;
    public static final int ScopeX = 12;
    public static final int ScopeY = 26;
    public static final int ScopeSize = 64;
    public static final int ScopeCentreX = 44;
    public static final int ScopeCentreY = 58;
    public static final int ScopeRadius = 30;
    public static final float ScopeScale = 30.0f / MatrixTuning.ScopeReach;
    public static final int ChargeX = 10;
    public static final int ChargeY = 100;
    public static final int ChargeWidth = 68;
    public static final int ChargeHeight = 6;
    public static final int ChargeU = 0;
    public static final int ChargeV = 236;
    public static final int HeaderY = 26;
    public static final int HeaderLeft = 91;
    public static final int HeaderRight = 182;
    public static final int ButtonX = 88;
    public static final int ButtonTop = 36;
    public static final int ButtonStep = 14;
    public static final int ButtonWidth = 96;
    public static final int ButtonHeight = 13;
    public static final int ButtonU = 0;
    public static final int ButtonNormalV = 156;
    public static final int ButtonHoverV = 170;
    public static final int ButtonSelectedV = 184;
    public static final int LedU = 100;
    public static final int LedV = 156;
    public static final int LedSize = 7;
    public static final int LedOffsetX = 4;
    public static final int LedOffsetY = 3;
    public static final int NameOffsetX = 15;
    public static final int TextOffsetY = 3;
    public static final int PowerX = 195;
    public static final int PowerY = 25;
    public static final int PowerWidth = 14;
    public static final int PowerHeight = 80;
    public static final int PowerU = 224;
    public static final int PowerV = 0;
    public static final int LcdX = 10;
    public static final int LcdY = 116;
    public static final int LcdWidth = 158;
    public static final int LcdHeight = 22;
    public static final int LampX = 14;
    public static final int LampY = 119;
    public static final int LampSize = 6;
    public static final int LampU = 116;
    public static final int LampV = 156;
    public static final int StatusTextX = 24;
    public static final int StatusTextY = 119;
    public static final int ReadoutX = 14;
    public static final int ReadoutY = 129;
    public static final int ReadoutColour = 0xFF8CFFA0;
    public static final int ReadoutDim = 0xFF3F7A4C;
    public static final int ToggleX = 176;
    public static final int ToggleY = 114;
    public static final int ToggleWidth = 36;
    public static final int ToggleHeight = 26;
    public static final int ToggleV = 200;
    public static final int ToggleStep = 38;

    private int Ticks;
    private int Pressed = -1;
    private float Sweep;
    private float SweepOld;

    public MatrixScreen(MatrixMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        SweepOld = Sweep;
        if (menu.getStatus().isRunning()) {
            Sweep += 5.0f + 7.0f * menu.getResonance();
            if (Sweep >= 360.0f) {
                Sweep -= 360.0f;
                SweepOld -= 360.0f;
            }
        }
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
        float Wave = 0.5f + 0.5f * Mth.sin(Time * (0.11f + 0.16f * menu.getResonance()));
        return 0.7f + 0.3f * Wave;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        FragmentRarity Selected = menu.getTier();
        MatrixStatus Status = menu.getStatus();
        float[] Tint = rgb(Selected.getColour());
        RenderSystem.enableBlend();
        scope(Graphics, PartialTick, Tint, Status);
        charge(Graphics, Tint, Status, PartialTick);
        for (FragmentRarity Tier : FragmentRarity.values()) {
            int Index = Tier.getTier() - 1;
            int Y = topPos + buttonY(Index);
            boolean Chosen = Tier == Selected;
            boolean Hover = !Chosen && isHovering(ButtonX, buttonY(Index), ButtonWidth, ButtonHeight, MouseX, MouseY);
            int V = Chosen || Pressed == Index ? ButtonSelectedV : Hover ? ButtonHoverV : ButtonNormalV;
            Graphics.blit(Texture, leftPos + ButtonX, Y, ButtonU, V, ButtonWidth, ButtonHeight);
            float[] Colour = rgb(Tier.getColour());
            float Shine = Chosen ? (Status.isRunning() ? beat(PartialTick) : 0.85f) : 0.42f;
            Graphics.setColor(Colour[0] * Shine, Colour[1] * Shine, Colour[2] * Shine, 1.0f);
            Graphics.blit(Texture, leftPos + ButtonX + LedOffsetX, Y + LedOffsetY + (Chosen ? 1 : 0), Chosen ? LedU : LedU + LedSize + 1, LedV, LedSize, LedSize);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        power(Graphics);
        int Lamp = lampIndex(Status);
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * (LampSize + 1), LampV, LampSize, LampSize);
        }
        boolean On = menu.isEnabled();
        boolean OverToggle = isHovering(ToggleX, ToggleY, ToggleWidth, ToggleHeight, MouseX, MouseY);
        int Frame = (On ? 2 : 0) + (OverToggle || Pressed == MatrixMenu.ToggleButton ? 1 : 0);
        Graphics.blit(Texture, leftPos + ToggleX, topPos + ToggleY, Frame * ToggleStep, ToggleV, ToggleWidth, ToggleHeight);
        RenderSystem.disableBlend();
    }

    private void power(GuiGraphics Graphics) {
        int Draw = MatrixTuning.drawFor(menu.getTier());
        int Received = menu.getDemand() >= Draw ? menu.getReceived() : 0;
        float Share = Draw <= 0 ? 0.0f : Mth.clamp(Received / (float) Draw, 0.0f, 1.0f);
        int Filled = Math.round(Share * PowerHeight);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + PowerX, topPos + PowerY + PowerHeight - Filled, PowerU, PowerV + PowerHeight - Filled, PowerWidth, Filled);
        }
    }

    private void charge(GuiGraphics Graphics, float[] Tint, MatrixStatus Status, float PartialTick) {
        int Filled = Math.round(menu.getResonance() * ChargeWidth);
        if (Filled <= 0) {
            return;
        }
        float Shine = Status.isRunning() ? beat(PartialTick) : 0.55f;
        Graphics.setColor(Mth.lerp(0.2f, Tint[0], 1.0f) * Shine, Mth.lerp(0.2f, Tint[1], 1.0f) * Shine, Mth.lerp(0.2f, Tint[2], 1.0f) * Shine, 1.0f);
        Graphics.blit(Texture, leftPos + ChargeX, topPos + ChargeY, ChargeU, ChargeV, Filled, ChargeHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void scope(GuiGraphics Graphics, float PartialTick, float[] Tint, MatrixStatus Status) {
        VertexConsumer Consumer = Graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f Matrix = Graphics.pose().last().pose();
        int Cx = leftPos + ScopeCentreX;
        int Cy = topPos + ScopeCentreY;
        boolean Running = Status.isRunning();
        float Strength = Running ? beat(PartialTick) : Status == MatrixStatus.Offline ? 0.3f : 0.5f;
        int Ring = argb(0.85f, Tint[0] * Strength, Tint[1] * Strength, Tint[2] * Strength);
        circle(Consumer, Matrix, Cx, Cy, Math.round(MatrixTuning.FieldRadius * ScopeScale), Ring);
        if (Running) {
            float Angle = Mth.lerp(PartialTick, SweepOld, Sweep);
            for (int Trail = 0; Trail < 4; Trail++) {
                float Alpha = 0.5f - Trail * 0.11f;
                line(Consumer, Matrix, Cx, Cy, Angle - Trail * 7.0f, ScopeRadius - 1,
                        argb(Alpha, Mth.lerp(0.4f, Tint[0], 1.0f), Mth.lerp(0.4f, Tint[1], 1.0f), Mth.lerp(0.4f, Tint[2], 1.0f)));
            }
        }
        for (int Index = 0; Index < InstabilityMatrixBlockEntity.BlipCount; Index++) {
            int Grade = menu.getBlipTier(Index);
            if (Grade <= 0) {
                continue;
            }
            int Packed = menu.getBlipOffset(Index);
            int X = Cx + Math.round(MatrixField.unpackX(Packed) * ScopeScale);
            int Y = Cy + Math.round(MatrixField.unpackZ(Packed) * ScopeScale);
            float[] Colour = rgb(FragmentRarity.byTier(Grade).getColour());
            float Blink = 0.75f + 0.25f * Mth.sin((Ticks + PartialTick) * 0.35f + Index * 1.7f);
            int Halo = argb(0.9f, Colour[0] * Blink * 0.75f, Colour[1] * Blink * 0.75f, Colour[2] * Blink * 0.75f);
            for (int OffsetY = -1; OffsetY <= 1; OffsetY++) {
                for (int OffsetX = -1; OffsetX <= 1; OffsetX++) {
                    if (OffsetX != 0 || OffsetY != 0) {
                        pixel(Consumer, Matrix, X + OffsetX, Y + OffsetY, Halo);
                    }
                }
            }
            pixel(Consumer, Matrix, X, Y, argb(1.0f, Mth.lerp(0.5f, Colour[0], 1.0f), Mth.lerp(0.5f, Colour[1], 1.0f), Mth.lerp(0.5f, Colour[2], 1.0f)));
        }
        int Centre = argb(1.0f, 0.86f, 0.9f, 0.88f);
        for (int OffsetY = -1; OffsetY <= 0; OffsetY++) {
            for (int OffsetX = -1; OffsetX <= 0; OffsetX++) {
                pixel(Consumer, Matrix, Cx + OffsetX, Cy + OffsetY, Centre);
            }
        }
        Graphics.flush();
    }

    private static void pixel(VertexConsumer Consumer, Matrix4f Matrix, int X, int Y, int Colour) {
        Consumer.addVertex(Matrix, X, Y, 0.0f).setColor(Colour);
        Consumer.addVertex(Matrix, X, Y + 1, 0.0f).setColor(Colour);
        Consumer.addVertex(Matrix, X + 1, Y + 1, 0.0f).setColor(Colour);
        Consumer.addVertex(Matrix, X + 1, Y, 0.0f).setColor(Colour);
    }

    private static void circle(VertexConsumer Consumer, Matrix4f Matrix, int Cx, int Cy, int Radius, int Colour) {
        int Span = Radius * 2 + 3;
        boolean[] Seen = new boolean[Span * Span];
        int Steps = Math.max(24, Radius * 8);
        for (int Step = 0; Step < Steps; Step++) {
            double Angle = Step * Math.PI * 2.0 / Steps;
            int OffsetX = (int) Math.round(Math.cos(Angle) * Radius);
            int OffsetY = (int) Math.round(Math.sin(Angle) * Radius);
            int Slot = (OffsetY + Radius + 1) * Span + OffsetX + Radius + 1;
            if (!Seen[Slot]) {
                Seen[Slot] = true;
                pixel(Consumer, Matrix, Cx + OffsetX, Cy + OffsetY, Colour);
            }
        }
    }

    private static void line(VertexConsumer Consumer, Matrix4f Matrix, int Cx, int Cy, float Degrees, int Length, int Colour) {
        double Angle = Math.toRadians(Degrees);
        double DirectionX = Math.sin(Angle);
        double DirectionY = -Math.cos(Angle);
        int LastX = Integer.MIN_VALUE;
        int LastY = Integer.MIN_VALUE;
        for (int Step = 2; Step <= Length; Step++) {
            int X = Cx + (int) Math.round(DirectionX * Step);
            int Y = Cy + (int) Math.round(DirectionY * Step);
            if (X == LastX && Y == LastY) {
                continue;
            }
            LastX = X;
            LastY = Y;
            pixel(Consumer, Matrix, X, Y, Colour);
        }
    }

    private int lampIndex(MatrixStatus Status) {
        return switch (Status) {
            case Resonating -> 0;
            case LowPower, Obstructed -> Ticks % 20 < 12 ? 1 : -1;
            case Unpowered -> Ticks % 20 < 10 ? 2 : -1;
            case Saturated -> 3;
            default -> -1;
        };
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, TitleX, TitleY, TitleColour, false);
        Component Left = Component.translatable("gui.scp_unleashed.instability_matrix.target");
        Component Right = Component.translatable("gui.scp_unleashed.instability_matrix.unit");
        Graphics.drawString(font, Left, HeaderLeft, HeaderY, EngraveColour, false);
        Graphics.drawString(font, Right, HeaderRight - font.width(Right), HeaderY, EngraveColour, false);
        FragmentRarity Selected = menu.getTier();
        for (FragmentRarity Tier : FragmentRarity.values()) {
            int Y = buttonY(Tier.getTier() - 1) + TextOffsetY + (Tier == Selected ? 1 : 0);
            float[] Colour = rgb(Tier.getColour());
            boolean Chosen = Tier == Selected;
            float Lift = Chosen ? 0.25f : 0.12f;
            float Dim = Chosen ? 1.0f : 0.86f;
            int NameColour = argb(1.0f, Mth.lerp(Lift, Colour[0], 1.0f) * Dim, Mth.lerp(Lift, Colour[1], 1.0f) * Dim, Mth.lerp(Lift, Colour[2], 1.0f) * Dim);
            Graphics.drawString(font, Component.translatable(InstabilityMatrixBlock.tierKey(Tier)), ButtonX + NameOffsetX, Y, NameColour, false);
            String Draw = MatrixTuning.grouped(MatrixTuning.drawFor(Tier));
            Graphics.drawString(font, Draw, ButtonX + ButtonWidth - 5 - font.width(Draw), Y, Chosen ? 0xFFF2F2EC : 0xFFA9B0B8, false);
        }
        MatrixStatus Status = menu.getStatus();
        Component StatusText = Component.translatable(Status.getTranslationKey());
        Integer StatusColour = Status.getColour().getColor();
        Graphics.drawString(font, StatusText, StatusTextX, StatusTextY, 0xFF000000 | (StatusColour == null ? 0xAAAAAA : StatusColour), false);
        Graphics.drawString(font, readout(Status), ReadoutX, ReadoutY, Status.isRunning() ? ReadoutColour : ReadoutDim, false);
    }

    private Component readout(MatrixStatus Status) {
        FragmentRarity Tier = menu.getTier();
        if (!Status.isRunning()) {
            return Component.translatable("gui.scp_unleashed.instability_matrix.readout_idle", MatrixTuning.FieldRadius);
        }
        int Odds = (int) Math.round(MatrixTuning.oddsPerMinute(Tier, menu.getFlow(), menu.getResonance()) * 100.0);
        String Eta = MatrixTuning.clock(MatrixTuning.expectedSeconds(Tier, menu.getFlow(), menu.getResonance()));
        return Component.translatable("gui.scp_unleashed.instability_matrix.readout", Odds, Eta);
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
        MatrixStatus Status = menu.getStatus();
        FragmentRarity Selected = menu.getTier();
        for (FragmentRarity Tier : FragmentRarity.values()) {
            if (!isHovering(ButtonX, buttonY(Tier.getTier() - 1), ButtonWidth, ButtonHeight, MouseX, MouseY)) {
                continue;
            }
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.tier_title", Component.translatable(InstabilityMatrixBlock.tierKey(Tier)))
                    .withStyle(Style.EMPTY.withColor(Tier.getColour() & 0xFFFFFF)));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.tier_draw", MatrixTuning.grouped(MatrixTuning.drawFor(Tier))));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.tier_time", MatrixTuning.clock(MatrixTuning.fullPowerSeconds(Tier))).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Tier == Selected ? "gui.scp_unleashed.instability_matrix.tier_selected" : "gui.scp_unleashed.instability_matrix.tier_select")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return Lines;
        }
        if (isHovering(ScopeX - 2, ScopeY - 2, ScopeSize + 4, ScopeSize + 4, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.field", MatrixTuning.FieldRadius));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.nearby", menu.getNearby(), RiftFormation.CapCount, RiftFormation.CapRadius)
                    .withStyle(menu.getNearby() >= RiftFormation.CapCount ? ChatFormatting.AQUA : ChatFormatting.GRAY));
            int Since = menu.getSecondsSinceFormed();
            Lines.add(Since < 0 ? Component.translatable("gui.scp_unleashed.instability_matrix.never").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("gui.scp_unleashed.instability_matrix.last", MatrixTuning.clock(Since), menu.getFormed()).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.scope_hint", MatrixTuning.ScopeReach).withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(ChargeX - 1, ChargeY - 1, ChargeWidth + 2, ChargeHeight + 2, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.resonance", Math.round(menu.getResonance() * 100.0f)));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.resonance_hint").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.comparator").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(PowerX - 1, PowerY - 1, PowerWidth + 2, PowerHeight + 2, MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.power", MatrixTuning.grouped(menu.getReceived()), MatrixTuning.grouped(menu.getDemand())));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.power_target", MatrixTuning.grouped(MatrixTuning.drawFor(Selected)))
                    .withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.power_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isHovering(LcdX, LcdY, LcdWidth, LcdHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
            Lines.add(Component.translatable(Status.getHintKey()).withStyle(ChatFormatting.GRAY));
            if (Status.isRunning()) {
                int Odds = (int) Math.round(MatrixTuning.oddsPerMinute(Selected, menu.getFlow(), menu.getResonance()) * 100.0);
                Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.odds", Odds));
                Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.eta",
                        MatrixTuning.clock(MatrixTuning.expectedSeconds(Selected, menu.getFlow(), menu.getResonance()))));
            }
        } else if (isHovering(ToggleX, ToggleY, ToggleWidth, ToggleHeight, MouseX, MouseY)) {
            Lines.add(Component.translatable(menu.isEnabled() ? "gui.scp_unleashed.instability_matrix.switch_on" : "gui.scp_unleashed.instability_matrix.switch_off"));
            Lines.add(Component.translatable("gui.scp_unleashed.instability_matrix.switch_hint").withStyle(ChatFormatting.GRAY));
        }
        return Lines;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            FragmentRarity Selected = menu.getTier();
            for (FragmentRarity Tier : FragmentRarity.values()) {
                int Index = Tier.getTier() - 1;
                if (Tier != Selected && isHovering(ButtonX, buttonY(Index), ButtonWidth, ButtonHeight, MouseX, MouseY)) {
                    press(MatrixMenu.FirstTierButton + Index, false);
                    return true;
                }
            }
            if (isHovering(ToggleX, ToggleY, ToggleWidth, ToggleHeight, MouseX, MouseY)) {
                press(MatrixMenu.ToggleButton, true);
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (Pressed == MatrixMenu.ToggleButton && minecraft != null) {
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
