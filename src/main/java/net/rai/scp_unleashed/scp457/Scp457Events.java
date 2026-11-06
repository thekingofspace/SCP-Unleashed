package net.rai.scp_unleashed.scp457;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.plumbing.EntityWashedEvent;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp457Events {
    private Scp457Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp457Registry.Burning.get(), Scp457.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp457Registry.BurningEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp457Registry.AshPileItem.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Victim = Event.getEntity();
        if (!(Victim.level() instanceof ServerLevel World) || Victim instanceof Scp457 || Victim instanceof ArmorStand) {
            return;
        }
        if (!Scp457Spread.credited(World, Victim, Event.getSource())) {
            return;
        }
        Scp457Spread.forget(Victim.getUUID());
        Scp457Spread.rise(World, Victim.position(), Victim.getYRot());
    }

    @SubscribeEvent
    public static void washed(EntityWashedEvent Event) {
        if (Event.getEntity() instanceof Scp457 Burning && Scp457Water.wet(Event.getLiquid().getFluid().defaultFluidState())) {
            Burning.douse(Event.getLevel());
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        Scp457Spread.clear();
    }
}
