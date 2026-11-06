package net.rai.scp_unleashed.seasonal.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp173.client.Scp173Renderer;
import net.rai.scp_unleashed.seasonal.Season;
import net.rai.scp_unleashed.seasonal.Seasons;
import software.bernie.geckolib.event.GeoRenderEvent;

import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class SeasonsClient {
    private SeasonsClient() {
    }

    public static boolean active(Season Which) {
        return Which.in(Seasons.clientMask());
    }

    public static boolean halloween() {
        return active(Season.Halloween);
    }

    public static Set<Season> current() {
        return Seasons.of(Seasons.clientMask());
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Seasons.resetClient();
    }

    @SubscribeEvent
    public static void compileGeoLayers(GeoRenderEvent.Entity.CompileRenderLayers Event) {
        if (Event.getRenderer() instanceof Scp173Renderer Renderer) {
            Event.addLayer(new Scp173PumpkinLayer(Renderer));
        }
    }
}
