package net.rai.scp_unleashed.warhead.integration;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.rai.scp_unleashed.warhead.client.WarheadHud;
import snownee.jade.api.Accessor;
import snownee.jade.api.callback.JadeBeforeRenderCallback;
import snownee.jade.api.ui.IBoxElement;
import snownee.jade.api.ui.TooltipRect;

public enum WarheadJadeNudge implements JadeBeforeRenderCallback {
    Instance;

    public static final int Gap = 2;

    public static boolean overlaps(Rect2i Area, int[] Plate) {
        return Area.getWidth() > 0 && Area.getHeight() > 0 && Area.getX() < Plate[2] && Area.getX() + Area.getWidth() > Plate[0]
                && Area.getY() < Plate[3] && Area.getY() + Area.getHeight() > Plate[1];
    }

    @Override
    public boolean beforeRender(IBoxElement Box, TooltipRect Rect, GuiGraphics Graphics, Accessor<?> Looked) {
        int[] Plate = WarheadHud.plateArea(Graphics.guiWidth());
        if (Plate != null && overlaps(Rect.expectedRect, Plate)) {
            Rect.rect.setY(Plate[3] + Gap);
        }
        return false;
    }
}
