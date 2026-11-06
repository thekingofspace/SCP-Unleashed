package net.rai.scp_unleashed.upgrade;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.registry.ModItems;
import net.rai.scp_unleashed.upgrade.client.MufflerClient;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class MufflerEvents {
    private MufflerEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        Event.registrar("1").playToClient(MufflerPayload.PayloadType, MufflerPayload.PayloadCodec, (Payload, Context) -> MufflerClient.accept(Payload));
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Upgrades.getKey()) {
            Event.accept(ModItems.SoundMufflerUpgrade.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void soundAtPosition(PlayLevelSoundEvent.AtPosition Event) {
        if (Event.getLevel() instanceof ServerLevel World && Event.getSound() != null) {
            Vec3 At = Event.getPosition();
            float Factor = SoundMuffling.factor(World, At.x, At.y, At.z, Event.getSource());
            if (Factor < 1.0f) {
                Event.setNewVolume(Event.getNewVolume() * Factor);
            }
        }
    }

    @SubscribeEvent
    public static void blockChanged(BlockEvent.NeighborNotifyEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Mufflers.changed(World, Event.getPos());
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World && World.getGameTime() % Mufflers.CheckInterval == 0) {
            Mufflers.audit(World);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Mufflers.sync(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Mufflers.sync(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Mufflers.sync(User);
        }
    }
}
