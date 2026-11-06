package net.rai.scp_unleashed.scp009;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp009Events {
    private Scp009Events() {
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && User.isAlive()) {
            Scp009Doom.checkCarried(User);
            Scp009Breaches.discover(User);
        }
    }

    @SubscribeEvent
    public static void equipmentChanged(LivingEquipmentChangeEvent Event) {
        LivingEntity Bearer = Event.getEntity();
        if (!(Bearer instanceof Player) && !Bearer.level().isClientSide && Scp009CrystalItem.is(Event.getTo())) {
            Scp009Doom.checkHeld(Bearer);
        }
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load Event) {
        Scp009Sources.chunkLoaded(Event.getLevel(), Event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload Event) {
        Scp009Sources.chunkUnloaded(Event.getLevel(), Event.getChunk().getPos());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void livingDeath(LivingDeathEvent Event) {
        LivingEntity Dead = Event.getEntity();
        if (!Dead.level().isClientSide && (Scp009Doom.isDoomed(Dead) || Event.getSource().is(Scp009Registry.CrystallizedDamage))) {
            Scp009Doom.shatter(Dead);
            Scp009Statues.raise(Dead);
        }
    }

    @SubscribeEvent
    public static void effectRemoved(MobEffectEvent.Remove Event) {
        if (isDoom(Event.getEffect())) {
            Scp009Doom.clearSlow(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void effectExpired(MobEffectEvent.Expired Event) {
        MobEffectInstance Instance = Event.getEffectInstance();
        if (Instance != null && isDoom(Instance.getEffect())) {
            Scp009Doom.clearSlow(Event.getEntity());
        }
    }

    private static boolean isDoom(@Nullable Holder<MobEffect> Effect) {
        return Effect != null && Effect.value() == Scp009Registry.Doomed.get();
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp009Doom.clearSlow(User);
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            RedIceGrowth.tick(World);
            Scp009Breaches.tick(World);
            Scp009Sources.tick(World);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        RedIceGrowth.clear();
        RedIceSpread.clear();
        Scp009Breaches.clear();
        Scp009Sources.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp009Registry.Shard.get());
            Event.accept(Scp009Registry.Fragment.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp009Registry.RedIceItem.get());
        }
    }
}
