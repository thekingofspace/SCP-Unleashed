package net.rai.scp_unleashed.scp098;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp098Events {
    private Scp098Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp098Registry.Crab.get(), Scp098Crab.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp098Registry.Vial.get());
            Event.accept(Scp098Registry.CrabEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp098Registry.HiveItem.get());
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Scp098Host.isHost(User)) {
            Scp098Host.tick(User);
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
        if (Ticked instanceof LivingEntity Subject && !(Ticked instanceof Player) && Scp098Host.isHost(Subject)) {
            Scp098Host.tick(Subject);
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                visit(Rider);
            }
        }
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent Event) {
        if (!Event.getLevel().isClientSide && Event.getEntity() instanceof PathfinderMob Walker && ControlBodies.isPersonLike(Walker)) {
            Walker.goalSelector.addGoal(1, new Scp098Goals.SeekHive(Walker));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent Event) {
        if (!Event.isCanceled() && !Event.getEntity().level().isClientSide) {
            Scp098Host.cure(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent Event) {
        if (Event.isCanceled() || !(Event.getSource().getEntity() instanceof Scp098Crab Hunter) || !Scp098Crab.isPreyKind(Event.getEntity())) {
            return;
        }
        for (ItemEntity Drop : Event.getDrops()) {
            Drop.getPersistentData().putBoolean(Scp098Crab.MorselKey, true);
        }
        if (!Event.getDrops().isEmpty()) {
            Hunter.onPreyKilled();
        }
    }
}
