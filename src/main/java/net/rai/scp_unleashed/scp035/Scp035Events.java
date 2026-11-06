package net.rai.scp_unleashed.scp035;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClassPack;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp035.client.Scp035ClientHooks;
import net.rai.scp_unleashed.scp035.network.Scp035LurePayload;
import net.rai.scp_unleashed.scp148.Scp148Shield;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp035Events {
    public static final int WhisperCheck = 20;
    public static final int WhisperChance = 40;
    private static final List<Pending> Queue = new ArrayList<>();

    private Scp035Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp035LurePayload.PayloadType, Scp035LurePayload.PayloadCodec, (Payload, Context) -> Scp035ClientHooks.lure(Payload));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp035Registry.Host.get(), Scp035Host.createAttributes().build());
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        Scp035Carry.tick(User);
        Scp035Lure.tick(User);
        Scp035Consume.tick(User);
        if (User.tickCount % WhisperCheck == 0 && Scp035Sight.wearsMask(User) && User.isAlive() && User.getRandom().nextInt(WhisperChance) == 0 && !Scp148Shield.blocks(User)) {
            User.playNotifySound(Scp035Registry.Whisper.get(), SoundSource.HOSTILE, 0.55f, 0.85f + User.getRandom().nextFloat() * 0.2f);
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
        if (Ticked instanceof Mob Person) {
            Scp035Consume.tick(Person);
            if (Person.isAlive()) {
                Scp035Carry.tick(Person);
            }
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                visit(Rider);
            }
        }
    }

    @SubscribeEvent
    public static void healed(LivingHealEvent Event) {
        if (Scp035Consume.isConsumed(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void died(LivingDeathEvent Event) {
        Scp035Possession.died(Event.getEntity());
    }

    @SubscribeEvent
    public static void joinLevel(EntityJoinLevelEvent Event) {
        if (!(Event.getEntity() instanceof ItemEntity Dropped) || !(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        ItemStack Stack = Dropped.getItem();
        if (!Scp035MaskItem.is(Stack)) {
            return;
        }
        Event.setCanceled(true);
        Queue.add(new Pending(World.dimension(), Dropped.position(), World.random.nextFloat() * 360.0f, Stack.copyWithCount(1)));
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        if (Queue.isEmpty()) {
            return;
        }
        MinecraftServer Server = Event.getServer();
        List<Pending> Due = new ArrayList<>(Queue);
        Queue.clear();
        for (Pending Entry : Due) {
            ServerLevel World = Server.getLevel(Entry.Dimension());
            if (World != null) {
                Scp035Mask.place(World, Entry.Spot(), Entry.Yaw(), Entry.Stack(), true);
            }
        }
    }

    @SubscribeEvent
    public static void containerClosed(PlayerContainerEvent.Close Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        AbstractContainerMenu Menu = Event.getContainer();
        boolean Refused = false;
        for (Slot Held : Menu.slots) {
            if (Held.container == User.getInventory() || Held.container instanceof CraftingContainer || Held.container instanceof ResultContainer
                    || !Scp035MaskItem.is(Held.getItem()) || isWornSlot(Held)) {
                continue;
            }
            ItemStack Back = Held.getItem().copy();
            Held.set(ItemStack.EMPTY);
            if (!User.getInventory().add(Back)) {
                Scp035Mask.place(User.serverLevel(), User.position(), User.getYRot(), Back, true);
            }
            Refused = true;
        }
        if (Refused) {
            User.playNotifySound(Scp035Registry.Refuse.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            User.displayClientMessage(Component.translatable(Scp035Possession.Prefix + "refuses_container").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    private static boolean isWornSlot(Slot Held) {
        return Held.container instanceof DClassPack && DClassPack.slotFor(Held.getContainerSlot()) == EquipmentSlot.HEAD;
    }

    @SubscribeEvent
    public static void interactEntity(PlayerInteractEvent.EntityInteract Event) {
        if (blocksDisplay(Event.getEntity(), Event.getTarget(), Event.getItemStack())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void interactEntityAt(PlayerInteractEvent.EntityInteractSpecific Event) {
        if (blocksDisplay(Event.getEntity(), Event.getTarget(), Event.getItemStack())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private static boolean blocksDisplay(Player User, Entity Target, ItemStack Held) {
        return (Target instanceof ArmorStand || Target instanceof ItemFrame) && Scp035MaskItem.is(Held);
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp035Lure.forget(Event.getEntity());
        Scp035Carry.reset(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        resend(Event.getEntity());
    }

    private static void resend(Player Target) {
        if (Target instanceof ServerPlayer User) {
            Scp035Carry.reset(User);
            Scp035Lure.resend(User);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp035Lure.clear();
        Scp035Carry.clear();
        Scp035Sources.clear();
        Queue.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp035MaskItem.fresh());
        }
    }

    private record Pending(ResourceKey<Level> Dimension, Vec3 Spot, float Yaw, ItemStack Stack) {
    }
}
