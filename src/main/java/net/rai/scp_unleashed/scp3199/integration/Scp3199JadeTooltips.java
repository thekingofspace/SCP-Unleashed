package net.rai.scp_unleashed.scp3199.integration;

import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.scp3199.Scp3199;
import snownee.jade.api.Accessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.JadeIds;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IBoxElement;

public final class Scp3199JadeTooltips {
    public static final String Title = "jade.scp_unleashed.scp3199.title";

    private Scp3199JadeTooltips() {
    }

    public static void collected(IBoxElement Root, Accessor<?> Found) {
        if (!(Found instanceof EntityAccessor Looked) || !(Looked.getEntity() instanceof Scp3199)) {
            return;
        }
        Root.getTooltip().replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(Component.translatable(Title)));
    }
}
