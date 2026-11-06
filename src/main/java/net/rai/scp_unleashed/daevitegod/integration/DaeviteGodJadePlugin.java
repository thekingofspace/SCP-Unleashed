package net.rai.scp_unleashed.daevitegod.integration;

import net.rai.scp_unleashed.daevitegod.DaeviteGod;
import snownee.jade.api.Accessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.ui.IBoxElement;

@WailaPlugin
public class DaeviteGodJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration Registration) {
        Registration.addTooltipCollectedCallback(DaeviteGodJadePlugin::collected);
    }

    private static void collected(IBoxElement Root, Accessor<?> Found) {
        if (!(Found instanceof EntityAccessor Looked) || !(Looked.getEntity() instanceof DaeviteGod)) {
            return;
        }
        ITooltip Tooltip = Root.getTooltip();
        Tooltip.remove(JadeIds.MC_ENTITY_HEALTH);
        Tooltip.remove(JadeIds.MC_ENTITY_ARMOR);
    }
}
