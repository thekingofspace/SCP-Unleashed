package net.rai.scp_unleashed.scp149;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp149Events {
    private Scp149Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp149Registry.Fly.get(), Scp149Fly.createAttributes().build());
        Event.put(Scp149Registry.Larva.get(), Scp149Larva.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp149Registry.FlyEgg.get());
            Event.accept(Scp149Registry.LarvaEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp149Registry.CocoonItem.get());
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Scp149Host.isHost(User)) {
            Scp149Host.tick(User);
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (!Ticked.level().isClientSide) {
            visit(Ticked);
        }
    }

    private static void visit(Entity Ticked) {
        if (Ticked instanceof LivingEntity Subject && !(Ticked instanceof Player) && Scp149Host.isHost(Subject)) {
            Scp149Host.tick(Subject);
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                visit(Rider);
            }
        }
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World && Event.getEntity() instanceof Scp149Fly Fly) {
            Fly.enlist(World);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent Event) {
        LivingEntity Fallen = Event.getEntity();
        if (!Event.isCanceled() && !Fallen.level().isClientSide && Scp149Host.isHost(Fallen)) {
            Scp149Host.onDeath(Fallen);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp149Escape.clear();
    }
}
