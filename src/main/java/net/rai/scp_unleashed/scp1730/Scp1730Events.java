package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp008.Scp008Infection;
import net.rai.scp_unleashed.scp1730.block.Scp1730CardBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730SymbolBlock;
import net.rai.scp_unleashed.scp1730.client.Scp1730ClientHooks;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Leech;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Painter;
import net.rai.scp_unleashed.scp1730.network.Scp1730HoldPayload;
import net.rai.scp_unleashed.scp1730.network.Scp1730PulsePayload;
import net.rai.scp_unleashed.scp1730.network.Scp1730ShakePayload;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp1730Events {
    public static final String InfectedId = "scp008_zombie";
    private static final Set<UUID> Cure = ConcurrentHashMap.newKeySet();

    private Scp1730Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp1730Registry.PainterType.get(), Scp1730Painter.createAttributes().build());
        Event.put(Scp1730Registry.GuardianType.get(), Scp1730Guardian.createAttributes().build());
        Event.put(Scp1730Registry.LeechType.get(), Scp1730Leech.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp1730HoldPayload.PayloadType, Scp1730HoldPayload.PayloadCodec, (Payload, Context) -> Scp1730ClientHooks.hold(Payload));
        Registrar.playToClient(Scp1730ShakePayload.PayloadType, Scp1730ShakePayload.PayloadCodec, (Payload, Context) -> Scp1730ClientHooks.shake(Payload));
        Registrar.playToClient(Scp1730PulsePayload.PayloadType, Scp1730PulsePayload.PayloadCodec, (Payload, Context) -> Scp1730ClientHooks.pulse(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp1730Registry.Fragment.get());
            Event.accept(Scp1730Registry.AccessCard.get());
            Event.accept(Scp1730Registry.ClearanceCard.get());
            Event.accept(Scp1730Registry.Core.get());
            Event.accept(Scp1730Registry.PainterEgg.get());
            Event.accept(Scp1730Registry.GuardianEgg.get());
            Event.accept(net.rai.scp_unleashed.scp1730.guardian.Scp1730GuardianRegistry.ContainedEgg.get());
            Event.accept(Scp1730Registry.LeechEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            for (DeferredItem<?> Entry : Scp1730Registry.facilityItems()) {
                Event.accept(new ItemStack(Entry.get()));
            }
        } else if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(Scp1730Registry.FolderItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Weapons.getKey()) {
            Event.accept(Scp1730Registry.Anchor.get());
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        Scp1730Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void aboutToStart(ServerAboutToStartEvent Event) {
        Scp1730World.salt(Event.getServer().getWorldData().worldGenOptions().seed());
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        Scp1730Director.tick(Server);
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            Scp1730Symbols.tick(User);
            if (!Cure.isEmpty() && Cure.remove(User.getUUID()) && GearTags.wearsFullHazmatSuit(User)) {
                Scp008Infection.cure(User);
            }
        }
        Cure.clear();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp1730Symbols.clear();
        Scp1730Cracks.clear();
        Scp1730Anchors.clear();
        Scp1730Shockwaves.clear();
        Scp1730Director.clear();
        Cure.clear();
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || !Scp1730World.inside(User)) {
            return;
        }
        Entity Source = Event.getSource().getEntity();
        if (Source != null && BuiltInRegistries.ENTITY_TYPE.getKey(Source.getType()).equals(Scp1730Registry.location(InfectedId)) && GearTags.wearsFullHazmatSuit(User)) {
            Cure.add(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (Event.getLevel().isClientSide() || Event.loadedFromDisk() || !(Event.getEntity() instanceof Mob Creature) || !Scp1730World.inside(Event.getLevel())) {
            return;
        }
        Scp1730Breach.adopt(Creature);
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp1730Symbols.release(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp1730Symbols.freeze(User, false);
            Scp1730Symbols.forget(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp1730Symbols.release(User);
        }
    }

    public static boolean protectedBlock(ServerLevel World, BlockPos Pos, BlockState State) {
        if (!Scp1730World.inside(World) || State.getBlock() instanceof Scp1730SymbolBlock || State.getBlock() instanceof Scp1730CardBlock) {
            return false;
        }
        Scp1730State Data = Scp1730State.get(World.getServer());
        return Data.run() >= 0 && Scp1730World.original(Data.run(), Pos, State);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void breaking(BlockEvent.BreakEvent Event) {
        Player User = Event.getPlayer();
        if (User == null || User.isCreative() || !(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        if (protectedBlock(World, Event.getPos(), Event.getState())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void detonating(ExplosionEvent.Detonate Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Scp1730Defences.blast(World, Event.getExplosion().center(), Event.getExplosion().radius());
        if (Scp1730World.inside(World)) {
            Event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void griefing(EntityMobGriefingEvent Event) {
        if (Event.getEntity() != null && Scp1730World.inside(Event.getEntity())) {
            Event.setCanGrief(false);
        }
    }
}
