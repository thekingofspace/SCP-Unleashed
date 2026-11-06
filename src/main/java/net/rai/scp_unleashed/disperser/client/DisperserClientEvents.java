package net.rai.scp_unleashed.disperser.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.disperser.DisperserBlockEntity;
import net.rai.scp_unleashed.disperser.DisperserRegistry;
import net.rai.scp_unleashed.disperser.OrganicGrenadeItem;
import net.rai.scp_unleashed.grenade.client.GrenadeClientExtensions;
import net.rai.scp_unleashed.labware.LabOrganisms;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class DisperserClientEvents {
    public static final int Untinted = 0xFFFFFFFF;
    public static final int VialLayer = 1;
    public static final int CultureLayer = 0;

    private DisperserClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(DisperserRegistry.OrganicGrenadeType.get(), OrganicGrenadeRenderer::new);
        Event.registerEntityRenderer(DisperserRegistry.Cloud.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(DisperserRegistry.Haze.get(), OrganismHazeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new GrenadeClientExtensions(), DisperserRegistry.OrganicGrenade.get());
    }

    @SubscribeEvent
    public static void registerItemColours(RegisterColorHandlersEvent.Item Event) {
        Event.register((Stack, Layer) -> Layer != CultureLayer ? Untinted : OrganicGrenadeItem.tintOf(Stack) | 0xFF000000, DisperserRegistry.OrganicGrenade.get());
    }

    @SubscribeEvent
    public static void registerBlockColours(RegisterColorHandlersEvent.Block Event) {
        Event.register((State, Getter, Pos, Layer) -> {
            if (Layer != VialLayer || Getter == null || Pos == null) {
                return Untinted;
            }
            return Getter.getBlockEntity(Pos) instanceof DisperserBlockEntity Machine ? Machine.getTint() | 0xFF000000 : LabOrganisms.UnknownColour | 0xFF000000;
        }, DisperserRegistry.Disperser.get());
    }
}
