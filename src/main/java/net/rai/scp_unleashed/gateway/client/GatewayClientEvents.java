package net.rai.scp_unleashed.gateway.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.ContainedGatewayBlockEntity;
import net.rai.scp_unleashed.gateway.DimensionalShardItem;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayGeometry;
import net.rai.scp_unleashed.gateway.GatewayRegistry;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GatewayClientEvents {
    public static final int MoteInterval = 3;
    public static final float MoteSize = 0.7f;

    private GatewayClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        ContainedGatewayBlockEntity.setClientEffects(GatewayClientEvents::motes);
        Event.enqueueWork(() -> {
            ItemProperties.register(GatewayRegistry.Shard.get(), GatewayRegistry.location("destination"),
                    (Stack, World, Holder, Seed) -> DimensionalShardItem.variantOf(Stack));
            GatewayDisplay.register();
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(GatewayRegistry.GatewayEntity.get(), ContainedGatewayRenderer::new);
    }

    private static void motes(ContainedGatewayBlockEntity Gate) {
        Level World = Gate.getLevel();
        GatewayDestination Destination = Gate.destination();
        if (World == null || !Gate.isOpen() || Destination == null || Gate.ticks() % MoteInterval != 0 || Minecraft.getInstance().isPaused()) {
            return;
        }
        RandomSource Random = World.getRandom();
        double Across = GatewayGeometry.Surface.minX + Random.nextDouble() * (GatewayGeometry.Surface.maxX - GatewayGeometry.Surface.minX);
        double Up = Random.nextDouble() * GatewayGeometry.Surface.maxY;
        double Side = Random.nextBoolean() ? 6.0 : 10.0;
        Vec3 Spot = GatewayGeometry.toWorld(Gate.origin(), Gate.front(), new Vec3(Across, Up, Side));
        Vec3 Out = GatewayGeometry.toWorld(Gate.origin(), Gate.front(), new Vec3(Across, Up, Side < 8.0 ? Side - 4.0 : Side + 4.0)).subtract(Spot).scale(0.12);
        float Lift = 0.85f + Random.nextFloat() * 0.3f;
        Vector3f Colour = new Vector3f(Math.min(1.0f, Destination.red() * Lift), Math.min(1.0f, Destination.green() * Lift), Math.min(1.0f, Destination.blue() * Lift));
        World.addParticle(new DustParticleOptions(Colour, MoteSize), Spot.x, Spot.y, Spot.z, Out.x, Out.y + 0.01, Out.z);
    }
}
