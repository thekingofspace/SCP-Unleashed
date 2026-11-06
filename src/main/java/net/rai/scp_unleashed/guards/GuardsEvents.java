package net.rai.scp_unleashed.guards;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class GuardsEvents {
    private GuardsEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(PatrolConfigPayload.PayloadType, PatrolConfigPayload.PayloadCodec, PatrolConfigPayload::handle);
    }

    @SubscribeEvent
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(GuardsRegistry.PatrolNodeItem.get());
            Event.accept(GuardsRegistry.PatrolManagerItem.get());
            Event.accept(GuardsRegistry.PatrolLinker.get());
            Event.accept(GuardsRegistry.PatrolOrders.get());
        }
    }

    @SubscribeEvent
    public static void protectFriends(LivingChangeTargetEvent Event) {
        LivingEntity Wanted = Event.getNewAboutToBeSetTarget();
        if (Wanted == null || !(Event.getEntity() instanceof DClass Guard) || Guard.level().isClientSide) {
            return;
        }
        PatrolRoute Route = GuardDuty.route(Guard);
        if (Route != null && GuardThreats.isProtected(Route, Route.Rules().toTurret(), Wanted)) {
            Event.setCanceled(true);
        }
    }
}
