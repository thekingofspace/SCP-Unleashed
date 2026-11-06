package net.rai.scp_unleashed.scp323;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClassPack;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp323.client.Scp323ClientHooks;
import net.rai.scp_unleashed.scp323.network.Scp323BondPayload;
import net.rai.scp_unleashed.scp323.network.Scp323LurePayload;
import net.rai.scp_unleashed.scp323.network.Scp323WatchPayload;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp323Events {
    public static final int WhisperCheck = 20;
    public static final int WhisperChance = 50;
    public static final int SleepInterval = 20;
    private static final List<Pending> Queue = new ArrayList<>();

    private Scp323Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp323LurePayload.PayloadType, Scp323LurePayload.PayloadCodec, (Payload, Context) -> Scp323ClientHooks.lure(Payload));
        Registrar.playToClient(Scp323BondPayload.PayloadType, Scp323BondPayload.PayloadCodec, (Payload, Context) -> Scp323ClientHooks.bond(Payload));
        Registrar.playToClient(Scp323WatchPayload.PayloadType, Scp323WatchPayload.PayloadCodec, (Payload, Context) -> Scp323ClientHooks.watch(Payload));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp323Registry.Beast.get(), Scp323Beast.createAttributes().build());
    }

    @SubscribeEvent
    public static void serverTickPre(ServerTickEvent.Pre Event) {
        MinecraftServer Server = Event.getServer();
        for (ServerLevel World : Server.getAllLevels()) {
            for (ServerPlayer User : List.copyOf(World.players())) {
                Scp323Watch.tick(User);
            }
        }
        Scp323Cycle.absorbQueued(Server);
    }

    @SubscribeEvent
    public static void serverTickPost(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        for (ServerLevel World : Server.getAllLevels()) {
            for (ServerPlayer User : List.copyOf(World.players())) {
                tickPlayer(User);
            }
        }
        if (Server.getTickCount() % SleepInterval == 0) {
            Scp323Cycle.sleep(Server);
        }
        placeQueued(Server);
    }

    private static void tickPlayer(ServerPlayer User) {
        if (User.isRemoved()) {
            return;
        }
        Scp323Carry.tick(User);
        Scp323Lure.tick(User);
        Scp323Cycle.observe(User);
        int Clock = User.tickCount + User.getId();
        if (User.server.getTickCount() % Scp323Sync.ResyncInterval == 0) {
            Scp323Sync.refresh(User);
        }
        if ((User.server.getTickCount() + User.getId()) % Scp323Cycle.CheckInterval == 0) {
            if (User.isAlive()) {
                Scp323Hosts.checkHead(User);
            }
            Scp323Cycle.check(User);
            Scp323Cycle.recall(User);
        }
        if (Clock % WhisperCheck == 0 && Scp323Bond.isHorned(User) && User.isAlive() && User.getRandom().nextInt(WhisperChance) == 0 && Scp323Night.isNight(User.level())) {
            User.playNotifySound(Scp323Registry.Whisper.get(), SoundSource.HOSTILE, 0.45f, 0.8f + User.getRandom().nextFloat() * 0.15f);
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (!Ticked.level().isClientSide) {
            visit(Ticked);
        }
    }

    private static void visit(Entity Ticked) {
        if (Ticked instanceof Mob Person && !(Ticked instanceof Scp323Beast)) {
            if (Person.isAlive()) {
                Scp323Carry.tick(Person);
                Scp323Cycle.observe(Person);
                if ((Person.tickCount + Person.getId()) % Scp323Cycle.CheckInterval == 0) {
                    Scp323Hosts.checkHead(Person);
                    if (Scp323Bond.isBound(Person)) {
                        Scp323Cycle.check(Person);
                    }
                }
            }
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                if (!(Rider instanceof Player)) {
                    visit(Rider);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Victim = Event.getEntity();
        if (Victim.level().isClientSide || Victim instanceof Scp323Beast) {
            return;
        }
        Scp323Possession.carriedDeath(Victim);
        if (Event.getSource().getEntity() instanceof Scp323Beast Beast && Scp323Prey.isPerson(Victim)) {
            Beast.recordKill(Victim);
        }
        Scp323Hosts.hostDied(Victim, Event.getSource());
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Entity Joining = Event.getEntity();
        if (Joining instanceof ItemEntity Dropped) {
            ItemStack Stack = Dropped.getItem();
            if (Scp323SkullItem.is(Stack)) {
                Event.setCanceled(true);
                Queue.add(new Pending(World.dimension(), Dropped.position(), World.random.nextFloat() * 360.0f, Stack.copyWithCount(1)));
            }
            return;
        }
        if (Joining instanceof LivingEntity Living && !Event.loadedFromDisk()) {
            Scp323Cycle.joined(World, Living);
        }
    }

    private static void placeQueued(MinecraftServer Server) {
        if (Queue.isEmpty()) {
            return;
        }
        List<Pending> Due = new ArrayList<>(Queue);
        Queue.clear();
        for (Pending Entry : Due) {
            ServerLevel World = Server.getLevel(Entry.Dimension());
            if (World != null) {
                Scp323Skull.place(World, Entry.Spot(), Entry.Yaw(), Entry.Stack(), true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void incomingDamage(LivingIncomingDamageEvent Event) {
        if (Scp323Watch.isWatching(Event.getEntity()) && !Event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent Event) {
        if (Scp323Watch.isWatching(Event.getNewAboutToBeSetTarget())) {
            Event.setCanceled(true);
        }
    }

    private static boolean locked(Player User, ICancellableEvent Event) {
        if (!Scp323Watch.isWatching(User)) {
            return false;
        }
        Event.setCanceled(true);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        if (locked(Event.getEntity(), Event)) {
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rightClickItem(PlayerInteractEvent.RightClickItem Event) {
        if (locked(Event.getEntity(), Event)) {
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock Event) {
        locked(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interactEntity(PlayerInteractEvent.EntityInteract Event) {
        if (locked(Event.getEntity(), Event) || blocksDisplay(Event.getTarget(), Event.getItemStack())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interactEntityAt(PlayerInteractEvent.EntityInteractSpecific Event) {
        if (locked(Event.getEntity(), Event) || blocksDisplay(Event.getTarget(), Event.getItemStack())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private static boolean blocksDisplay(Entity Target, ItemStack Held) {
        return (Target instanceof ArmorStand || Target instanceof ItemFrame) && Scp323SkullItem.is(Held);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent Event) {
        locked(Event.getEntity(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakBlock(BlockEvent.BreakEvent Event) {
        locked(Event.getPlayer(), Event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void placeBlock(BlockEvent.EntityPlaceEvent Event) {
        if (Event.getEntity() instanceof Player User) {
            locked(User, Event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useItem(LivingEntityUseItemEvent.Start Event) {
        if (Event.getEntity() instanceof Player User) {
            locked(User, Event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void toss(ItemTossEvent Event) {
        Player User = Event.getPlayer();
        if (!Scp323Watch.isWatching(User)) {
            return;
        }
        Event.setCanceled(true);
        ItemStack Back = Event.getEntity().getItem().copy();
        if (!Back.isEmpty() && !User.getInventory().add(Back)) {
            User.getInventory().placeItemBackInInventory(Back);
        }
    }

    @SubscribeEvent
    public static void containerOpened(PlayerContainerEvent.Open Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Scp323Watch.isWatching(User)) {
            User.closeContainer();
        }
    }

    @SubscribeEvent
    public static void containerClosed(PlayerContainerEvent.Close Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || !Scp323Night.isNight(User.level())) {
            return;
        }
        AbstractContainerMenu Menu = Event.getContainer();
        boolean Refused = false;
        for (Slot Held : Menu.slots) {
            if (Held.container == User.getInventory() || Held.container instanceof CraftingContainer || Held.container instanceof ResultContainer
                    || !Scp323SkullItem.is(Held.getItem()) || isWornSlot(Held)) {
                continue;
            }
            ItemStack Back = Held.getItem().copy();
            Held.set(ItemStack.EMPTY);
            if (!User.getInventory().add(Back)) {
                Scp323Skull.place(User.serverLevel(), User.position(), User.getYRot(), Back, true);
            }
            Refused = true;
        }
        if (Refused) {
            User.playNotifySound(Scp323Registry.Refuse.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            User.displayClientMessage(Component.translatable(Scp323Possession.Prefix + "refuses_container").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    private static boolean isWornSlot(Slot Held) {
        return Held.container instanceof DClassPack && DClassPack.slotFor(Held.getContainerSlot()) == EquipmentSlot.HEAD;
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            arrive(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        Scp323Lure.forget(User);
        Scp323Carry.reset(User);
        disconnected(User);
        Scp323Sync.forget(User.getUUID());
        Scp323Watch.forget(User.getUUID());
        Scp323Cycle.forget(User.getUUID());
    }

    public static void disconnected(ServerPlayer User) {
        Scp323Bond Bond = Scp323Bond.of(User);
        if (Bond.transformed()) {
            Scp323Beast Beast = Scp323Hosts.beast(User.server, Bond.Creature());
            int Kills = Bond.Kills();
            if (Beast != null && User.getUUID().equals(Beast.hostPlayer())) {
                Kills = Beast.kills();
                if (Beast.level() instanceof ServerLevel World) {
                    Scp323Hosts.retracted(World, Beast.position());
                }
                Beast.vanish();
            }
            Scp323Bond.set(User, Bond.interrupt(Kills));
        }
        if (Scp323Watch.isWatching(User)) {
            Scp323Watch.stop(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            arrive(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp323Carry.reset(User);
            Scp323Lure.resend(User);
            Scp323Sync.resend(User);
        }
    }

    private static void arrive(ServerPlayer User) {
        Scp323Carry.reset(User);
        Scp323Lure.resend(User);
        Scp323Sync.resend(User);
        Scp323Cycle.grace(User);
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking Event) {
        if (Event.getEntity() instanceof ServerPlayer Viewer) {
            Scp323Sync.tell(Viewer, Event.getTarget());
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp323Lure.clear();
        Scp323Carry.clear();
        Scp323Sources.clear();
        Scp323Sync.clear();
        Scp323Watch.clear();
        Scp323Cycle.clear();
        Queue.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp323SkullItem.fresh());
            Event.accept(Scp323Registry.SpawnEgg.get());
        }
    }

    private record Pending(ResourceKey<Level> Dimension, Vec3 Spot, float Yaw, ItemStack Stack) {
    }
}
