package net.rai.scp_unleashed.scp513;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp513.client.Scp513ClientHooks;
import net.rai.scp_unleashed.scp513.network.Scp513StatePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp513Events {
    private Scp513Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp513StatePayload.PayloadType, Scp513StatePayload.PayloadCodec, (Payload, Context) -> Scp513ClientHooks.state(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp513Registry.Bell.get());
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && User.hasData(Scp513Registry.Affliction.get())) {
            Scp513Affliction.tick(User);
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        if (Event.getEntity() instanceof LivingEntity Living && !(Living instanceof Player) && !Living.level().isClientSide()
                && Living.hasData(Scp513Registry.Affliction.get())) {
            Scp513Affliction.tick(Living);
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent Event) {
        LivingEntity Living = Event.getEntity();
        if (!Living.level().isClientSide() && Living.hasData(Scp513Registry.Affliction.get())) {
            Scp513Affliction.cure(Living);
        }
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        if (Scp513Affliction.isAfflicted(Event.target())) {
            Event.markCured(Scp513Affliction.cure(Event.target()));
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp513Affliction.sync(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp513Affliction.sync(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp513Affliction.sync(User);
        }
    }
}
