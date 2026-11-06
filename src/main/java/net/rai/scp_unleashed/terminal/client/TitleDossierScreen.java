package net.rai.scp_unleashed.terminal.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.archive.client.DossierScreen;

public class TitleDossierScreen extends DossierScreen {
    public TitleDossierScreen(ItemStack Virtual, Screen Parent) {
        super(Virtual, Parent);
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        TerminalPaint Paint = new TerminalPaint(Graphics, font, width, height, Util.getMillis());
        Paint.background();
        Paint.crt();
    }
}
