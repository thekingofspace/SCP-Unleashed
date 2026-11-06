package net.rai.scp_unleashed.oil.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.fission.client.FissionGuiFluids;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.oil.ChemicalProcessingRecipe;
import net.rai.scp_unleashed.oil.ChemicalProcessorBlockEntity;
import net.rai.scp_unleashed.oil.OilMachineMenu;

import java.util.List;

public class ChemicalProcessorScreen extends OilMachineScreen {
    public static final ResourceLocation Texture = guiTexture("chemical_processor");
    public static final int BayX = 25;
    public static final int BayY = 15;
    public static final int BayWidth = 124;
    public static final int BayHeight = 56;
    public static final int InputX = 31;
    public static final int OutputX = 125;
    public static final int ChamberX = 60;
    public static final int ChamberY = 18;
    public static final int ChamberWidth = 52;
    public static final int ChamberHeight = 50;
    public static final int MeterX = 66;
    public static final int MeterY = 21;
    public static final int MeterWidth = 40;
    public static final int InArrowX = 49;
    public static final int OutArrowX = 114;
    public static final int ArrowY = 40;
    public static final int LampX = 50;
    public static final int LampY = 61;
    public static final int PoolTop = 48;
    public static final int CoilX = 64;
    public static final int CoilY = 59;
    public static final int CoilWidth = 44;
    public static final int CoilHeight = 6;
    public static final int CoilU = 176;
    public static final int CoilV = 0;
    public static final int CoilDarkV = 8;
    public static final int BubbleU = 176;
    public static final int BubbleV = 24;
    public static final int BubbleSize = 3;
    public static final int Bubbles = 6;
    public static final HoverArea RecipeArea = new HoverArea(ChamberX - 1, ChamberY - 1, ChamberWidth + 2, ChamberHeight + 2);

    public ChemicalProcessorScreen(OilMachineMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
    }

    @Override
    protected ResourceLocation texture() {
        return Texture;
    }

    @Override
    protected List<TankSpot> tanks() {
        return List.of(TankSpot.full(InputX, ChemicalProcessorBlockEntity.InputTank), TankSpot.full(OutputX, ChemicalProcessorBlockEntity.OutputTank));
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
        return new int[][]{{InArrowX, ArrowY}, {OutArrowX, ArrowY}};
    }

    @Override
    protected int[] meter() {
        return new int[]{MeterX, MeterY, MeterWidth};
    }

    @Override
    protected int drawRating() {
        return ChemicalProcessingRecipe.DefaultPower;
    }

    @Override
    protected void renderChamber(GuiGraphics Graphics, float Shown, float Time) {
        int Left = leftPos + ChamberX;
        int Top = topPos + ChamberY;
        Fluid Liquid = menu.getFluid(ChemicalProcessorBlockEntity.InputTank);
        if (Liquid != Fluids.EMPTY && menu.getAmount(ChemicalProcessorBlockEntity.InputTank) > 0) {
            FissionGuiFluids.drawFluid(Graphics, Liquid, Left, topPos + PoolTop, ChamberWidth, Top + ChamberHeight - (topPos + PoolTop));
        }
        Graphics.blit(Texture, leftPos + CoilX, topPos + CoilY, CoilU, CoilDarkV, CoilWidth, CoilHeight);
        if (Shown <= 0.02f) {
            return;
        }
        Graphics.enableScissor(Left, Top, Left + ChamberWidth, Top + ChamberHeight);
        RenderSystem.enableBlend();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Shown);
        Graphics.blit(Texture, leftPos + CoilX, topPos + CoilY, CoilU, CoilV, CoilWidth, CoilHeight);
        for (int Index = 0; Index < Bubbles; Index++) {
            float Speed = 0.5f + (Index * 37 % 7) * 0.07f;
            float Travel = (Time * Speed + Index * 4.7f) % (CoilY - PoolTop + 2);
            int X = Left + 4 + (Index * 17 + 5) % (ChamberWidth - 8);
            int Y = topPos + CoilY - Math.round(Travel);
            Graphics.blit(Texture, X, Y, BubbleU, BubbleV, BubbleSize, BubbleSize);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        Graphics.disableScissor();
    }
}
