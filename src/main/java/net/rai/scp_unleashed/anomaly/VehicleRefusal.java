package net.rai.scp_unleashed.anomaly;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class VehicleRefusal {
    public static final TagKey<EntityType<?>> MayRide = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "may_ride_vehicles"));

    private VehicleRefusal() {
    }

    public static boolean refuses(Entity Rider, Entity Vehicle) {
        if (Rider instanceof Player || !(Vehicle instanceof Boat || Vehicle instanceof AbstractMinecart) || Rider.getType().is(MayRide)) {
            return false;
        }
        return Scp_unleashed.ModId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(Rider.getType()).getNamespace());
    }

    @SubscribeEvent
    public static void mount(EntityMountEvent Event) {
        if (Event.isMounting() && refuses(Event.getEntityMounting(), Event.getEntityBeingMounted())) {
            Event.setCanceled(true);
        }
    }
}
