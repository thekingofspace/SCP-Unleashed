package net.rai.scp_unleashed.anomaly;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class AnomalyPersistence {
    public static final TagKey<EntityType<?>> MayDespawn = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "may_despawn"));

    private AnomalyPersistence() {
    }

    public static boolean isPersistent(Mob Body) {
        EntityType<?> Type = Body.getType();
        return Scp_unleashed.ModId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(Type).getNamespace()) && !Type.is(MayDespawn);
    }

    @SubscribeEvent
    public static void keep(MobDespawnEvent Event) {
        if (isPersistent(Event.getEntity())) {
            Event.setResult(MobDespawnEvent.Result.DENY);
        }
    }
}
