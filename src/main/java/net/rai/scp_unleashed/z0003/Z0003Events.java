package net.rai.scp_unleashed.z0003;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Z0003Events {
    public static final double HoldSlack = 0.05;

    private Z0003Events() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Z0003DeviceItem.create());
            Event.accept(Z0003VialItem.create(1));
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Z0003Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void chat(ServerChatEvent Event) {
        Z0003Voice.heard(Event.getPlayer(), Event.getRawText());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void entityTick(EntityTickEvent.Pre Event) {
        Entity Subject = Event.getEntity();
        if (!(Subject instanceof LivingEntity Living) || !(Subject.level() instanceof ServerLevel World)) {
            return;
        }
        if (Subject instanceof ServerPlayer User) {
            playerBefore(User);
            return;
        }
        if (Z0003Petrify.isStatue(Living)) {
            Z0003Petrify.hold(Living, World);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void entityTickAfter(EntityTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && User.level() instanceof ServerLevel World) {
            playerAfter(User, World);
            return;
        }
        if (Event.getEntity() instanceof Mob Holder && !(Holder instanceof DClass) && Holder.level() instanceof ServerLevel World && Holder.isAlive()) {
            mobHands(Holder, World);
        }
    }

    private static void mobHands(Mob Holder, ServerLevel World) {
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Held = Holder.getItemInHand(Hand);
            if (Z0003DeviceItem.isArmed(Held)) {
                Z0003DeviceItem.releaseFrom(World, Holder, Held);
            }
        }
    }

    private static void playerBefore(ServerPlayer User) {
        Z0003Statue Statue = Z0003Petrify.statueOf(User);
        if (Statue == null) {
            User.getData(Z0003Registry.Stride.get()).step(User);
            return;
        }
        User.setShiftKeyDown(Statue.Crouching());
        User.setSprinting(false);
    }

    private static void playerAfter(ServerPlayer User, ServerLevel World) {
        Z0003Statue Statue = Z0003Petrify.statueOf(User);
        if (Statue == null) {
            return;
        }
        Z0003Petrify.tick(User, World);
        if (!Z0003Petrify.isStatue(User)) {
            return;
        }
        double Dx = User.getX() - Statue.X();
        double Dz = User.getZ() - Statue.Z();
        boolean Drifted = Dx * Dx + Dz * Dz > HoldSlack * HoldSlack || User.getY() > Statue.Y() + HoldSlack;
        if (Drifted && !User.isPassenger()) {
            User.connection.teleport(Statue.X(), Math.min(User.getY(), Statue.Y()), Statue.Z(), User.getYRot(), User.getXRot());
        }
        if (User.getY() < Statue.Y() - HoldSlack) {
            User.setData(Z0003Registry.Statue.get(), Statue.at(Statue.X(), User.getY(), Statue.Z()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void incomingDamage(LivingIncomingDamageEvent Event) {
        if (Z0003Petrify.isStatue(Event.getEntity()) && !Event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void knockBack(LivingKnockBackEvent Event) {
        if (Z0003Petrify.isStatue(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent Event) {
        if (Z0003Petrify.isStatue(Event.getNewAboutToBeSetTarget()) || Z0003Petrify.isStatue(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interactEntity(PlayerInteractEvent.EntityInteract Event) {
        Player User = Event.getEntity();
        if (Z0003Petrify.isStatue(User)) {
            Event.setCancellationResult(InteractionResult.FAIL);
            Event.setCanceled(true);
            return;
        }
        if (!(Event.getTarget() instanceof LivingEntity Target) || !Z0003Petrify.isStatue(Target)) {
            return;
        }
        InteractionResult Result = Z0003VialItem.pour(User, Event.getHand(), Target);
        if (Result == InteractionResult.PASS && Target instanceof Player Holder) {
            Result = Z0003VialItem.takeFrom(User, Event.getHand(), Holder);
        }
        Event.setCancellationResult(Result == InteractionResult.PASS ? InteractionResult.FAIL : Result);
        Event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific Event) {
        if (Z0003Petrify.isStatue(Event.getEntity()) || Z0003Petrify.isStatue(Event.getTarget())) {
            Event.setCancellationResult(InteractionResult.FAIL);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClickItem(PlayerInteractEvent.RightClickItem Event) {
        Player User = Event.getEntity();
        if (Z0003Petrify.isStatue(User)) {
            Event.setCancellationResult(InteractionResult.FAIL);
            Event.setCanceled(true);
            return;
        }
        if (yank(User, Event.getItemStack()) || cut(User, Event.getItemStack(), Event.getHand())) {
            Event.setCancellationResult(InteractionResult.sidedSuccess(User.level().isClientSide));
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        if (Z0003Petrify.isStatue(User)) {
            Event.setCancellationResult(InteractionResult.FAIL);
            Event.setCanceled(true);
            return;
        }
        if (yank(User, Event.getItemStack()) || cut(User, Event.getItemStack(), Event.getHand())) {
            Event.setCancellationResult(InteractionResult.sidedSuccess(User.level().isClientSide));
            Event.setCanceled(true);
        }
    }

    private static boolean cut(Player User, ItemStack Held, InteractionHand Hand) {
        if (!Held.is(Items.SHEARS) || !User.isShiftKeyDown()) {
            return false;
        }
        if (User.level().isClientSide) {
            return Z0003Device.hasTetherNear(User);
        }
        int Cut = Z0003Device.cutAll(User);
        if (Cut > 0) {
            Held.hurtAndBreak(1, User, LivingEntity.getSlotForHand(Hand));
        }
        return Cut > 0;
    }

    @SubscribeEvent
    public static void holderDied(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Z0003Device.releaseAll(User);
        }
    }

    @SubscribeEvent
    public static void holderLeft(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Z0003Device.releaseAll(User);
        }
    }

    private static boolean yank(Player User, ItemStack Held) {
        if (!Held.is(Items.LEAD)) {
            return false;
        }
        if (User.level().isClientSide) {
            return Z0003Device.hasTetherNear(User);
        }
        return Z0003Device.yankAll(User) > 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock Event) {
        if (Z0003Petrify.isStatue(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attack(AttackEntityEvent Event) {
        if (Z0003Petrify.isStatue(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void breakBlock(BlockEvent.BreakEvent Event) {
        if (Z0003Petrify.isStatue(Event.getPlayer())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useStart(LivingEntityUseItemEvent.Start Event) {
        if (Z0003Petrify.isStatue(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useTick(LivingEntityUseItemEvent.Tick Event) {
        if (Z0003Petrify.isStatue(Event.getEntity())) {
            Event.setDuration(Event.getDuration() + 1);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useStop(LivingEntityUseItemEvent.Stop Event) {
        if (Z0003Petrify.isStatue(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void toss(ItemTossEvent Event) {
        Player User = Event.getPlayer();
        if (!Z0003Petrify.isStatue(User)) {
            return;
        }
        ItemStack Kept = Event.getEntity().getItem().copy();
        Event.setCanceled(true);
        if (!User.getInventory().add(Kept) && !Kept.isEmpty()) {
            User.spawnAtLocation(Kept);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void pickup(ItemEntityPickupEvent.Pre Event) {
        if (Z0003Petrify.isStatue(Event.getPlayer())) {
            Event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void containerOpen(PlayerContainerEvent.Open Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Z0003Petrify.isStatue(User)) {
            User.closeContainer();
        }
    }
}
