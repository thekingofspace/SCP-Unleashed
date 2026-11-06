package net.rai.scp_unleashed.scp087;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp087Rules {
    public static final long WarnGap = 50L;
    private static final Map<UUID, Long> Warned = new ConcurrentHashMap<>();

    private Scp087Rules() {
    }

    public static boolean exempt(Player User) {
        return User.isCreative() && User.hasPermissions(2);
    }

    public static boolean bound(Entity Target) {
        return Scp087Stairwell.inStairwell(Target) && !(Target instanceof Player User && exempt(User));
    }

    public static void refuse(Player User, String Rule) {
        if (!(User instanceof ServerPlayer Visitor)) {
            return;
        }
        long Now = Visitor.serverLevel().getGameTime();
        Long Last = Warned.get(Visitor.getUUID());
        if (Last != null && Now >= Last && Now - Last < WarnGap) {
            return;
        }
        Warned.put(Visitor.getUUID(), Now);
        Visitor.displayClientMessage(Component.translatable("message.scp_unleashed.scp087.rule." + Rule).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
    }

    public static boolean allowed(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return true;
        }
        Item Kind = Stack.getItem();
        if (Kind instanceof SwordItem || Kind instanceof AxeItem || Kind instanceof TridentItem || Kind instanceof MaceItem || Kind instanceof ProjectileWeaponItem
                || Kind instanceof ShieldItem || Kind instanceof GunItem || Kind instanceof ArmorItem) {
            return true;
        }
        if (Kind instanceof LingeringPotionItem) {
            return false;
        }
        if (Kind instanceof PotionItem || Kind instanceof MilkBucketItem || Stack.has(DataComponents.FOOD)) {
            return true;
        }
        UseAnim Motion = Stack.getUseAnimation();
        return !(Kind instanceof BlockItem) && (Motion == UseAnim.EAT || Motion == UseAnim.DRINK);
    }

    private static boolean insideEntrance(Level World, BlockPos Pos) {
        return Scp087Entrance.isInterior(World, Pos);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        Player User = Event.getPlayer();
        BlockState State = Event.getState();
        if (Scp087SealedBlock.isSealed(State) || State.getBlock() instanceof Scp087RailBlock || State.getBlock() instanceof Scp087DoorBlock) {
            if (User == null || !User.isCreative()) {
                Event.setCanceled(true);
                return;
            }
        }
        if (User != null && bound(User)) {
            Event.setCanceled(true);
            refuse(User, "break");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakSpeed(PlayerEvent.BreakSpeed Event) {
        if (bound(Event.getEntity())) {
            Event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void placing(BlockEvent.EntityPlaceEvent Event) {
        if (!(Event.getLevel() instanceof Level World)) {
            return;
        }
        Entity Placer = Event.getEntity();
        boolean Exempt = Placer instanceof Player User && exempt(User);
        if (Scp087Stairwell.inStairwell(World)) {
            if (Exempt) {
                Scp087World.get(World.getServer()).markDirty(Scp087Stairwell.slotAt(Event.getPos().getX()));
                return;
            }
            Event.setCanceled(true);
            if (Placer instanceof Player User) {
                refuse(User, "build");
            }
            return;
        }
        if (!Exempt && insideEntrance(World, Event.getPos())) {
            Event.setCanceled(true);
            if (Placer instanceof Player User) {
                refuse(User, "build");
            }
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
        if (Event.getLevel() instanceof Level World && (Scp087Stairwell.inStairwell(World) || insideEntrance(World, Event.getPos()))) {
            Event.setNewState(Event.getOriginalState());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void portal(BlockEvent.PortalSpawnEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp087Stairwell.inStairwell(World)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Start Event) {
        if (Scp087Stairwell.inStairwell(Event.getLevel())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void detonate(ExplosionEvent.Detonate Event) {
        Level World = Event.getLevel();
        Event.getAffectedBlocks().removeIf(Pos -> {
            BlockState State = World.getBlockState(Pos);
            return Scp087SealedBlock.isSealed(State) || State.getBlock() instanceof Scp087DoorBlock || State.getBlock() instanceof Scp087RailBlock;
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useOnBlock(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        if (!bound(User)) {
            return;
        }
        Event.setUseItem(TriState.FALSE);
        if (!(User.level().getBlockState(Event.getPos()).getBlock() instanceof Scp087DoorBlock)) {
            Event.setUseBlock(TriState.FALSE);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useItem(PlayerInteractEvent.RightClickItem Event) {
        Player User = Event.getEntity();
        if (!bound(User) || allowed(Event.getItemStack())) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.FAIL);
        refuse(User, Event.getItemStack().getItem() instanceof BlockItem ? "build" : "item");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useEntity(PlayerInteractEvent.EntityInteract Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useEntitySpecific(PlayerInteractEvent.EntityInteractSpecific Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pearl(EntityTeleportEvent.EnderPearl Event) {
        if (Scp087Stairwell.inStairwell(Event.getEntity())) {
            Event.setCanceled(true);
            if (Event.getEntity() instanceof Player User) {
                refuse(User, "escape");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void chorus(EntityTeleportEvent.ChorusFruit Event) {
        if (Scp087Stairwell.inStairwell(Event.getEntity())) {
            Event.setCanceled(true);
            if (Event.getEntity() instanceof Player User) {
                refuse(User, "escape");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void enderTeleport(EntityTeleportEvent.EnderEntity Event) {
        if (Scp087Stairwell.inStairwell(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void sleeping(CanPlayerSleepEvent Event) {
        if (Scp087Stairwell.inStairwell(Event.getEntity())) {
            Event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tossing(ItemTossEvent Event) {
        Player User = Event.getPlayer();
        if (!bound(User) || User.level().isClientSide) {
            return;
        }
        ItemStack Stack = Event.getEntity().getItem().copy();
        Event.setCanceled(true);
        if (!User.getInventory().add(Stack) && !Stack.isEmpty()) {
            User.spawnAtLocation(Stack);
        }
        User.inventoryMenu.broadcastChanges();
        refuse(User, "toss");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void spawning(FinalizeSpawnEvent Event) {
        if (!(Event.getLevel().getLevel() instanceof Level World) || !Scp087Stairwell.inStairwell(World)) {
            return;
        }
        MobSpawnType Kind = Event.getSpawnType();
        if (Kind != MobSpawnType.COMMAND && Kind != MobSpawnType.MOB_SUMMONED && Kind != MobSpawnType.EVENT) {
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

    @SubscribeEvent
    public static void forget(ServerStoppedEvent Event) {
        Warned.clear();
    }
}
