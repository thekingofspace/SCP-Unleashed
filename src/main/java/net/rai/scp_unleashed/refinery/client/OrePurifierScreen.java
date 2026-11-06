package net.rai.scp_unleashed.refinery.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.RecipeHint;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.client.FissionGuiFluids;
import net.rai.scp_unleashed.refinery.OrePurifierBlockEntity;
import net.rai.scp_unleashed.refinery.OrePurifierMenu;
import net.rai.scp_unleashed.refinery.PurifierStatus;
import net.rai.scp_unleashed.refinery.PurifyingRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OrePurifierScreen extends AbstractContainerScreen<OrePurifierMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/ore_purifier.png");
    public static final ResourceLocation DrumTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/ore_purifier_drum.png");
    public static final int PanelWidth = 176;
    public static final int PanelHeight = 166;
    public static final int WaterX = 8;
    public static final int WasteX = 139;
    public static final int TankY = 17;
    public static final int TankWidth = 10;
    public static final int TankHeight = 54;
    public static final int BarX = 152;
    public static final int BarY = 17;
    public static final int BarWidth = 16;
    public static final int BarHeight = 54;
    public static final int BarU = 176;
    public static final int BarV = 0;
    public static final int ArrowX = 79;
    public static final int ArrowY = 36;
    public static final int ArrowWidth = 17;
    public static final int ArrowHeight = 15;
    public static final int ArrowU = 192;
    public static final int ArrowV = 0;
    public static final int LampX = 57;
    public static final int LampY = 62;
    public static final int LampSize = 6;
    public static final int LampU = 192;
    public static final int LampV = 16;
    public static final int DrumX = 48;
    public static final int DrumY = 31;
    public static final int DrumSize = 24;
    public static final int DrumColumns = 8;
    public static final int DrumSheetWidth = 192;
    public static final int DrumSheetHeight = 72;
    public static final int DrumFrames = 16;
    public static final int IdleFrame = 16;
    public static final int ChamberX = 44;
    public static final int ChamberY = 27;
    public static final int ChamberSize = 32;

    private enum HoverSpot {
        WaterTank(WaterX - 1, TankY - 1, TankWidth + 2, TankHeight + 2),
        WasteTank(WasteX - 1, TankY - 1, TankWidth + 2, TankHeight + 2),
        PowerBar(BarX - 1, BarY - 1, BarWidth + 2, BarHeight + 2),
        Chamber(ChamberX, ChamberY, ChamberSize, ChamberSize),
        Arrow(ArrowX, ArrowY, ArrowWidth, ArrowHeight),
        StatusLamp(LampX - 1, LampY - 1, LampSize + 2, LampSize + 2);

        private final int X;
        private final int Y;
        private final int Width;
        private final int Height;

        HoverSpot(int X, int Y, int Width, int Height) {
            this.X = X;
            this.Y = Y;
            this.Width = Width;
            this.Height = Height;
        }

        private boolean contains(double GuiX, double GuiY) {
            return GuiX >= X && GuiX < X + Width && GuiY >= Y && GuiY < Y + Height;
        }
    }

    public static final float SpinUpRate = 0.08f;
    public static final float SpinDownRate = 0.05f;
    public static final float FramesPerTick = 0.6f;
    public static final float IdleBelow = 0.02f;
    public static final float MillisPerTick = 50.0f;
    public static final long LongestStep = 250L;
    public static final int SettleTicks = 4;

    private float Churn;
    private double Phase;
    private long LastFrameMillis;
    private int Age;

    public OrePurifierScreen(OrePurifierMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        inventoryLabelY = 72;
        Churn = targetChurn();
        ClientLevel World = Minecraft.getInstance().level;
        Phase = World == null ? 0.0 : (World.getGameTime() * FramesPerTick * Churn) % DrumFrames;
        LastFrameMillis = Util.getMillis();
    }

    private float targetChurn() {
        if (!menu.getStatus().isWorking()) {
            return 0.0f;
        }
        return Mth.clamp(menu.getReceived() / (float) Math.max(1, menu.getDemand()), 0.0f, 1.0f);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Age++;
        float Target = targetChurn();
        if (Age <= SettleTicks) {
            Churn = Target;
            return;
        }
        Churn += (Target - Churn) * (Target > Churn ? SpinUpRate : SpinDownRate);
        if (Math.abs(Target - Churn) < 0.002f) {
            Churn = Target;
        }
    }

    private int drumFrameNow() {
        long Now = Util.getMillis();
        long Step = Mth.clamp(Now - LastFrameMillis, 0L, LongestStep);
        LastFrameMillis = Now;
        Phase = (Phase + Step / MillisPerTick * FramesPerTick * Churn) % DrumFrames;
        return Churn > IdleBelow ? Math.floorMod((int) Math.floor(Phase), DrumFrames) : IdleFrame;
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int WaterFill = Math.round((float) menu.getWater() / OrePurifierBlockEntity.TankCapacity * TankHeight);
        if (WaterFill > 0) {
            FissionGuiFluids.drawFluid(Graphics, Fluids.WATER, leftPos + WaterX, topPos + TankY + TankHeight - WaterFill, TankWidth, WaterFill);
        }
        int WasteFill = Math.round((float) menu.getWaste() / OrePurifierBlockEntity.TankCapacity * TankHeight);
        if (WasteFill > 0) {
            FissionGuiFluids.drawFluid(Graphics, FissionRegistry.WasteWater.get(), leftPos + WasteX, topPos + TankY + TankHeight - WasteFill, TankWidth, WasteFill);
        }
        drumFrame(Graphics, drumFrameNow());
        int Filled = Math.round(menu.getProgress() * ArrowWidth);
        if (Filled > 0) {
            Graphics.blit(Texture, leftPos + ArrowX, topPos + ArrowY, ArrowU, ArrowV, Filled, ArrowHeight);
        }
        int Lamp = lampIndex(menu.getStatus());
        if (Lamp >= 0) {
            Graphics.blit(Texture, leftPos + LampX, topPos + LampY, LampU + Lamp * LampSize, LampV, LampSize, LampSize);
        }
        int Power = Math.round(Mth.clamp(menu.getReceived() / (float) Math.max(1, menu.getDemand()), 0.0f, 1.0f) * BarHeight);
        if (menu.getDemand() <= 0) {
            Power = 0;
        }
        if (Power > 0) {
            Graphics.blit(Texture, leftPos + BarX, topPos + BarY + BarHeight - Power, BarU, BarV + BarHeight - Power, BarWidth, Power);
        }
    }

    private void drumFrame(GuiGraphics Graphics, int Frame) {
        Graphics.blit(DrumTexture, leftPos + DrumX, topPos + DrumY, (Frame % DrumColumns) * DrumSize, (Frame / DrumColumns) * DrumSize,
                DrumSize, DrumSize, DrumSheetWidth, DrumSheetHeight);
    }

    private int lampIndex(PurifierStatus Status) {
        long Ticks = Util.getMillis() / (long) MillisPerTick;
        return switch (Status) {
            case Working -> 0;
            case LowPower, NoWater, WasteFull, Blocked -> Ticks % 20 < 12 ? 1 : -1;
            case Unpowered -> Ticks % 20 < 10 ? 2 : -1;
            default -> -1;
        };
    }

    @Nullable
    private static HoverSpot spotAt(double GuiX, double GuiY) {
        for (HoverSpot Spot : HoverSpot.values()) {
            if (Spot.contains(GuiX, GuiY)) {
                return Spot;
            }
        }
        return null;
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
        double GuiX = MouseX - leftPos;
        double GuiY = MouseY - topPos;
        HoverSpot Spot = spotAt(GuiX, GuiY);
        if (Spot != null) {
            Graphics.renderComponentTooltip(font, tooltipFor(Spot), MouseX, MouseY);
        }
    }

    private List<Component> tooltipFor(HoverSpot Spot) {
        List<Component> Lines = new ArrayList<>();
        switch (Spot) {
            case WaterTank -> {
                Lines.add(Component.translatable("container.scp_unleashed.ore_purifier.water", menu.getWater(), OrePurifierBlockEntity.TankCapacity));
                Lines.add(Component.translatable("container.scp_unleashed.ore_purifier.water_hint", PurifyingRecipe.DefaultWater).withStyle(ChatFormatting.GRAY));
            }
            case WasteTank -> {
                Lines.add(Component.translatable("container.scp_unleashed.ore_purifier.waste", menu.getWaste(), OrePurifierBlockEntity.TankCapacity));
                Lines.add(Component.translatable("container.scp_unleashed.ore_purifier.waste_hint").withStyle(ChatFormatting.GRAY));
            }
            case PowerBar -> {
                Lines.add(Component.translatable("container.scp_unleashed.ore_purifier.power", menu.getReceived(), menu.getDemand()));
                Lines.add(Component.translatable("container.scp_unleashed.ore_purifier.draws", OrePurifierBlockEntity.WorkDraw).withStyle(ChatFormatting.GRAY));
            }
            case Chamber, Arrow, StatusLamp -> {
                PurifierStatus Status = menu.getStatus();
                Lines.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
                if (Status != PurifierStatus.Idle) {
                    Lines.add(Component.translatable("container.scp_unleashed.ore_purifier.progress", Math.round(menu.getProgress() * 100.0f)));
                }
                return RecipeHint.with(Lines, Spot == HoverSpot.Chamber || Spot == HoverSpot.Arrow);
            }
        }
        return Lines;
    }
}
