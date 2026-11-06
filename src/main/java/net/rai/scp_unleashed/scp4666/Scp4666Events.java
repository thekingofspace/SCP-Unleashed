package net.rai.scp_unleashed.scp4666;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp4666.client.Scp4666ClientHooks;
import net.rai.scp_unleashed.scp4666.network.Scp4666DiaryPayload;
import net.rai.scp_unleashed.scp4666.network.Scp4666PosePayload;
import net.rai.scp_unleashed.scp4666.network.Scp4666FxPayload;
import net.rai.scp_unleashed.scp4666.network.Scp4666SyncPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp4666Events {
    public static final String EpochKey = "scp_unleashed_scp4666_epoch";

    private Scp4666Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp4666SyncPayload.PayloadType, Scp4666SyncPayload.PayloadCodec, (Payload, Context) -> Scp4666ClientHooks.sync(Payload));
        Registrar.playToClient(Scp4666FxPayload.PayloadType, Scp4666FxPayload.PayloadCodec, (Payload, Context) -> Scp4666ClientHooks.effect(Payload));
        Registrar.playToClient(Scp4666DiaryPayload.PayloadType, Scp4666DiaryPayload.PayloadCodec, (Payload, Context) -> Scp4666ClientHooks.diary(Payload));
        Registrar.playToClient(Scp4666PosePayload.PayloadType, Scp4666PosePayload.PayloadCodec, (Payload, Context) -> Scp4666ClientHooks.pose(Payload));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp4666Registry.YuleMan.get(), Scp4666YuleMan.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp4666Registry.Doll.get());
            Event.accept(Scp4666Registry.Necklace.get());
            Event.accept(Scp4666Registry.Cane.get());
            Event.accept(Scp4666Registry.Firewood.get());
            Event.accept(Scp4666Registry.Scuttle.get());
            Event.accept(Scp4666Registry.Boards.get());
            Event.accept(Scp4666Registry.Gift.get());
            Event.accept(Scp4666Registry.Oats.get());
            Event.accept(Scp4666Registry.WinterHat.get());
            Event.accept(Scp4666Registry.WinterCoat.get());
            Event.accept(Scp4666Registry.WinterBoots.get());
            Event.accept(Scp4666Registry.SaintNote.get());
            Event.accept(Scp4666Registry.Egg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp4666Registry.FrozenGatewayItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Foods.getKey()) {
            Event.accept(Scp4666Registry.Cocoa.get());
            Event.accept(Scp4666Registry.OatBread.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Decoration.getKey()) {
            for (DeferredItem<? extends Item> Entry : Scp4666Registry.Furniture) {
                Event.accept(Entry.get());
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        Scp4666Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Pre Event) {
        Scp4666Director.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void loaded(LevelEvent.Load Event) {
        if (Event.getLevel() instanceof ServerLevel World && Scp4666World.inside(World)) {
            Scp4666Clock.bind(World);
            Scp4666Clock.set(Scp4666State.get(World.getServer()).clock());
        }
    }

    @SubscribeEvent
    public static void unloaded(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World && Scp4666World.inside(World)) {
            Scp4666Clock.bind(null);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && !User.isDeadOrDying()) {
            Scp4666Director.loggedIn(User);
        }
    }

    @SubscribeEvent
    public static void sleeping(CanPlayerSleepEvent Event) {
        ServerPlayer User = Event.getEntity();
        if (!Scp4666World.inside(User) || !Scp4666World.participant(User)) {
            return;
        }
        Scp4666State Shared = Scp4666State.get(User.server);
        if (!Scp4666World.exempt(User) && Scp4666Cold.chill(User) >= Scp4666Cold.ChillSleep) {
            Event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.too_cold").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return;
        }
        if (Shared.isCold(User.getUUID())) {
            Event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.cold_no_bed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return;
        }
        int Phase = Shared.phase();
        if (Phase == Scp4666State.PhaseMidnight || Phase == Scp4666State.PhaseWoken) {
            if (Event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_NOW || Event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_HERE) {
                Event.setProblem(null);
            }
            return;
        }
        Event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666." + (Phase == Scp4666State.PhaseDay ? "not_tired" : "no_sleep"))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void joining(EntityJoinLevelEvent Event) {
        if (Event.getLevel().isClientSide() || !Scp4666World.inside(Event.getLevel()) || Event.getEntity() instanceof Player || Event.getLevel().getServer() == null) {
            return;
        }
        Entity Joined = Event.getEntity();
        if (Scp4666World.standingIn() && !Scp4666World.nearSite(Event.getLevel().getServer(), Joined.blockPosition())) {
            return;
        }
        int Epoch = Scp4666State.get(Event.getLevel().getServer()).epoch();
        if (!Joined.getPersistentData().contains(EpochKey)) {
            Joined.getPersistentData().putInt(EpochKey, Epoch);
        } else if (Joined.getPersistentData().getInt(EpochKey) != Epoch) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load Event) {
        if (Event.getLevel() instanceof ServerLevel World && Scp4666World.inside(World) && Scp4666Builder.covered(Scp4666Map.get(World.getServer()),
                Event.getChunk().getPos().getMiddleBlockX(), Event.getChunk().getPos().getMiddleBlockZ())) {
            Scp4666Builder.queueThaw(World.getServer(), Event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void reload(AddReloadListenerEvent Event) {
        Scp4666Map.forget();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp4666Map.forget();
        Scp4666Cues.clear(Event.getServer());
        Scp4666Endings.clear(Event.getServer());
        Scp4666Builder.forget(Event.getServer());
        Scp4666Chores.clear(Event.getServer());
        Scp4666Cold.clear(Event.getServer());
        Scp4666Clock.bind(null);
    }
}
