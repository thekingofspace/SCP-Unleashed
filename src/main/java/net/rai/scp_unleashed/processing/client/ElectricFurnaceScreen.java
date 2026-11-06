package net.rai.scp_unleashed.processing.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.processing.ElectricFurnaceMenu;
import net.rai.scp_unleashed.processing.MachineStatus;

public class ElectricFurnaceScreen extends ProcessingScreen<ElectricFurnaceMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/electric_furnace.png");
    public static final int ArrowX = 73;
    public static final int ArrowY = 25;
    public static final int ArrowWidth = 24;
    public static final int ArrowHeight = 16;
    public static final int ArrowU = 176;
    public static final int ArrowV = 0;
    public static final int CoilX = 47;
    public static final int CoilY = 50;
    public static final int CoilWidth = 80;
    public static final int CoilHeight = 12;
    public static final int CoilU = 176;
    public static final int CoilV = 16;
    public static final int FillU = 176;
    public static final int FillV = 28;
    public static final int ThermoX = 19;
    public static final int ThermoY = 21;
    public static final int ThermoWidth = 4;
    public static final int ThermoHeight = 38;
    public static final int ThermoU = 192;
    public static final int ThermoV = 28;
    public static final int LampU = 200;
    public static final int LampV = 28;
    public static final float HeatUp = 0.06f;
    public static final float CoolDown = 0.025f;

    private float Heat;
    private float PreviousHeat;

    public ElectricFurnaceScreen(ElectricFurnaceMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title, Texture, FillU, FillV, LampU, LampV);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        PreviousHeat = Heat;
        MachineStatus Status = menu.getStatus();
        boolean Heating = Status == MachineStatus.Running || Status == MachineStatus.Underpowered;
        float Target = Heating ? 0.35f + 0.65f * menu.getLoad() : 0.0f;
        Heat += (Target - Heat) * (Target > Heat ? HeatUp : CoolDown);
    }

    @Override
    protected void renderMachine(GuiGraphics Graphics, float PartialTick) {
        float Shown = PreviousHeat + (Heat - PreviousHeat) * PartialTick;
        if (Shown > 0.01f) {
            RenderSystem.enableBlend();
            Graphics.setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, Shown));
            Graphics.blit(Texture, leftPos + CoilX, topPos + CoilY, CoilU, CoilV, CoilWidth, CoilHeight);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
        int Mercury = Math.round(Math.min(1.0f, Shown) * ThermoHeight);
        if (Mercury > 0) {
            Graphics.blit(Texture, leftPos + ThermoX, topPos + ThermoY + ThermoHeight - Mercury, ThermoU, ThermoV + ThermoHeight - Mercury, ThermoWidth, Mercury);
        }
        int Width = Math.round(menu.getProgress() * ArrowWidth);
        if (Width > 0) {
            Graphics.blit(Texture, leftPos + ArrowX, topPos + ArrowY, ArrowU, ArrowV, Width, ArrowHeight);
        }
    }

    @Override
    protected boolean isOverProgress(int MouseX, int MouseY) {
        return isHovering(ArrowX, ArrowY, ArrowWidth, ArrowHeight, MouseX, MouseY);
    }

    @Override
    protected String getActionKey() {
        return "container.scp_unleashed.electric_furnace.progress";
    }
}
