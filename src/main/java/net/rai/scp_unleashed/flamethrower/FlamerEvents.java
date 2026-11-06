package net.rai.scp_unleashed.flamethrower;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class FlamerEvents {
    private FlamerEvents() {
    }

    public static boolean flame(DamageSource Source) {
        return Source.is(FlamerRegistry.StreamDamage) || Source.is(FlamerRegistry.BurnDamage);
    }

    @SubscribeEvent
    public static void invulnerability(EntityInvulnerabilityCheckEvent Event) {
        if (!Event.isInvulnerable() || !flame(Event.getSource())) {
            return;
        }
        Entity Target = Event.getEntity();
        if (Target.isInvulnerable() || Target.isRemoved() || !Target.fireImmune() || !FlamerBurns.vulnerable(Target)) {
            return;
        }
        Event.setInvulnerable(false);
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            long Now = World.getGameTime();
            FlamerBurns.tick(World, Now);
            FlamerWorld.tick(World, Now);
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            FlamerBurns.forget(World);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        FlamerStream.forget(Event.getEntity().getUUID());
        FlamethrowerItem.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        FlamerStream.forgetAll();
        FlamerBurns.clear();
        FlamerWorld.clear();
    }
}
