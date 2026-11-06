package net.rai.scp_unleashed.scp024;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ChorusFruitItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp024Rules {
    public static final long WarnGap = 60L;
    private static final Map<UUID, Long> Warned = new ConcurrentHashMap<>();

    private Scp024Rules() {
    }

    public static boolean exempt(Player User) {
        return User.isCreative() && User.hasPermissions(2);
    }

    private static boolean bound(Entity Target) {
        return Scp024Stage.inStage(Target) && !(Target instanceof Player User && exempt(User));
    }

    public static void refuse(Player User, String Rule) {
        if (!(User instanceof ServerPlayer Contestant)) {
            return;
        }
        long Now = Contestant.serverLevel().getGameTime();
        Long Last = Warned.get(Contestant.getUUID());
        if (Last != null && Now - Last < WarnGap && Now >= Last) {
            return;
        }
        Warned.put(Contestant.getUUID(), Now);
        Contestant.displayClientMessage(Component.translatable("message.scp_unleashed.scp024.rule." + Rule).withStyle(ChatFormatting.GOLD), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        Player User = Event.getPlayer();
        if (Scp024SolidBlock.isSealed(Event.getState()) && (User == null || !User.isCreative())) {
            Event.setCanceled(true);
            return;
        }
        if (User != null && bound(User)) {
            Event.setCanceled(true);
            refuse(User, "break");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakSpeed(PlayerEvent.BreakSpeed Event) {
        if (Scp024SolidBlock.isSealed(Event.getState()) || bound(Event.getEntity())) {
            Event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void placing(BlockEvent.EntityPlaceEvent Event) {
        if (!(Event.getLevel() instanceof Level World) || !Scp024Stage.inStage(World)) {
            return;
        }
        Entity Placer = Event.getEntity();
        if (Placer instanceof Player User && exempt(User)) {
            return;
        }
        Event.setCanceled(true);
        if (Placer instanceof Player User) {
            refuse(User, "build");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void toolUse(BlockEvent.BlockToolModificationEvent Event) {
        Player User = Event.getPlayer();
        if (User != null && bound(User)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fluidMade(BlockEvent.FluidPlaceBlockEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp024Stage.inStage(World)) {
            Event.setNewState(Event.getOriginalState());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void portal(BlockEvent.PortalSpawnEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp024Stage.inStage(World)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Start Event) {
        if (Scp024Stage.inStage(Event.getLevel())) {
            Event.setCanceled(true);
        }
    }

    public static boolean forbidden(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        return Stack.getItem() instanceof BucketItem || Stack.getItem() instanceof SolidBucketItem || Stack.getItem() instanceof MobBucketItem
                || Stack.getItem() instanceof BlockItem || Stack.getItem() instanceof BoatItem || Stack.getItem() instanceof SpawnEggItem
                || Stack.getItem() instanceof EnderpearlItem || Stack.getItem() instanceof ChorusFruitItem || Stack.getItem() instanceof FireworkRocketItem
                || Stack.getItem() instanceof EnderEyeItem || Stack.getItem() instanceof WindChargeItem || Stack.getItem() instanceof FireChargeItem
                || Stack.getItem() instanceof EndCrystalItem || Stack.getItem() instanceof ArmorStandItem || Stack.getItem() instanceof HangingEntityItem
                || Stack.getItem() instanceof MinecartItem || Stack.getItem() instanceof LeadItem || Stack.getItem() instanceof FlintAndSteelItem
                || Stack.getItem() instanceof BoneMealItem || Stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
    }

    private static String reason(ItemStack Stack) {
        if (Stack.getItem() instanceof BucketItem || Stack.getItem() instanceof SolidBucketItem || Stack.getItem() instanceof MobBucketItem
                || Stack.getCapability(Capabilities.FluidHandler.ITEM) != null) {
            return "fluid";
        }
        if (Stack.getItem() instanceof EnderpearlItem || Stack.getItem() instanceof ChorusFruitItem || Stack.getItem() instanceof EnderEyeItem
                || Stack.getItem() instanceof FireworkRocketItem || Stack.getItem() instanceof WindChargeItem) {
            return "escape";
        }
        return Stack.getItem() instanceof BlockItem ? "build" : "item";
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useOnBlock(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        if (!bound(User)) {
            return;
        }
        Event.setUseItem(TriState.FALSE);
        if (User.level().isClientSide) {
            return;
        }
        BlockPos Pos = Event.getPos();
        BlockEntity Holder = User.level().getBlockEntity(Pos);
        if (Holder instanceof MenuProvider || Holder instanceof Container) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
            refuse(User, "item");
            return;
        }
        if (User instanceof ServerPlayer Contestant && Event.getHand() == InteractionHand.MAIN_HAND && Scp024Runs.use(Contestant, Pos)) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        ItemStack Held = Event.getItemStack();
        if (forbidden(Held)) {
            refuse(User, reason(Held));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useItem(PlayerInteractEvent.RightClickItem Event) {
        Player User = Event.getEntity();
        if (!bound(User) || !forbidden(Event.getItemStack())) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.FAIL);
        refuse(User, reason(Event.getItemStack()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useEntity(PlayerInteractEvent.EntityInteract Event) {
        if (bound(Event.getEntity()) && Scp024Stage.isOwned(Event.getTarget()) && !(Event.getTarget() instanceof Scp024Host)) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useEntitySpecific(PlayerInteractEvent.EntityInteractSpecific Event) {
        if (bound(Event.getEntity()) && Scp024Stage.isOwned(Event.getTarget()) && !(Event.getTarget() instanceof Scp024Host)) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pearl(EntityTeleportEvent.EnderPearl Event) {
        if (Scp024Stage.inStage(Event.getEntity())) {
            Event.setCanceled(true);
            if (Event.getEntity() instanceof Player User) {
                refuse(User, "escape");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void chorus(EntityTeleportEvent.ChorusFruit Event) {
        if (Scp024Stage.inStage(Event.getEntity())) {
            Event.setCanceled(true);
            if (Event.getEntity() instanceof Player User) {
                refuse(User, "escape");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void enderTeleport(EntityTeleportEvent.EnderEntity Event) {
        if (Scp024Stage.inStage(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void sleeping(CanPlayerSleepEvent Event) {
        if (Scp024Stage.inStage(Event.getEntity())) {
            Event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            refuse(Event.getEntity(), "sleep");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tossing(ItemTossEvent Event) {
        Player User = Event.getPlayer();
        if (!bound(User) || User.level().isClientSide || !(User instanceof ServerPlayer Contestant)) {
            return;
        }
        ItemStack Stack = Event.getEntity().getItem().copy();
        Event.setCanceled(true);
        Scp024Runs.giveBack(Contestant, Stack);
        Contestant.inventoryMenu.broadcastChanges();
        refuse(User, "toss");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void spawning(FinalizeSpawnEvent Event) {
        if (!(Event.getLevel().getLevel() instanceof Level World) || !Scp024Stage.inStage(World)) {
            return;
        }
        MobSpawnType Kind = Event.getSpawnType();
        if (Kind != MobSpawnType.COMMAND && Kind != MobSpawnType.MOB_SUMMONED) {
            Event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        Player User = Event.getEntity();
        if (User.level().isClientSide || !bound(User)) {
            return;
        }
        if (User.isFallFlying()) {
            User.stopFallFlying();
            refuse(User, "escape");
        }
    }
}
