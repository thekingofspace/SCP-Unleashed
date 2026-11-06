package net.rai.scp_unleashed.scp999;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp999Events {
    private Scp999Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp999Registry.Blob.get(), Scp999Blob.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp999Registry.Egg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Foods.getKey()) {
            Event.accept(Scp999Registry.Candy.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Decoration.getKey()) {
            Event.accept(Scp999Registry.BowlItem.get());
        }
    }

    @SubscribeEvent
    public static void detonate(ExplosionEvent.Detonate Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Vec3 Centre = Event.getExplosion().center();
        for (Scp999Blob Blob : World.getEntitiesOfClass(Scp999Blob.class, new AABB(Centre, Centre).inflate(Scp999Danger.ExplosionRadius))) {
            if (Blob.isAlive() && Blob.distanceToSqr(Centre) <= Scp999Danger.ExplosionRadius * Scp999Danger.ExplosionRadius) {
                Blob.frighten(World, Centre, Scp999Danger.ExplosionFear);
            }
        }
    }
}
