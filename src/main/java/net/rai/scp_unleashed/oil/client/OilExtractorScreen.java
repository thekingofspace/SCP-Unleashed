package net.rai.scp_unleashed.oil.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.fission.client.HoverArea;
import net.rai.scp_unleashed.oil.OilExtractorBlockEntity;
import net.rai.scp_unleashed.oil.OilMachineMenu;

import java.util.List;

public class OilExtractorScreen extends OilMachineScreen {
    public static final ResourceLocation Texture = guiTexture("oil_extractor");
    public static final int BayX = 8;
    public static final int BayY = 14;
    public static final int BayWidth = 138;
    public static final int BayHeight = 58;
    public static final int ChamberX = 13;
    public static final int ChamberY = 18;
    public static final int ChamberWidth = 96;
    public static final int ChamberHeight = 50;
    public static final int MeterX = 23;
    public static final int MeterY = 21;
    public static final int MeterWidth = 76;
    public static final int FrameX = 35;
    public static final int FrameY = 26;
    public static final int FrameWidth = 52;
    public static final int FrameHeight = 38;
    public static final int FrameU = 0;
    public static final int FrameV = 168;
    public static final int Frames = 8;
    public static final int FramesPerRow = 4;
    public static final int CrudeX = 125;
    public static final int ArrowX = 112;
    public static final int ArrowY = 40;
    public static final int LampX = 114;
    public static final int LampY = 61;
    public static final HoverArea RecipeArea = new HoverArea(ChamberX - 1, ChamberY - 1, ChamberWidth + 2, ChamberHeight + 2);

    private float Phase;

    public OilExtractorScreen(OilMachineMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Phase = (Phase + Heat * OilExtractorBlockEntity.PhaseStep) % 1.0f;
    }

    @Override
    protected ResourceLocation texture() {
        return Texture;
    }

    @Override
    protected List<TankSpot> tanks() {
        return List.of(TankSpot.full(CrudeX, OilExtractorBlockEntity.CrudeTank));
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
        return new int[][]{{ArrowX, ArrowY}};
    }

    @Override
    protected int[] meter() {
        return new int[]{MeterX, MeterY, MeterWidth};
    }

    @Override
    protected int drawRating() {
        return OilExtractorBlockEntity.WorkDraw;
    }

    @Override
    protected void renderChamber(GuiGraphics Graphics, float Shown, float Time) {
        int Frame = Math.floorMod((int) Math.floor(Phase * Frames), Frames);
        int U = FrameU + Frame % FramesPerRow * FrameWidth;
        int V = FrameV + Frame / FramesPerRow * FrameHeight;
        Graphics.blit(Texture, leftPos + FrameX, topPos + FrameY, U, V, FrameWidth, FrameHeight);
    }
}
