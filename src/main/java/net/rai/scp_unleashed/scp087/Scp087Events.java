package net.rai.scp_unleashed.scp087;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp087.client.Scp087ClientHooks;
import net.rai.scp_unleashed.scp087.network.Scp087ShiftPayload;
import net.rai.scp_unleashed.scp087.network.Scp087StatePayload;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp087Events {
    public static final int DossierEvery = 200;
    private static final Set<UUID> Outbound = ConcurrentHashMap.newKeySet();

    private Scp087Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp087StatePayload.PayloadType, Scp087StatePayload.PayloadCodec, (Payload, Context) -> Scp087ClientHooks.state(Payload));
        Registrar.playToClient(Scp087ShiftPayload.PayloadType, Scp087ShiftPayload.PayloadCodec, (Payload, Context) -> Scp087ClientHooks.shift(Payload));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp087Registry.Hunter.get(), Scp087Hunter.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp087Registry.DoorItem.get());
            Event.accept(Scp087Registry.ConcreteItem.get());
            Event.accept(Scp087Registry.FloorItem.get());
            Event.accept(Scp087Registry.StairsItem.get());
            Event.accept(Scp087Registry.RailItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp087Registry.HunterEgg.get());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        Scp087Descent.tick(Server);
        if (!Outbound.isEmpty()) {
            ServerLevel Stairwell = Scp087Stairwell.level(Server);
            for (UUID Id : new ArrayList<>(Outbound)) {
                Outbound.remove(Id);
                Entity Found = Stairwell == null ? null : Stairwell.getEntity(Id);
                if (Found != null && !Found.isRemoved() && !(Found instanceof Player)) {
                    Scp087Descent.relocate(Server, Found);
                }
            }
        }
        if (Server.getTickCount() % DossierEvery == 0 && Scp087World.get(Server).inert()) {
            for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
                Scp087Dossier.refresh(User);
            }
        }
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent Event) {
        Scp087Descent.stopping();
        Outbound.clear();
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp087Descent.endVisit(User.server, User.getUUID());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        Scp087Dossier.refresh(User);
        if (Scp087Stairwell.inStairwell(User) && Scp087Descent.visitOf(User) == null && !Scp087Rules.exempt(User)) {
            Scp087Descent.leave(User, Component.translatable("message.scp_unleashed.scp087.outside").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && !Scp087Stairwell.inStairwell(User)) {
            Scp087World.get(User.server).clearReturn(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Event.getFrom() == Scp087Stairwell.Key && Event.getTo() != Scp087Stairwell.Key) {
            Scp087Descent.endVisit(User.server, User.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Dead = Event.getEntity();
        if (Dead.level().isClientSide) {
            return;
        }
        if (Dead instanceof ServerPlayer User && Scp087Stairwell.inStairwell(User)) {
            Scp087Descent.endVisit(User.server, User.getUUID());
            return;
        }
    }

    private static Scp087World.Spot lostAndFound(Entity Dead) {
        MinecraftServer Server = Dead.getServer();
        if (Dead instanceof ServerPlayer User) {
            return Scp087Descent.outside(User);
        }
        ServerPlayer Nearest = null;
        double Best = Double.MAX_VALUE;
        for (Player Candidate : Dead.level().players()) {
            if (!(Candidate instanceof ServerPlayer User)) {
                continue;
            }
            double Gap = User.distanceToSqr(Dead);
            if (Gap < Best) {
                Best = Gap;
                Nearest = User;
            }
        }
        return Nearest != null ? Scp087Descent.outside(Nearest) : Scp087Descent.entranceSpot(Server);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void drops(LivingDropsEvent Event) {
        Entity Dead = Event.getEntity();
        if (!Scp087Stairwell.inStairwell(Dead) || Dead.getServer() == null || Dead instanceof Scp087Hunter) {
            return;
        }
        Scp087World.Spot Spot = lostAndFound(Dead);
        ServerLevel Target = Dead.getServer().getLevel(Spot.Dimension());
        if (Target == null) {
            return;
        }
        for (ItemEntity Drop : Event.getDrops()) {
            ItemEntity Moved = new ItemEntity(Target, Spot.Pos().x, Spot.Pos().y + 0.3, Spot.Pos().z, Drop.getItem().copy());
            Moved.setDefaultPickUpDelay();
            Target.addFreshEntity(Moved);
        }
        Event.getDrops().clear();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void experience(LivingExperienceDropEvent Event) {
        LivingEntity Dead = Event.getEntity();
        if (!Scp087Stairwell.inStairwell(Dead) || Dead.getServer() == null || Event.getDroppedExperience() <= 0) {
            return;
        }
        if (Dead instanceof Scp087Hunter && Event.getAttackingPlayer() instanceof ServerPlayer Killer) {
            ExperienceOrb.award(Killer.serverLevel(), Killer.position(), Event.getDroppedExperience());
            Event.setCanceled(true);
            return;
        }
        Scp087World.Spot Spot = lostAndFound(Dead);
        ServerLevel Target = Dead.getServer().getLevel(Spot.Dimension());
        if (Target != null) {
            ExperienceOrb.award(Target, Spot.Pos(), Event.getDroppedExperience());
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        Level World = Event.getLevel();
        if (World.isClientSide || !Scp087Stairwell.inStairwell(World)) {
            return;
        }
        Entity Found = Event.getEntity();
        if (Found instanceof Scp087Face Face) {
            if (!Scp087Descent.isLive(Face.token())) {
                Event.setCanceled(true);
            }
            return;
        }
        if (Found instanceof Scp087Hunter Hunter) {
            if (Scp087Descent.isLive(Hunter.token())) {
                return;
            }
            Scp087Instance Home = Scp087Descent.instanceFor(Hunter);
            if (Home == null || Home.Ending || Scp087World.get(World.getServer()).inert()) {
                Event.setCanceled(true);
                return;
            }
            Hunter.bind(Home.Token);
            if (Home.Hunter == null) {
                Home.Hunter = Hunter.getUUID();
            }
            return;
        }
        if (Found instanceof Player || !(Found instanceof LivingEntity Living) || !Living.isAlive()) {
            return;
        }
        Outbound.add(Found.getUUID());
    }
}
