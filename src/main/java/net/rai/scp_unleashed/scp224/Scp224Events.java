package net.rai.scp_unleashed.scp224;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp224.client.Scp224ClientHooks;
import net.rai.scp_unleashed.scp224.network.Scp224AgePayload;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp224Events {
    public static final List<Holder<GameEvent>> BlockEvents = List.of(GameEvent.BLOCK_PLACE, GameEvent.BLOCK_DESTROY, GameEvent.BLOCK_CHANGE,
            GameEvent.BLOCK_OPEN, GameEvent.BLOCK_CLOSE, GameEvent.FLUID_PLACE, GameEvent.FLUID_PICKUP);

    private Scp224Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp224AgePayload.PayloadType, Scp224AgePayload.PayloadCodec, (Payload, Context) -> Scp224ClientHooks.age(Payload));
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp224Aging.tick(User);
            Scp224Aging.sync(User);
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        if (Event.getEntity() instanceof LivingEntity Living && !(Living instanceof Player) && !Living.level().isClientSide) {
            Scp224Aging.tick(Living);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Target = Event.getEntity();
        if (!Target.level().isClientSide && Target.hasData(Scp224Registry.Age)) {
            Scp224Aging.clear(Target);
            if (Target instanceof ServerPlayer User) {
                Scp224Aging.sync(User);
            }
        }
    }

    @SubscribeEvent
    public static void gameEvent(VanillaGameEvent Event) {
        if (Event.getLevel().isClientSide || !Scp224Clocks.any(Event.getLevel())) {
            return;
        }
        for (Holder<GameEvent> Kind : BlockEvents) {
            if (Event.getVanillaEvent().is(Kind)) {
                Scp224Clocks.changed(Event.getLevel(), BlockPos.containing(Event.getEventPosition()));
                return;
            }
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        resend(Event.getEntity());
    }

    private static void resend(Player Target) {
        if (Target instanceof ServerPlayer User) {
            Scp224Aging.resend(User);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp224Clocks.clear();
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp224Registry.ClockItem.get());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp224Commands.register(Event.getDispatcher());
    }
}
