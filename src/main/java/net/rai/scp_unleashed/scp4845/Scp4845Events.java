package net.rai.scp_unleashed.scp4845;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp4845.item.Scp4845MenuItem;
import net.rai.scp_unleashed.scp4845.order.Scp4845Orders;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Employee;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Lines;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp4845Events {
    public static final int ChimeCheck = 5;
    private static final Map<UUID, Boolean> Inside = new ConcurrentHashMap<>();

    private Scp4845Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp4845Registry.Employee.get(), Scp4845Employee.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            for (DeferredItem<? extends Item> Entry : Scp4845Registry.Placeables) {
                Event.accept(Entry.get());
            }
        } else if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp4845Registry.Egg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Foods.getKey()) {
            for (Scp4845MenuItem Entry : Scp4845MenuItem.values()) {
                Event.accept(Scp4845Registry.food(Entry));
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp4845Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        Scp4845Orders.tick(Server);
        if (Server.getTickCount() % ChimeCheck == 0) {
            chimes(Server);
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World && Scp4845Restaurant.isMoon(World)) {
            Scp4845Staffing.tick(World);
        }
    }

    private static void chimes(MinecraftServer Server) {
        Scp4845Site Site = Scp4845Restaurant.current(Server);
        if (Site == null) {
            return;
        }
        ServerLevel World = Server.getLevel(Site.Dimension);
        if (World == null) {
            return;
        }
        for (ServerPlayer Visitor : World.players()) {
            boolean Now = Site.Building.contains(Visitor.position()) && Site.inside(World, Visitor.position());
            Boolean Before = Inside.put(Visitor.getUUID(), Now);
            if (Now && Before != null && !Before && Visitor.position().distanceTo(Vec3.atBottomCenterOf(Site.Doorway)) < 8.0) {
                BlockPos Door = Site.Entrance.getFirst();
                Scp4845Registry.playLayers(World, null, Door.getX() + 0.5, Door.getY() + 2.5, Door.getZ() + 0.5, Scp4845Registry.ChimeLayers, SoundSource.BLOCKS, 0.7f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Inside.remove(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent Event) {
        Scp4845Restaurant.forget();
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp4845Restaurant.forget();
        Scp4845Restaurant.standIn(null);
        Scp4845Staffing.forget();
        Scp4845Orders.clear();
        Scp4845Lines.forget();
        Inside.clear();
    }
}
