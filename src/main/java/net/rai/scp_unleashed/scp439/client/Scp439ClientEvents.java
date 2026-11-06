package net.rai.scp_unleashed.scp439.client;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp439.Scp439Brute;
import net.rai.scp_unleashed.scp439.Scp439Infection;
import net.rai.scp_unleashed.scp439.Scp439Queen;
import net.rai.scp_unleashed.scp439.Scp439Registry;
import net.rai.scp_unleashed.scp439.Scp439Worker;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp439ClientEvents {
    public static final float ItchChance = 0.012f;

    private Scp439ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp439Registry.Queen.get(), Context -> new Scp439CreatureRenderer<Scp439Queen>(Context, "scp439_queen", Scp439Queen.RenderScale, 0.6f));
        Event.registerEntityRenderer(Scp439Registry.Worker.get(), Context -> {
            Scp439CreatureRenderer<Scp439Worker> Made = new Scp439CreatureRenderer<>(Context, "scp439_worker", Scp439Worker.RenderScale, 0.3f);
            Made.addRenderLayer(new Scp439CarryLayer(Made));
            return Made;
        });
        Event.registerEntityRenderer(Scp439Registry.Brute.get(), Context -> new Scp439CreatureRenderer<Scp439Brute>(Context, "scp439_brute", Scp439Brute.RenderScale, 0.6f));
        Event.registerEntityRenderer(Scp439Registry.Hive.get(), Scp439HiveRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp439Registry.Flake.get(), Scp439FlakeParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerReloads(RegisterClientReloadListenersEvent Event) {
        Event.registerReloadListener((ResourceManagerReloadListener) Manager -> {
            Scp439GrowthShapes.clear();
            Scp439VillagerLayer.forget();
        });
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        if (!(Event.getEntity() instanceof LivingEntity Living) || !Living.level().isClientSide || Living.isInvisible()) {
            return;
        }
        int Stage = Scp439Infection.stageOf(Living);
        if (Stage <= 0) {
            return;
        }
        RandomSource Random = Living.getRandom();
        if (Random.nextFloat() >= ItchChance * Stage) {
            return;
        }
        double Wide = Living.getBbWidth() * 0.6;
        Living.level().addParticle(Scp439Registry.Flake.get(), Living.getX() + (Random.nextDouble() - 0.5) * Wide, Living.getY() + Random.nextDouble() * Living.getBbHeight() * 0.8,
                Living.getZ() + (Random.nextDouble() - 0.5) * Wide, 0.0, 0.0, 0.0);
    }
}
