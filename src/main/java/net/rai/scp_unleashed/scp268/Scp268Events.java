package net.rai.scp_unleashed.scp268;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp268.integration.Scp268Curios;
import net.rai.scp_unleashed.senses.Senses;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp268Events {
    private Scp268Events() {
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent Event) {
        if (Senses.curiosLoaded()) {
            Scp268Curios.register();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp268Registry.Cap.get());
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Pre Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp268Neglect.update(User, true);
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Pre Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp268Neglect.update(User, User.isPassenger());
        } else if (Event.getEntity().level().isClientSide()) {
            Scp268.refresh(Event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void changeTarget(LivingChangeTargetEvent Event) {
        if (Scp268.isNeglected(Event.getNewAboutToBeSetTarget())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void gameEvent(VanillaGameEvent Event) {
        if (Scp268.isNeglected(Event.getCause())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent Event) {
        if (Scp268.isNeglected(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock Event) {
        if (Scp268.isNeglected(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        if (Scp268.isNeglected(Event.getEntity())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClickItem(PlayerInteractEvent.RightClickItem Event) {
        if (Scp268.isNeglected(Event.getEntity())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entityInteract(PlayerInteractEvent.EntityInteract Event) {
        if (Scp268.isNeglected(Event.getEntity())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific Event) {
        if (Scp268.isNeglected(Event.getEntity())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Joined) {
            Scp268Neglect.joined(Joined);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp268Neglect.left(User);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp268Neglect.reset();
    }
}
