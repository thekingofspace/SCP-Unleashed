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
import net.rai.scp_unleashed.oil.OilMachineMenu;
import net.rai.scp_unleashed.oil.OilRefineryBlockEntity;
import net.rai.scp_unleashed.oil.OilRefiningRecipe;

import java.util.List;

public class OilRefineryScreen extends OilMachineScreen {
    public static final ResourceLocation Texture = guiTexture("oil_refinery");
    public static final int BayX = 8;
    public static final int BayY = 14;
    public static final int BayWidth = 138;
    public static final int BayHeight = 58;
    public static final int InputX = 14;
    public static final int ChamberX = 44;
    public static final int ChamberY = 18;
    public static final int ChamberWidth = 42;
    public static final int ChamberHeight = 50;
    public static final int MeterX = 48;
    public static final int MeterY = 21;
    public static final int MeterWidth = 34;
    public static final int WaterX = 126;
    public static final int InArrowX = 32;
    public static final int InArrowY = 40;
    public static final int OutArrowX = 88;
    public static final int SlotArrowY = 40;
    public static final int WaterArrowY = 24;
    public static final int LampX = 102;
    public static final int LampY = 61;
    public static final int PoolTop = 52;
    public static final int PelletU = 176;
    public static final int PelletV = 24;
    public static final int PelletWidth = 4;
    public static final int PelletHeight = 3;
    public static final int Pellets = 4;
    public static final HoverArea RecipeArea = new HoverArea(ChamberX - 1, ChamberY - 1, ChamberWidth + 2, ChamberHeight + 2);

    public OilRefineryScreen(OilMachineMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
    }

    @Override
    protected ResourceLocation texture() {
        return Texture;
    }

    @Override
    protected List<TankSpot> tanks() {
        return List.of(TankSpot.full(InputX, OilRefineryBlockEntity.InputTank), TankSpot.full(WaterX, OilRefineryBlockEntity.WaterTank));
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
        return new int[][]{{InArrowX, InArrowY}, {OutArrowX, SlotArrowY}, {OutArrowX, WaterArrowY}};
    }

    @Override
    protected int[] meter() {
        return new int[]{MeterX, MeterY, MeterWidth};
    }

    @Override
    protected int drawRating() {
        return OilRefiningRecipe.DefaultPower;
    }

    @Override
    protected void renderChamber(GuiGraphics Graphics, float Shown, float Time) {
        int Left = leftPos + ChamberX;
        int Top = topPos + ChamberY;
        Fluid Liquid = menu.getFluid(OilRefineryBlockEntity.InputTank);
        if (Liquid != Fluids.EMPTY && menu.getAmount(OilRefineryBlockEntity.InputTank) > 0) {
            FissionGuiFluids.drawFluid(Graphics, Liquid, Left, topPos + PoolTop, ChamberWidth, Top + ChamberHeight - (topPos + PoolTop));
        }
        if (Shown <= 0.02f) {
            return;
        }
        Graphics.enableScissor(Left, Top, Left + ChamberWidth, Top + ChamberHeight);
        RenderSystem.enableBlend();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Shown);
        int Fall = PoolTop - (ChamberY + 8) - PelletHeight;
        for (int Index = 0; Index < Pellets; Index++) {
            float Travel = (Time * (0.35f + Index * 0.05f) + Index * 9.0f) % Fall;
            int X = Left + 6 + Index * 9;
            int Y = topPos + PoolTop - PelletHeight - Math.round(Travel);
            Graphics.blit(Texture, X, Y, PelletU, PelletV, PelletWidth, PelletHeight);
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        Graphics.disableScissor();
    }
}
