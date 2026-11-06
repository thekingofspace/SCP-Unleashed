package net.rai.scp_unleashed.scp939.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.scp939.Scp939;
import net.rai.scp_unleashed.scp939.client.Scp939Renderer;
import snownee.jade.api.Accessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IBoxElement;

public final class Scp939JadeTooltips {
    public static final String Indistinct = "jade.scp_unleashed.scp939.indistinct";

    private Scp939JadeTooltips() {
    }

    public static void collected(IBoxElement Root, Accessor<?> Found) {
        if (!(Found instanceof EntityAccessor Looked) || !(Looked.getEntity() instanceof Scp939 Owner) || Minecraft.getInstance().player == null) {
            return;
        }
        if (Scp939Renderer.hazeShown(Owner) <= 0.0f) {
            return;
        }
        ITooltip Tooltip = Root.getTooltip();
        Tooltip.clear();
        Tooltip.add(IThemeHelper.get().title(Component.translatable(Indistinct)));
    }
}
