package net.rai.scp_unleashed.scp179.client;

import net.minecraft.client.gui.GuiGraphics;
import net.rai.scp_unleashed.scp179.SatelliteTerminalBlockEntity;

public interface SkyView {
    void render(GuiGraphics Graphics, int X, int Y, int Width, int Height, SatelliteTerminalBlockEntity Terminal, float Ticks);
}
