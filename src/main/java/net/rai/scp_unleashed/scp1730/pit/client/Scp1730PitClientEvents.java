package net.rai.scp_unleashed.scp1730.pit.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1730.pit.Scp1730PitRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp1730PitClientEvents {
    public static final float PitScale = 4.7f;
    public static final float MassScale = 2.6f;
    public static final float WhipScale = 3.0f;
    public static final float TongueScale = 2.5f;

    private Scp1730PitClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp1730PitRegistry.PitType.get(), Context -> new Scp1730PitRenderer<>(Context, "scp1730_3", 3, PitScale, 0.0f, true, 2.5f, 2.0f, 14.0));
        Event.registerEntityRenderer(Scp1730PitRegistry.MassType.get(), Context -> new Scp1730PitRenderer<>(Context, "scp1730_3_mass", 1, MassScale, 3.2f, true, 0.0f, -3.8f, 10.0));
        Event.registerEntityRenderer(Scp1730PitRegistry.WhipType.get(), Context -> new Scp1730PitRenderer<>(Context, "scp1730_3_whip", 1, WhipScale, 0.0f, false, 0.0f, 0.0f, 10.0));
        Event.registerEntityRenderer(Scp1730PitRegistry.TongueType.get(), Context -> new Scp1730PitRenderer<>(Context, "scp1730_3_tongue", 1, TongueScale, 0.0f, true, 0.0f, 0.0f, 14.0));
    }
}
