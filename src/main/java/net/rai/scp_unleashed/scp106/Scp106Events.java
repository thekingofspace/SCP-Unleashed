package net.rai.scp_unleashed.scp106;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp106Events {
    private static final Set<UUID> Outbound = ConcurrentHashMap.newKeySet();

    private Scp106Events() {
    }

    @SubscribeEvent
    public static void freeSeated(PlayerInteractEvent.EntityInteract Event) {
        if (!(Event.getTarget().getVehicle() instanceof Scp106Restraint Seat) || Event.getEntity() == Event.getTarget()) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.SUCCESS);
        Level World = Event.getLevel();
        if (World.isClientSide) {
            return;
        }
        BlockPos Pos = Seat.blockPosition();
        Scp106FemurBreakerBlockEntity Machine = Scp106FemurBreakerBlock.masterEntity(World, Pos, World.getBlockState(Pos));
        if (Machine != null && Machine.release(Event.getEntity())) {
            Event.getEntity().displayClientMessage(Component.translatable("message.scp_unleashed.scp106.unbuckled").withStyle(ChatFormatting.GRAY), true);
        }
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp106Registry.OldMan.get(), Scp106OldMan.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp106Registry.FemurBreakerItem.get());
            Event.accept(Scp106Registry.PocketStoneItem.get());
            Event.accept(Scp106Registry.CorrodedStoneItem.get());
            Event.accept(Scp106Registry.PocketTilesItem.get());
            Event.accept(Scp106Registry.PocketSeepItem.get());
            Event.accept(Scp106Registry.DoorwayItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp106Registry.OldManEgg.get());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        Scp106ReturnLedger.get(Server).tick(Server);
        Scp106Pocket.flushTakes(Server);
        Scp106Doorways.flush(Server);
        Scp106Pocket.tickCaptives(Server);
        if (Outbound.isEmpty()) {
            return;
        }
        ServerLevel Pocket = Scp106Pocket.level(Server);
        for (UUID Id : new ArrayList<>(Outbound)) {
            Outbound.remove(Id);
            Entity Found = Pocket == null ? null : Pocket.getEntity(Id);
            if (Found instanceof LivingEntity Living && Living.isAlive() && !(Found instanceof Player) && !Scp106Pocket.isCaptive(Server, Id)) {
                Scp106Pocket.takeCaptive(Living);
            }
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        Level World = Event.getLevel();
        if (!World.isClientSide && World.getServer() != null && Event.getEntity() instanceof Scp106OldMan Old
                && Scp106ReturnLedger.get(World.getServer()).consumeSuperseded(Old.getUUID())) {
            Event.setCanceled(true);
            return;
        }
        if (World.isClientSide || !Scp106Pocket.inPocket(World) || World.getServer() == null) {
            return;
        }
        Entity Found = Event.getEntity();
        if (Found instanceof Player || Found instanceof Scp106OldMan || !(Found instanceof LivingEntity Living) || !Living.isAlive()) {
            return;
        }
        if (Scp106Pocket.isCaptive(World.getServer(), Found.getUUID()) || Scp106PocketLayout.roomAt(Found.getX()) == Scp106PocketLayout.Warren) {
            return;
        }
        Outbound.add(Found.getUUID());
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || !Scp106Pocket.inPocket(User)) {
            return;
        }
        if (Scp106PocketWorld.get(User.server).visitOf(User.getUUID()) == null && !Scp106PocketRules.exempt(User)) {
            Scp106Pocket.leave(User, false);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.outside").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Event.getFrom() == Scp106Pocket.Key && Event.getTo() != Scp106Pocket.Key) {
            Scp106PocketWorld.get(User.server).endVisit(User.getUUID());
            Scp106Doorways.forget(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && !Scp106Pocket.inPocket(User)) {
            Scp106PocketWorld.get(User.server).endVisit(User.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        if (Event.isCanceled()) {
            return;
        }
        LivingEntity Dead = Event.getEntity();
        if (Dead instanceof ServerPlayer User && Scp106Pocket.inPocket(User)) {
            Scp106PocketWorld.get(User.server).endVisit(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        Outbound.clear();
        Scp106Doorways.clear();
        Scp106Pocket.clearQueue();
    }
}
