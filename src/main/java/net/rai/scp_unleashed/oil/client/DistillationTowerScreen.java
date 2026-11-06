package net.rai.scp_unleashed.oil.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.oil.DistillationTowerBlockEntity;
import net.rai.scp_unleashed.oil.DistillingRecipe;
import net.rai.scp_unleashed.oil.OilMachineMenu;

import java.util.List;

public class DistillationTowerScreen extends OilMachineScreen {
    public static final ResourceLocation Texture = guiTexture("distillation_tower");
    public static final int BayX = 8;
    public static final int BayY = 14;
    public static final int BayWidth = 138;
    public static final int BayHeight = 58;
    public static final int FeedX = 14;
    public static final int ChamberX = 44;
    public static final int ChamberY = 18;
    public static final int ChamberWidth = 44;
    public static final int ChamberHeight = 50;
    public static final int MeterX = 48;
    public static final int MeterY = 21;
    public static final int MeterWidth = 36;
    public static final int OutputX = 104;
    public static final int TopY = 19;
    public static final int BottomY = 46;
    public static final int HalfHeight = 23;
    public static final int InArrowX = 32;
    public static final int InArrowY = 40;
    public static final int OutArrowX = 91;
    public static final int TopArrowY = 27;
    public static final int BottomArrowY = 54;
    public static final int LampX = 127;
    public static final int LampY = 61;
    public static final int BubbleU = 176;
    public static final int BubbleV = 24;
    public static final int BubbleSize = 3;
    public static final int Bubbles = 7;
    public static final int ColumnLeft = 52;
    public static final int ColumnWidth = 28;
    public static final int ColumnTop = 26;
    public static final int ColumnBottom = 66;
    public static final HoverArea RecipeArea = new HoverArea(ChamberX - 1, ChamberY - 1, ChamberWidth + 2, ChamberHeight + 2);

    public DistillationTowerScreen(OilMachineMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
    }

    @Override
    protected ResourceLocation texture() {
        return Texture;
    }

    @Override
    protected List<TankSpot> tanks() {
        return List.of(TankSpot.full(FeedX, DistillationTowerBlockEntity.FeedTank),
                new TankSpot(OutputX, TopY, HalfHeight, DistillationTowerBlockEntity.TopTank),
                new TankSpot(OutputX, BottomY, HalfHeight, DistillationTowerBlockEntity.BottomTank));
    }

    @Override
    protected HoverArea chamber() {
        return RecipeArea;
    }

    @Override
    protected int[] lamp() {
        return new int[]{LampX, LampY};
    }

    @Override
    protected int[][] arrows() {
        return new int[][]{{InArrowX, InArrowY}, {OutArrowX, TopArrowY}, {OutArrowX, BottomArrowY}};
    }

    @Override
    protected int[] meter() {
        return new int[]{MeterX, MeterY, MeterWidth};
    }

    @Override
    protected int drawRating() {
        return DistillingRecipe.DefaultPower;
    }

    @Override
    protected void renderChamber(GuiGraphics Graphics, float Shown, float Time) {
        if (Shown <= 0.02f) {
            return;
        }
        RenderSystem.enableBlend();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Shown);
        int Rise = ColumnBottom - ColumnTop - BubbleSize;
        for (int Index = 0; Index < Bubbles; Index++) {
            float Speed = 0.45f + (Index * 37 % 7) * 0.06f;
            float Travel = (Time * Speed + Index * 6.1f) % Rise;
            int X = leftPos + ColumnLeft + 2 + (Index * 11 + 3) % (ColumnWidth - 6);
            int Y = topPos + ColumnBottom - BubbleSize - Math.round(Travel);
            Graphics.blit(Texture, X, Y, BubbleU, BubbleV, BubbleSize, BubbleSize);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
