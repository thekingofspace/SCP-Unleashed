package net.rai.scp_unleashed.oil.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.oil.CombustionGeneratorBlockEntity;
import net.rai.scp_unleashed.oil.OilMachineMenu;

import java.util.List;

public class CombustionGeneratorScreen extends OilMachineScreen {
    public static final ResourceLocation Texture = guiTexture("combustion_generator");
    public static final int BayX = 25;
    public static final int BayY = 15;
    public static final int BayWidth = 124;
    public static final int BayHeight = 56;
    public static final int FuelX = 31;
    public static final int ExhaustX = 125;
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
    public static final int PistonX = 76;
    public static final int PistonY = 28;
    public static final int PistonWidth = 20;
    public static final int PistonHeight = 12;
    public static final int PistonTravel = 6;
    public static final int PistonU = 176;
    public static final int PistonV = 24;
    public static final int FlameX = 78;
    public static final int FlameY = 50;
    public static final int FlameWidth = 16;
    public static final int FlameHeight = 10;
    public static final int FlameU = 176;
    public static final int FlameV = 40;
    public static final int FlameFrames = 3;
    public static final HoverArea RecipeArea = new HoverArea(ChamberX - 1, ChamberY - 1, ChamberWidth + 2, ChamberHeight + 2);

    public CombustionGeneratorScreen(OilMachineMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
    }

    @Override
    protected ResourceLocation texture() {
        return Texture;
    }

    @Override
    protected List<TankSpot> tanks() {
        return List.of(TankSpot.full(FuelX, CombustionGeneratorBlockEntity.FuelTank), TankSpot.full(ExhaustX, GasTank));
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
    protected void appendChamberLines(List<Component> Lines) {
        if (menu.getPowerMax() > 0) {
            Lines.add(Component.translatable("gui.scp_unleashed.combustion_generator.rating", menu.getPowerMax()).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    protected void renderChamber(GuiGraphics Graphics, float Shown, float Time) {
        float Stroke = Shown > 0.02f ? (Mth.sin(Time * 0.9f) * 0.5f + 0.5f) * Shown : 0.0f;
        int Drop = Math.round(Stroke * PistonTravel);
        Graphics.blit(Texture, leftPos + PistonX, topPos + PistonY + Drop, PistonU, PistonV, PistonWidth, PistonHeight);
        if (Shown <= 0.02f) {
            return;
        }
        int Frame = Math.floorMod((int) (Time / 3.0f), FlameFrames);
        RenderSystem.enableBlend();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Shown);
        Graphics.blit(Texture, leftPos + FlameX, topPos + FlameY, FlameU, FlameV + Frame * FlameHeight, FlameWidth, FlameHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
