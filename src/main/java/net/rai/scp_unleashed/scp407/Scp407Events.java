package net.rai.scp_unleashed.scp407;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerSoundEvent;

import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp407Events {
    private Scp407Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp407Registry.Sprout.get(), Scp407Sprout.createAttributes().build());
        Event.put(Scp407Registry.Titan.get(), Scp407Titan.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp407Registry.Cassette.get());
            Event.accept(Scp407Registry.SproutEgg.get());
            Event.accept(Scp407Registry.TitanEgg.get());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp407Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void speakerSound(SpeakerSoundEvent Event) {
        ServerLevel World = Event.getLevel();
        SoundMessages.Request Request = Event.getRequest();
        Scp407Sessions Data = Scp407Sessions.get(World);
        if (Request != null && Request.Id().equals(Scp407Registry.SongId)) {
            Data.started(World, Event.getSpeaker(), Request);
        } else {
            Data.silenced(World, Event.getSpeaker().getBlockPos());
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Scp407Sessions Data = Scp407Sessions.get(World);
        Data.tick(World);
        Scp407Fusion.unholdStrays(World, Data);
        if (World == World.getServer().overworld() && World.getGameTime() % Scp407Doom.SweepTicks == 0) {
            Scp407Doom.get(World.getServer()).sweep(World.getServer());
        }
    }

    private static boolean shielded(LivingEntity Target) {
        if (!(Target.level() instanceof ServerLevel World)) {
            return false;
        }
        return Scp407Sessions.get(World).shielding(Scp407Hearing.keyOf(Target)) != null;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void shieldDamage(LivingIncomingDamageEvent Event) {
        if (!Event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && shielded(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void shieldDeath(LivingDeathEvent Event) {
        LivingEntity Target = Event.getEntity();
        if (!Event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && shielded(Target)) {
            Event.setCanceled(true);
            Target.setHealth(1.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void listenerDied(LivingDeathEvent Event) {
        if (Event.isCanceled() || !(Event.getEntity().level() instanceof ServerLevel World)) {
            return;
        }
        listenerGone(World, Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void listenerRecalled(EntityLeaveLevelEvent Event) {
        Entity Gone = Event.getEntity();
        if (Gone instanceof LivingEntity && !Gone.getType().canSerialize() && Gone.getRemovalReason() == Entity.RemovalReason.DISCARDED
                && Event.getLevel() instanceof ServerLevel World) {
            listenerGone(World, Gone.getUUID());
        }
    }

    private static void listenerGone(ServerLevel World, UUID Key) {
        for (ServerLevel Dimension : World.getServer().getAllLevels()) {
            for (Scp407Session Session : Scp407Sessions.get(Dimension).all()) {
                Scp407Session.Listener Entry = Session.Listeners.get(Key);
                if (Entry != null && Session.isGenesis()) {
                    Entry.Struck = true;
                    Entry.Fused = false;
                }
            }
        }
        Scp407Doom.get(World.getServer()).forget(Key);
    }
}
