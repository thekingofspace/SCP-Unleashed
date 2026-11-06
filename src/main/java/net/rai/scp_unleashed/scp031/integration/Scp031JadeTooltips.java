package net.rai.scp_unleashed.scp031.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.scp031.Scp031Mimic;
import net.rai.scp_unleashed.scp031.client.Scp031Disguises;
import snownee.jade.api.Accessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IBoxElement;

public final class Scp031JadeTooltips {
    public static final String Unknown = "jade.scp_unleashed.scp031.unknown";
    public static final String PlayerName = "entity.minecraft.player";
    public static final String VanillaName = "Minecraft";

    private Scp031JadeTooltips() {
    }

    public static void collected(IBoxElement Root, Accessor<?> Found) {
        if (!(Found instanceof EntityAccessor Looked) || !(Looked.getEntity() instanceof Scp031Mimic Mimic)) {
            return;
        }
        if (AntiMemetic.isProtected(Minecraft.getInstance().player)) {
            return;
        }
        ITooltip Tooltip = Root.getTooltip();
        if (!Scp031Disguises.isNear(Mimic)) {
            Tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(Component.translatable(Unknown)));
            return;
        }
        Scp031Disguises.Disguise Look = Scp031Disguises.of(Mimic);
        Component Name = Look.Name() != null ? Look.Name() : Component.translatable(PlayerName);
        Tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(Name));
        Tooltip.replace(JadeIds.CORE_MOD_NAME, IThemeHelper.get().modName(VanillaName));
    }
}
